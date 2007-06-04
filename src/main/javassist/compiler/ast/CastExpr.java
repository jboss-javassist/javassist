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

import javassist.compiler.TokenId;
import javassist.compiler.CompileError;

/**
 * Cast expression.
 */
public class CastExpr extends ASTList implements TokenId {
    protected int castType;
    protected int arrayDim;

    public CastExpr(ASTList className, int dim, ASTree expr) {
        super(className, new ASTList(expr));
        castType = CLASS;
        arrayDim = dim;
    }

    public CastExpr(int type, int dim, ASTree expr) {
        super(null, new ASTList(expr));
        castType = type;
        arrayDim = dim;
    }

    /* Returns CLASS, BOOLEAN, INT, or ...
     */
    public int getType() { return castType; }

    public int getArrayDim() { return arrayDim; }

    public ASTList getClassName() { return (ASTList)getLeft(); }

    public ASTree getOprand() { return getRight().getLeft(); }

    public void setOprand(ASTree t) { getRight().setLeft(t); }

    public String getTag() { return "cast:" + castType + ":" + arrayDim; }

    public void accept(Visitor v) throws CompileError { v.atCastExpr(this); }
}
