/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package javassist.compiler.ast;

import javassist.compiler.CompileError;

/**
 * Assignment expression.
 */
public class AssignExpr extends Expr {
    /* operator must be either of:
     * =, %=, &=, *=, +=, -=, /=, ^=, |=, <<=, >>=, >>>=
     */

    public AssignExpr(int op, ASTree _head, ASTList _tail) {
        super(op, _head, _tail);
    }

    public static AssignExpr makeAssign(int op, ASTree oprand1,
                                        ASTree oprand2) {
        return new AssignExpr(op, oprand1, new ASTList(oprand2));
    }

    public void accept(Visitor v) throws CompileError {
        v.atAssignExpr(this);
    }
}
