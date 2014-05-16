/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;

@NodeField(name = "nonContainerPreserved", type = boolean.class)
public abstract class CastToContainerNode extends CastNode {

    public abstract RAbstractContainer executeRAbstractContainer(VirtualFrame frame, Object value);

    public abstract Object executeObject(VirtualFrame frame, Object value);

    public abstract boolean isNonContainerPreserved();

    protected boolean preserveNonContainer() {
        return isNonContainerPreserved();
    }

    @Specialization(order = 1, guards = "preserveNonContainer")
    @SuppressWarnings("unused")
    public RNull castNull(RNull rnull) {
        return RNull.instance;
    }

    @Specialization(order = 2, guards = "!preserveNonContainer")
    @SuppressWarnings("unused")
    public RAbstractVector cast(RNull rnull) {
        return RDataFactory.createList();
    }

    @Specialization(order = 3, guards = "preserveNonContainer")
    public RFunction castFunction(RFunction f) {
        return f;
    }

    @Specialization(order = 4, guards = "!preserveNonContainer")
    @SuppressWarnings("unused")
    public RAbstractVector cast(RFunction f) {
        return RDataFactory.createList();
    }

    @Specialization
    public RAbstractVector cast(RAbstractVector vector) {
        return vector;
    }

    @Specialization
    public RDataFrame cast(RDataFrame dataFrame) {
        return dataFrame;
    }

}