/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.compiler.ast;

import javassist.compiler.CompileError;
import javassist.compiler.TokenId;

/**
 * Statement.
 */
public class Stmnt extends ASTList implements TokenId {
    /** default serialVersionUID */
    private static final long serialVersionUID = 1L;
    protected int operatorId;

    public Stmnt(int op, ASTree _head, ASTList _tail, int lineNumber) {
        super(_head, _tail, lineNumber);
        operatorId = op;
    }

    public Stmnt(int op, ASTree _head, int lineNumber) {
        super(_head, lineNumber);
        operatorId = op;
    }

    public Stmnt(int op, int lineNumber) {
        this(op, null, lineNumber);
    }

    public static Stmnt make(int op, ASTree oprand1, ASTree oprand2, int lineNumber) {
        return new Stmnt(op, oprand1, new ASTList(oprand2, lineNumber), lineNumber);
    }

    public static Stmnt make(int op, ASTree op1, ASTree op2, ASTree op3, int lineNumber) {
        return new Stmnt(op, op1, new ASTList(op2, new ASTList(op3, lineNumber), lineNumber), lineNumber);
    }

    @Override
    public void accept(Visitor v) throws CompileError { v.atStmnt(this); }

    public int getOperator() { return operatorId; }

    @Override
    protected String getTag() {
        if (operatorId < 128)
            return "stmnt:" + (char)operatorId;
        return "stmnt:" + operatorId;
    }
}
