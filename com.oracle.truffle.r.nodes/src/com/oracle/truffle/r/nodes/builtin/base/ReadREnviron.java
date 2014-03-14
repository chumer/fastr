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

import java.io.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("readRenviron")
public abstract class ReadREnviron extends RBuiltinNode {

    @Specialization(guards = "lengthOneCVector")
    public Object doReadEnviron(RAbstractStringVector vec) {
        String path = Utils.tildeExpand(vec.getDataAt(0));
        byte result = RRuntime.LOGICAL_TRUE;
        try {
            REnvVars.readEnvironFile(path);
        } catch (FileNotFoundException ex) {
            RContext.getInstance().setEvalWarning(ex.getMessage());
            result = RRuntime.LOGICAL_FALSE;
        } catch (IOException ex) {
            throw RError.getGenericError(getSourceSection(), ex.getMessage());
        }
        return new RInvisible(result);
    }

    public static boolean lengthOneCVector(RAbstractStringVector vec) {
        return vec.getLength() == 1;
    }

    @Generic
    public Object doReadEnvironGeneric(@SuppressWarnings("unused") Object x) {
        throw RError.getGenericError(getSourceSection(), "argument 'x' must be a character string");
    }
}