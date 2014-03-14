/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class RBuiltinPackages implements RBuiltinLookup {

    private static final List<RBuiltinPackage> packages = new ArrayList<>();

    protected void load(RBuiltinPackage builtins) {
        packages.add(builtins);
    }

    public List<RBuiltinPackage> getPackages() {
        return packages;
    }

    @Override
    public RFunction lookup(String methodName) {
        RFunction function = RContext.getInstance().getCachedFunction(methodName);
        if (function != null) {
            return function;
        }

        RBuiltinFactory builtin = lookupBuiltin(methodName);
        if (builtin == null) {
            return null;
        }
        return createFunction(builtin, methodName);
    }

    private static RFunction createFunction(RBuiltinFactory builtin, String methodName) {
        CallTarget callTarget = RBuiltinNode.createArgumentsCallTarget(builtin);
        return RContext.getInstance().putCachedFunction(methodName, new RFunction(builtin.getBuiltinNames()[0], callTarget, true));
    }

    public static RBuiltinFactory lookupBuiltin(String name) {
        return RBuiltinPackage.lookupByName(name);
    }

}