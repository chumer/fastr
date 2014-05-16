/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.envframe;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.REnvironment.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Access to the frame component, handled by delegation in {@link REnvironment}. The default
 * implementation throws an exception for all calls. It is used in the {@link FunctionDefinition}
 * environment which never has an associated frame.
 */
public class REnvFrameAccess {
    /**
     * Return the unique id that identifies the associated environment in the frame, or {@code null}
     * if none.
     */
    public Object id() {
        throw notImplemented("id");
    }

    /**
     * Return the value of object named {@code name} or {@code null} if not found.
     */
    public Object get(@SuppressWarnings("unused") String key) {
        throw notImplemented("get");
    }

    /**
     * Set the value of object named {@code name} to {@code value}. if {@code value == null},
     * effectively removes the name.
     *
     * @throws PutException if the binding is locked
     */
    @SuppressWarnings("unused")
    public void put(String key, Object value) throws REnvironment.PutException {
        throw notImplemented("put");
    }

    /**
     * Remove binding.
     */
    public void rm(@SuppressWarnings("unused") String key) {
        throw notImplemented("rm");
    }

    @SuppressWarnings("unused")
    public RStringVector ls(boolean allNames, String pattern) {
        throw notImplemented("ls");
    }

    public void lockBindings() {
        throw notImplemented("lockBindings");
    }

    /**
     * Disallow updates to {@code key}.
     */
    public void lockBinding(@SuppressWarnings("unused") String key) {
        throw notImplemented("lockBinding");
    }

    /**
     * Allow updates to (previously locked) {@code key}.
     */
    public void unlockBinding(@SuppressWarnings("unused") String key) {
        throw notImplemented("unlockBinding");
    }

    public boolean bindingIsLocked(@SuppressWarnings("unused") String key) {
        throw notImplemented("bindingIsLocked");
    }

    public MaterializedFrame getFrame() {
        throw notImplemented("getFrame");
    }

    private static RuntimeException notImplemented(String methodName) {
        return new RuntimeException("FrameAccess method '" + methodName + "' not implemented");
    }

}