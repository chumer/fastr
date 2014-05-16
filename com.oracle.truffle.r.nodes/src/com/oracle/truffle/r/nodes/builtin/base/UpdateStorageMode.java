/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;

@RBuiltin(value = "storage.mode<-")
public abstract class UpdateStorageMode extends RBuiltinNode {

    @Child private Typeof typeof;
    @Child private CastTypeNode castTypeNode;
    @Child private IsFactor isFactor;

    @Specialization(order = 0)
    public Object update(VirtualFrame frame, final Object x, final String value) {
        controlVisibility();
        if (value.equals(RRuntime.REAL)) {
            throw RError.getDefunct(getEncapsulatingSourceSection(), RRuntime.REAL, RRuntime.TYPE_DOUBLE);
        }
        if (value.equals(RRuntime.SINGLE)) {
            throw RError.getDefunct(getEncapsulatingSourceSection(), RRuntime.SINGLE, "mode<-");
        }
        if (typeof == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeof = insert(TypeofFactory.create(new RNode[1], this.getBuiltin()));
        }
        String typeX = typeof.execute(frame, x).getDataAt(0);
        if (typeX.equals(value)) {
            return x;
        }
        if (isFactor == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isFactor = insert(IsFactorFactory.create(new RNode[1], this.getBuiltin()));
        }
        if (isFactor.execute(frame, x) == RRuntime.LOGICAL_TRUE) {
            throw RError.getInvalidStorageModeUpdate(getEncapsulatingSourceSection());
        }
        if (castTypeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castTypeNode = insert(CastTypeNodeFactory.create(new RNode[2], this.getBuiltin()));
        }
        Object result = castTypeNode.execute(frame, x, value);
        // TODO: copy attributes from x to result.
        return result != null ? result : x;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 1)
    public Object update(VirtualFrame frame, final Object x, final Object value) {
        controlVisibility();
        throw RError.getValueNull(getEncapsulatingSourceSection());
    }
}