/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.control;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.RemoveAndAnswerNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Holds the sequence of nodes created for R's replacement assignment. Allows custom deparse and
 * debug handling.
 */
public final class ReplacementNode extends RNode implements RSyntaxNode {

    /**
     * This holds the AST for the "untransformed" AST, i.e. as it appears in the source. Currently
     * only used in {@code deparse} and {@code serialize}.
     */
    @CompilationFinal private RSyntaxNode syntaxLhs;
    private final boolean isSuper;

    @Child private WriteVariableNode storeRhs;
    @Child private WriteVariableNode storeValue;
    @Children private final RNode[] updates;
    @Child private RemoveAndAnswerNode removeTemp;
    @Child private RemoveAndAnswerNode removeRhs;

    public ReplacementNode(SourceSection src, boolean isSuper, RSyntaxNode syntaxLhs, RSyntaxNode rhs, String rhsSymbol, RNode v, String tmpSymbol, List<RNode> updates) {
        this.isSuper = isSuper;
        this.syntaxLhs = syntaxLhs;
        this.storeRhs = WriteVariableNode.createAnonymous(rhsSymbol, rhs.asRNode(), WriteVariableNode.Mode.INVISIBLE);
        this.storeValue = WriteVariableNode.createAnonymous(tmpSymbol, v, WriteVariableNode.Mode.INVISIBLE);
        this.updates = updates.toArray(new RNode[updates.size()]);
        // remove var and rhs, returning rhs' value
        this.removeTemp = RemoveAndAnswerNode.create(tmpSymbol);
        this.removeRhs = RemoveAndAnswerNode.create(rhsSymbol);
        assignSourceSection(src);
    }

    private String getSymbol() {
        return isSuper ? "<<-" : "<-";
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        storeRhs.execute(frame);
        storeValue.execute(frame);
        for (RNode update : updates) {
            update.execute(frame);
        }
        removeTemp.execute(frame);
        return removeRhs.execute(frame);
    }

    @Override
    public void deparseImpl(RDeparse.State state) {
        state.startNodeDeparse(this);
        syntaxLhs.deparseImpl(state);
        state.append(' ');
        state.append(getSymbol());
        state.append(' ');
        storeRhs.getRhs().asRSyntaxNode().deparseImpl(state);
        state.endNodeDeparse(this);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        state.setAsLangType();
        state.setCarAsSymbol(getSymbol());
        state.openPairList();
        state.serializeNodeSetCar(syntaxLhs);
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(storeRhs.getRhs());
        state.setCdr(state.closePairList());
        state.setCdr(state.closePairList());
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        // TODO: implement this correctly
        return this;
    }

    public int getRlengthImpl() {
        return 3;
    }

    @Override
    public Object getRelementImpl(int index) {
        switch (index) {
            case 0:
                return RDataFactory.createSymbolInterned(getSymbol());
            case 1:
                return RASTUtils.createLanguageElement(syntaxLhs.asRNode());
            case 2:
                return RASTUtils.createLanguageElement(storeRhs.getRhs());
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    @Override
    public boolean getRequalsImpl(RSyntaxNode other) {
        throw RInternalError.unimplemented();
    }
}
