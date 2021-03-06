/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.nodes.*;

@RBuiltin(name = "formals", kind = RBuiltinKind.INTERNAL, parameterNames = {"fun"})
public abstract class Formals extends RBuiltinNode {
    @Specialization
    @TruffleBoundary
    protected Object formals(RFunction fun) {
        controlVisibility();
        if (fun.isBuiltin()) {
            return RNull.instance;
        }
        FunctionDefinitionNode fdNode = (FunctionDefinitionNode) fun.getTarget().getRootNode();
        if (fdNode.getParameterCount() == 0) {
            return RNull.instance;
        }
        FormalArguments formalArgs = fdNode.getFormalArguments();
        Object succ = RNull.instance;
        for (int i = formalArgs.getSignature().getLength() - 1; i >= 0; i--) {
            RNode def = formalArgs.getDefaultArgument(i);
            Object defValue = def == null ? RSymbol.MISSING : RDataFactory.createLanguage(def);
            RPairList pl = RDataFactory.createPairList(defValue, succ, RDataFactory.createSymbol(formalArgs.getSignature().getName(i)));
            succ = pl;
        }
        return succ;
    }
}
