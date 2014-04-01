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
package com.oracle.truffle.r.runtime;

import java.util.*;

/**
 * Support for (global) variables deined by packages (e.g. {@code base)}. Similar to
 * {@link ROptions}, when a package is loaded, if it defines (global) variables, it must register
 * with this class. On startup, the {@code Handler} is called with the {@link REnvironment} instance
 * for the package, which can be used to define the variables.
 */
public class RPackageVariables {

    public interface Handler {
        void initialize(REnvironment env);
    }

    private static Map<String, Handler> map = new HashMap<>();

    public static void registerHandler(String name, Handler handler) {
        map.put(name, handler);
    }

    public static void initialize() {
        for (Map.Entry<String, Handler> entry : map.entrySet()) {
            entry.getValue().initialize(REnvironment.lookup(entry.getKey()));
        }
    }
}