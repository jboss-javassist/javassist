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

    public String getTag() { return "cast:" + castType + ":" + arrayDim; }

    public void accept(Visitor v) throws CompileError { v.atCastExpr(this); }
}
