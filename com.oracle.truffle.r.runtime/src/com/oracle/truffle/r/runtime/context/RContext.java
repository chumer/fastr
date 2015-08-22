/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.context;

import java.util.*;
import java.util.concurrent.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.vm.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RCmdOptions.Client;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.ffi.*;
import com.oracle.truffle.r.runtime.rng.*;

/**
 * Encapsulates the runtime state ("context") of an R session. All access to that state from the
 * implementation <b>must</b> go through this class. There can be multiple instances
 * (multiple-tenancy) active within a single process/Java-VM.
 *
 * The context provides two sub-interfaces {@link ConsoleHandler} and {@link Engine}that are
 * (typically) implemented elsewhere, and accessed through {@link #getConsoleHandler()} and
 * {@link #getEngine()}, respectively.
 *
 * Context-specific state for implementation classes is managed by this class (or the associated
 * engine) and accessed through the {@code getXXXState} methods.
 *
 * The life-cycle of a {@link RContext} is:
 * <ol>
 * <li>created: {@link #create(Env)}</li>
 * <li>destroyed: {@link #destroy()}</li>
 * </ol>
 *
 * Evaluations are only possible on an active context.
 *
 */
public final class RContext extends ExecutionContext {

    public static final int CONSOLE_WIDTH = 80;

    public enum ContextKind {
        /**
         * Essentially a clean restart, modulo the basic VM-wide initialization. which does include,
         * for example, reading the external environment variables. I.e., it is not a goal to create
         * a multi-user environment with different external inputs. This kind of context can be used
         * in a parallel computation. The initial context is always of this kind. The intent is that
         * all mutable state is localized to the context, which may not be completely achievable.
         * For example, shared native libraries are assumed to contain no mutable state and be
         * re-entrant.
         */
        SHARE_NOTHING,

        /**
         * Shares the set of loaded packages of the parent context at the time the context is
         * created. Only useful when there is a priori knowledge on the evaluation that the context
         * will be used for. Cannot safely be used for parallel context evaluation. Must be created
         * as a child of an existing parent context of type {@link #SHARE_NOTHING} or
         * {@link #SHARE_PARENT_RO} and only one such child is allowed. (Strictly speaking the
         * invariant should be only one active child, but the implementation enforces it at creation
         * time). Evidently any changes made to the shared environment, e.g., loading a package,
         * affect the parent.
         */
        SHARE_PARENT_RW,

        /**
         * Intermediate between {@link #SHARE_NOTHING} and {@link #SHARE_PARENT_RW}, this is similar
         * to the standard shared code/copied data model provided by operating systems, although the
         * code/data distinction isn't completely applicable to a language like R. Unlike
         * {@link #SHARE_NOTHING}, where the ASTs for the functions in the default packages are
         * distinct copies in each context, in this kind of context, they are shared. Strictly
         * speaking, the bindings of R functions are shared, and this is achieved by creating a
         * shallow copy of the environments associated with the default packages of the parent
         * context at the time the context is created.
         */
        SHARE_PARENT_RO;

        public static final ContextKind[] VALUES = values();
    }

    /**
     * Tagging interface denoting a class that carries the context-specific state for a class that
     * has context-specific state. The class specific state must implement this interface.
     */
    public interface ContextState {
        /**
         * Called in response to the {@link RContext#destroy} method. Provides a hook for finalizing
         * any state before the context is destroyed.
         */
        @SuppressWarnings("unused")
        default void beforeDestroy(RContext context) {
            // default empty implementation
        }
    }

    /**
     * A thread that is explicitly associated with a context for efficient lookup.
     */
    public static class ContextThread extends Thread {
        protected RContext context;

        public ContextThread(RContext context) {
            this.context = context;
        }

        protected ContextThread() {

        }

        public void setContext(RContext context) {
            this.context = context;
        }
    }

    /**
     * A thread for performing an evaluation (used by {@code fastr} package.
     */
    public static class EvalThread extends ContextThread {
        private final Source source;
        private final ContextInfo info;

        public static final Map<Integer, Thread> threads = new ConcurrentHashMap<>();

        public EvalThread(ContextInfo info, Source source) {
            super(null);
            this.info = info;
            this.source = source;
            threads.put(info.getId(), this);
        }

        @Override
        public void run() {
            TruffleVM vm = info.newContext();
            setContext(truffleVMContexts.get(vm));
            try {
                try {
                    context.engine.parseAndEval(source, true);
                } catch (ParseException e) {
                    throw e.throwAsRError();
                }
            } finally {
                context.destroy();
                threads.remove(info.getId());
            }
        }
    }

    private final ContextInfo info;
    private final Engine engine;

    /**
     * Denote whether the result of an expression should be printed in the shell or not.
     */
    private boolean resultVisible = true;

    /**
     * A context-specific value that is checked in {@code HiddenInternalFunctions} to avoid an error
     * report on a {@code SUBSTITUTE} builtin. Not worth promoting to a {@link ContextState}.
     */
    private boolean loadingBase;

    /**
     * At most one shared child.
     */
    private RContext sharedChild;

    /**
     * Back pointer to the evalThread.
     */
    private EvalThread evalThread;

