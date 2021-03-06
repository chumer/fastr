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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "dim<-", kind = PRIMITIVE, parameterNames = {"x", "value"})
public abstract class UpdateDim extends RInvisibleBuiltinNode {

    @Child private CastIntegerNode castInteger;

    private RAbstractIntVector castInteger(RAbstractVector vector) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeGen.create(true, false, false));
        }
        return (RAbstractIntVector) castInteger.execute(vector);
    }

    @Specialization
    protected RAbstractVector updateDim(RAbstractVector vector, @SuppressWarnings("unused") RNull dimensions) {
        controlVisibility();
        RVector result = (RVector) vector.materializeNonShared();
        result.resetDimensions(null);
        return result;
    }

    @Specialization
    protected RAbstractVector updateDim(RAbstractVector vector, RAbstractVector dimensions) {
        controlVisibility();
        if (dimensions.getLength() == 0) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(this, RError.Message.LENGTH_ZERO_DIM_INVALID);
        }
        int[] dimsData = castInteger(dimensions).materialize().getDataCopy();
        RVector.verifyDimensions(vector.getLength(), dimsData, this);
        RVector result = (RVector) vector.materializeNonShared();
        result.resetDimensions(dimsData);
        return result;
    }
}
