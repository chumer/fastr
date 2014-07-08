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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "unique", kind = INTERNAL)
// TODO Implement general case
public abstract class Unique extends RBuiltinNode {

    @SuppressWarnings("unused")
    @Specialization
    public RStringVector doUnique(RAbstractStringVector vec, byte incomparables, byte fromLast, byte nmax) {
        ArrayList<String> dataList = new ArrayList<>(vec.getLength());
        for (int i = 0; i < vec.getLength(); i++) {
            String s = vec.getDataAt(i);
            if (!dataList.contains(s)) {
                dataList.add(s);
            }
        }
        String[] data = new String[dataList.size()];
        dataList.toArray(data);
        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
    }
}