    /**
     * Typically there is a 1-1 relationship between an {@link RContext} and the thread that is
     * performing the evaluation, so we can store the {@link RContext} in a {@link ThreadLocal}.
     *
     * When a context is first created no threads are attached, to allow contexts to be used as
     * values in the experimental {@code fastr.createcontext} function. Additional threads can be
     * added by the {@link #attachThread} method.
     */
    public static final ThreadLocal<RContext> threadLocalContext = new ThreadLocal<>();

    /**
     * Used by the MethodListDispatch class.
     */

    private boolean methodTableDispatchOn = true;

    private boolean active;

    /**
     * A (hopefully) temporary workaround to ignore the setting of {@link #resultVisible} for
     * benchmarks. Set across all contexts.
     */
    @CompilationFinal private static boolean ignoreVisibility;

    /*
     * Workarounds to finesse project circularities between runtime/nodes.
     */
    @CompilationFinal private static RRuntimeASTAccess runtimeASTAccess;
    @CompilationFinal private static RBuiltinLookup builtinLookup;

    /**
     * Initialize VM-wide static values.
     */
    public static void initialize(RRuntimeASTAccess rASTHelperArg, RBuiltinLookup rBuiltinLookupArg, boolean ignoreVisibilityArg) {
        runtimeASTAccess = rASTHelperArg;
        builtinLookup = rBuiltinLookupArg;
        ignoreVisibility = ignoreVisibilityArg;
    }

    /**
     * Associates this {@link RContext} with the current thread.
     */
    public void attachThread() {
        Thread current = Thread.currentThread();
        if (current instanceof ContextThread) {
            ((ContextThread) current).setContext(this);
        } else {
            threadLocalContext.set(this);
        }
    }

