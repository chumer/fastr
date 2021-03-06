/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.tools;

import com.oracle.truffle.r.parser.ast.*;

public class BasicVisitor<R> implements Visitor<R> {

    public R visit(ASTNode n) {
        n.visitAll(this);
        return null;
    }

    @Override
    public R visit(Sequence n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(If n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(Repeat n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(While n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(For n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(Next n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(Break n) {
        return visit((ASTNode) n);
    }

    public R visit(Operation op) {
        return visit((ASTNode) op);
    }

    public R visit(BinaryOperation op) {
        return visit((Operation) op);
    }

    @Override
    public R visit(UnaryOperation op) {
        return visit((ASTNode) op);
    }

    @Override
    public R visit(Constant c) {
        return visit((ASTNode) c);
    }

    @Override
    public R visit(AccessVariable n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(AccessVariadicComponent n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(AssignVariable n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(Replacement n) {
        return visit((ASTNode) n);
    }

    public R visit(Call n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(Function n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(ArgNode n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(Formula n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(Missing m) {
        return visit((ASTNode) m);
    }

}
