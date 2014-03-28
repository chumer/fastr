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
package com.oracle.truffle.r.nodes.builtin.base;

import java.io.*;
import java.nio.file.*;
import java.nio.file.FileSystem;

import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class FileLinkAdaptor extends RBuiltinNode {
    protected Object doFileLink(RAbstractStringVector vecFrom, RAbstractStringVector vecTo, boolean symbolic) {
        int lenFrom = vecFrom.getLength();
        int lenTo = vecTo.getLength();
        if (lenFrom < 1) {
            throw RError.getGenericError(getEncapsulatingSourceSection(), "nothing to link");
        }
        if (lenTo < 1) {
            return RDataFactory.createLogicalVector(0);
        }
        int len = lenFrom > lenTo ? lenFrom : lenTo;
        FileSystem fileSystem = FileSystems.getDefault();
        byte[] status = new byte[len];
        for (int i = 0; i < len; i++) {
            String from = vecFrom.getDataAt(i % lenFrom);
            String to = vecTo.getDataAt(i % lenTo);
            if (from == RRuntime.STRING_NA || to == RRuntime.STRING_NA) {
                status[i] = RRuntime.LOGICAL_FALSE;
            } else {
                Path fromPath = fileSystem.getPath(Utils.tildeExpand(from));
                Path toPath = fileSystem.getPath(Utils.tildeExpand(to));
                status[i] = RRuntime.LOGICAL_TRUE;
                try {
                    if (symbolic) {
                        Files.createSymbolicLink(toPath, fromPath);
                    } else {
                        Files.createLink(toPath, fromPath);
                    }
                } catch (UnsupportedOperationException | IOException ex) {
                    status[i] = RRuntime.LOGICAL_FALSE;
                    RContext.getInstance().setEvalWarning("  cannot link '" + from + "' to '" + to + "', reason " + ex.getMessage());
                }
            }
        }
        return RDataFactory.createLogicalVector(status, RDataFactory.COMPLETE_VECTOR);
    }

}