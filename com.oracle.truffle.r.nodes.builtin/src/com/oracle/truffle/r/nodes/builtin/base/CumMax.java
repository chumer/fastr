/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "cummax", kind = PRIMITIVE, parameterNames = {"x"})
public abstract class CumMax extends RBuiltinNode {

    private final NACheck na = NACheck.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Child private CastDoubleNode castDouble;

    @Specialization
    protected double cummax(double arg) {
        controlVisibility();
        return arg;
    }

    @Specialization
    protected int cummax(int arg) {
        controlVisibility();
        return arg;
    }

    @Specialization
    protected Object cummax(String arg) {
        controlVisibility();
        return na.convertStringToDouble(arg);
    }

    @Specialization
    protected int cummax(byte arg) {
        controlVisibility();
        na.enable(arg);
        if (na.check(arg)) {
            return RRuntime.INT_NA;
        }
        return arg;
    }

    @Specialization
    protected RAbstractIntVector cummaxIntSequence(RIntSequence v, //
                    @Cached("createBinaryProfile()") ConditionProfile negativeStrideProfile) {
        controlVisibility();
        if (negativeStrideProfile.profile(v.getStride() < 0)) {
            // all numbers are smaller than the first one
            return RDataFactory.createIntSequence(v.getStart(), 0, v.getLength());
        } else {
            return v;
        }
    }

    @Specialization
    protected RDoubleVector cummax(RAbstractDoubleVector v) {
        controlVisibility();
        double[] cmaxV = new double[v.getLength()];
        double max = v.getDataAt(0);
        cmaxV[0] = max;
        na.enable(v);
        int i;
        for (i = 1; i < v.getLength(); i++) {
            if (v.getDataAt(i) > max) {
                max = v.getDataAt(i);
            }
            if (na.check(v.getDataAt(i))) {
                break;
            }
            cmaxV[i] = max;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(cmaxV, i, cmaxV.length, RRuntime.DOUBLE_NA);
        }
        return RDataFactory.createDoubleVector(cmaxV, na.neverSeenNA(), v.getNames(attrProfiles));
    }

    @Specialization(contains = "cummaxIntSequence")
    protected RIntVector cummax(RAbstractIntVector v) {
        controlVisibility();
        int[] cmaxV = new int[v.getLength()];
        int max = v.getDataAt(0);
        cmaxV[0] = max;
        na.enable(v);
        int i;
        for (i = 1; i < v.getLength(); i++) {
            if (v.getDataAt(i) > max) {
                max = v.getDataAt(i);
            }
            if (na.check(v.getDataAt(i))) {
                break;
            }
            cmaxV[i] = max;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(cmaxV, i, cmaxV.length, RRuntime.INT_NA);
        }
        return RDataFactory.createIntVector(cmaxV, na.neverSeenNA(), v.getNames(attrProfiles));
    }

    @Specialization
    protected RIntVector cummax(RAbstractLogicalVector v) {
        controlVisibility();
        int[] cmaxV = new int[v.getLength()];
        int max = v.getDataAt(0);
        cmaxV[0] = max;
        na.enable(v);
        int i;
        for (i = 1; i < v.getLength(); i++) {
            if (v.getDataAt(i) > max) {
                max = v.getDataAt(i);
            }
            if (na.check(v.getDataAt(i))) {
                break;
            }
            cmaxV[i] = max;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(cmaxV, i, cmaxV.length, RRuntime.INT_NA);
        }
        return RDataFactory.createIntVector(cmaxV, na.neverSeenNA(), v.getNames(attrProfiles));
    }

    @Specialization
    protected RDoubleVector cummax(RAbstractStringVector v) {
        controlVisibility();
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeGen.create(false, false, false));
        }
        return cummax((RDoubleVector) castDouble.executeDouble(v));
    }

    @Specialization
    @TruffleBoundary
    protected RComplexVector cummax(@SuppressWarnings("unused") RAbstractComplexVector v) {
        controlVisibility();
        throw RError.error(this, RError.Message.CUMMAX_UNDEFINED_FOR_COMPLEX);
    }
}
