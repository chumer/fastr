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
package com.oracle.truffle.r.runtime.data.closures;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/*
 * This closure is meant to be used only for implementation of the binary operators.
 */
public class RFactorToStringVectorClosure extends RToStringVectorClosure implements RAbstractStringVector {

    private final RIntVector vector;
    private final RAbstractStringVector levels;

    public RFactorToStringVectorClosure(RFactor factor, NACheck naCheck) {
        super(factor.getVector(), naCheck);
        this.vector = factor.getVector();
        this.levels = (RAbstractStringVector) vector.getAttr(RRuntime.LEVELS_ATTR_KEY);
        if (this.levels == null) {
            RError.warning(RError.Message.IS_NA_TO_NON_VECTOR, "NULL");
        }
    }

    public String getDataAt(int index) {
        if (levels == null) {
            return RRuntime.STRING_NA;
        } else {
            return this.levels.getDataAt(vector.getDataAt(index) - 1);
        }
    }
}