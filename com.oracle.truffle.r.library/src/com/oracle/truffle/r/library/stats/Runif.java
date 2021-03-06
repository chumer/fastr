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
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.rng.RandomNumberNode;

/**
 * TODO GnuR checks/updates {@code .Random.seed} across this call.
 */
public abstract class Runif extends RExternalBuiltinNode.Arg3 {

    @Child private RandomNumberNode random = new RandomNumberNode();

    @Specialization
    protected Object doRunif(Object n, Object min, Object max) {
        // TODO full error checks
        int nInt = castInt(castVector(n));
        double minDouble = castDouble(castVector(min)).getDataAt(0);
        double maxDouble = castDouble(castVector(max)).getDataAt(0);
        double delta = maxDouble - minDouble;

        double[] result = random.executeDouble(nInt);
        for (int i = 0; i < nInt; i++) {
            result[i] = minDouble + result[i] * delta;
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.COMPLETE_VECTOR);
    }
}
