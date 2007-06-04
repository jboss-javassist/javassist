/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.compiler.ast;

import javassist.compiler.CompileError;

/**
 * Binary expression.
 *
 * <p>If the operator is +, the right node might be null.
 * See TypeChecker.atBinExpr().
 */
public class BinExpr extends Expr {
    /* operator must be either of:
     * ||, &&, |, ^, &, ==, !=, <=, >=, <, >,
     * <<, >>, >>>, +, -, *, /, %
     */

    private BinExpr(int op, ASTree _head, ASTList _tail) {
        super(op, _head, _tail);
    }

    public static BinExpr makeBin(int op, ASTree oprand1, ASTree oprand2) {
        return new BinExpr(op, oprand1, new ASTList(oprand2));
    }

    public void accept(Visitor v) throws CompileError { v.atBinExpr(this); }
}
