/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * A trait that enables the caching of {@link Closure}s for certain expressions ({@link RNode}s).
 */
public interface ClosureCache {
    /**
     * @param expr
     * @return A {@link Closure} representing the given {@link RNode}. If expr is <code>null</code>
     *         <code>null</code> is returned.
     */
    @TruffleBoundary
    default Closure getOrCreateClosure(RNode expr) {
        if (expr == null) {
            return null;
        }

        IdentityHashMap<RNode, Closure> cache = getContent();
        Closure result = cache.get(expr);
        if (result == null) {
            result = Closure.create(expr);
            cache.put(expr, result);
        }
        return result;
    }

    /**
     * Access to the raw content.
     *
     * @return The {@link Map} containing the cached values
     */
    IdentityHashMap<RNode, Closure> getContent();
}