    /**
     * Waits for the associated EvalThread to finish.
     *
     * @throws InterruptedException
     */
    public void joinThread() throws InterruptedException {
        EvalThread t = this.evalThread;
        if (t == null) {
            throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "no eval thread in a given context");
        }
        this.evalThread = null;
        t.join();
    }

    private static final Assumption singleContextAssumption = Truffle.getRuntime().createAssumption("single RContext");
    @CompilationFinal private static RContext singleContext;

    private final Env env;
    private final HashMap<String, TruffleObject> exportedSymbols = new HashMap<>();

    /**
     * The set of classes for which the context manages context-specific state, and their state. We
     * could do this more dynamically with a registration process, perhaps driven by an annotation
     * processor, but the set is relatively small, so we just enumerate them here.
     */
    public final REnvVars stateREnvVars;
    public final RProfile stateRProfile;
    public final ROptions.ContextStateImpl stateROptions;
    public final REnvironment.ContextStateImpl stateREnvironment;
    public final RErrorHandling.ContextStateImpl stateRErrorHandling;
    public final ConnectionSupport.ContextStateImpl stateRConnection;
    public final StdConnections.ContextStateImpl stateStdConnections;
    public final RRNG.ContextStateImpl stateRNG;
    public final ContextState stateRFFI;
    public final RSerialize.ContextStateImpl stateRSerialize;

    private ContextState[] contextStates() {
        return new ContextState[]{stateREnvVars, stateRProfile, stateROptions, stateREnvironment, stateRErrorHandling, stateRConnection, stateStdConnections, stateRNG, stateRFFI, stateRSerialize};
    }

    private RContext(Env env) {
        if (tempInitializingContextInfo == null) {
            this.info = ContextInfo.create(RCmdOptions.parseArguments(Client.R, new String[0]), ContextKind.SHARE_NOTHING, null, new DefaultConsoleHandler(env));
        } else {
            this.info = tempInitializingContextInfo;
            lastContext = this;
        }

        this.env = env;
        if (info.getConsoleHandler() == null) {
            throw Utils.fail("no console handler set");
        }

        if (singleContextAssumption.isValid()) {
            if (singleContext == null) {
                singleContext = this;
            } else {
                singleContext = null;
                singleContextAssumption.invalidate();
            }
        }
        engine = RContext.getRRuntimeASTAccess().createEngine(this);

        /*
         * Activate the context by attaching the current thread and initializing the {@link
         * ContextState} objects. Note that we attach the thread before creating the new context
         * state. This means that code that accesses the state through this interface will receive a
         * {@code null} value. Access to the parent state is available through the {@link RContext}
         * argument passed to the newContext methods. It might be better to attach the thread after
         * state creation but it is a finely balanced decision and risks incorrectly accessing the
         * parent state.
         */
        assert !active;
        active = true;
        attachThread();
        stateREnvVars = REnvVars.newContext(this);
        stateRProfile = RProfile.newContext(this, stateREnvVars);
        stateROptions = ROptions.ContextStateImpl.newContext(this, stateREnvVars);
        stateREnvironment = REnvironment.ContextStateImpl.newContext(this);
        stateRErrorHandling = RErrorHandling.ContextStateImpl.newContext(this);
        stateRConnection = ConnectionSupport.ContextStateImpl.newContext(this);
        stateStdConnections = StdConnections.ContextStateImpl.newContext(this);
        stateRNG = RRNG.ContextStateImpl.newContext(this);
        stateRFFI = RFFIContextStateFactory.newContext(this);
        stateRSerialize = RSerialize.ContextStateImpl.newContext(this);
        engine.activate(stateREnvironment);

        if (info.getKind() == ContextKind.SHARE_PARENT_RW) {
            if (info.getParent().sharedChild != null) {
                throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "can't have multiple active SHARED_PARENT_RW contexts");
            }
            info.getParent().sharedChild = this;
        }
        for (ContextState state : contextStates()) {
            assert state != null;
        }
    }

    /**
     * Create a context with the given configuration.
     */
    public static RContext create(Env env) {
        return new RContext(env);
    }

    /**
     * Destroy this context.
     */
    public void destroy() {
        for (ContextState state : contextStates()) {
            state.beforeDestroy(this);
        }
        if (info.getKind() == ContextKind.SHARE_PARENT_RW) {
            info.getParent().sharedChild = null;
        }
        if (info.getParent() == null) {
            threadLocalContext.set(null);
        } else {
            threadLocalContext.set(info.getParent());
        }
    }

    public RContext getParent() {
        return info.getParent();
    }

    public Env getEnv() {
        return env;
    }

    public ContextKind getKind() {
        return info.getKind();
    }

    @TruffleBoundary
    private static RContext getInstanceInternal() {
        RContext result = threadLocalContext.get();
        assert result != null;
        assert result.active;
        return result;
    }

    public static RContext getInstance() {
        RContext context = singleContext;
        if (context != null) {
            try {
                singleContextAssumption.check();
                return context;
            } catch (InvalidAssumptionException e) {
                // fallback to slow case
            }
        }

        Thread current = Thread.currentThread();
        if (current instanceof ContextThread) {
            context = ((ContextThread) current).context;
            assert context != null;
            return context;
        } else {
            return getInstanceInternal();
        }
    }

    /**
     * Access to the engine, when an {@link RContext} object is available, and/or when {@code this}
     * context is not active.
     */
    public Engine getThisEngine() {
        return engine;
    }

    public boolean isVisible() {
        return resultVisible;
    }

    public void setVisible(boolean v) {
        resultVisible = v;
    }

    public boolean isMethodTableDispatchOn() {
        return methodTableDispatchOn;
    }

    public void setMethodTableDispatchOn(boolean on) {
        methodTableDispatchOn = on;
    }

    public boolean isInteractive() {
        return info.getConsoleHandler().isInteractive();
    }

    public static boolean isIgnoringVisibility() {
        return ignoreVisibility;
    }

    public ConsoleHandler getConsoleHandler() {
        return info.getConsoleHandler();
    }

    /**
     * This is a static property of the implementation and not context-specific.
     */
    public static RRuntimeASTAccess getRRuntimeASTAccess() {
        return runtimeASTAccess;
    }

    /**
     * Is {@code name} a builtin function (but not a {@link RBuiltinKind#INTERNAL}?
     */
    public static boolean isPrimitiveBuiltin(String name) {
        return builtinLookup.isPrimitiveBuiltin(name);
    }

    /**
     * Return the {@link RFunction} for the builtin {@code name}.
     */
    @TruffleBoundary
    public static RFunction lookupBuiltin(String name) {
        return builtinLookup.lookupBuiltin(name);
    }

    /**
     * Returns the descriptor for the builtin with the given name. This does not cause an RFunction
     * to be created.
     */
    public static RBuiltinDescriptor lookupBuiltinDescriptor(String name) {
        return builtinLookup.lookupBuiltinDescriptor(name);
    }

    public RCmdOptions getOptions() {
        return info.getOptions();
    }

    @Override
    public String toString() {
        return "context: " + info.getId();
    }

    /*
     * static functions necessary in code where the context is only implicit in the thread(s)
     * running an evaluation
     */

    public static Engine getEngine() {
        return RContext.getInstance().engine;
    }

    public void setLoadingBase(boolean b) {
        loadingBase = b;
    }

    public boolean getLoadingBase() {
        return loadingBase;
    }

    public Map<String, TruffleObject> getExportedSymbols() {
        return exportedSymbols;
    }

    public TimeZone getSystemTimeZone() {
        return info.getSystemTimeZone();
    }

    /*
     * TODO: this fields are used to convey initialization information from outside TruffleVM to
     * RContext. need to be replaced with a mechanism provided by TruffleVM once this is available.
     */
    public static ContextInfo tempInitializingContextInfo;
    private static RContext lastContext;
    private static final Map<TruffleVM, RContext> truffleVMContexts = new HashMap<>();

    // TODO: destroying a TruffleVM should be handled in TruffleVM itself
    public static void destroyContext(TruffleVM vm) {
        truffleVMContexts.get(vm).destroy();
    }

    public static void associate(TruffleVM vm) {
        assert lastContext != null;
        truffleVMContexts.put(vm, lastContext);
        lastContext = null;
        tempInitializingContextInfo = null;
    }

    public static RContext fromTruffleVM(TruffleVM vm) {
        return truffleVMContexts.get(vm);
    }
}