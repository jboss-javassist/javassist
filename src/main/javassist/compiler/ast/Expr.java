/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
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

import javassist.compiler.TokenId;
import javassist.compiler.CompileError;

/**
 * Expression.
 */
public class Expr extends ASTList implements TokenId {
    /* operator must be either of:
     * (unary) +, (unary) -, ++, --, !, ~,
     * CALL, ARRAY, . (dot), MEMBER (static member access).
     * Otherwise, the object should be an instance of a subclass.
     */

    protected int operatorId;

    public Expr(int op, ASTree _head, ASTList _tail) {
        super(_head, _tail);
        operatorId = op;
    }

    public Expr(int op, ASTree _head) {
        super(_head);
        operatorId = op;
    }

    public static Expr make(int op, ASTree oprand1, ASTree oprand2) {
        return new Expr(op, oprand1, new ASTList(oprand2));
    }

    public int getOperator() { return operatorId; }

    public ASTree oprand1() { return getLeft(); }

    public ASTree oprand2() { return getRight().getLeft(); }

    public void accept(Visitor v) throws CompileError { v.atExpr(this); }

    public String getName() {
        int id = operatorId;
        if (id < 128)
            return String.valueOf((char)id);
        else if (NEQ <= id && id <= ARSHIFT_E)
            return opNames[id - NEQ];
        else if (id == INSTANCEOF)
            return "instanceof";
        else
            return String.valueOf(id);
    }

    protected String getTag() {
        return "op:" + getName();
    }
}
