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
 * New Expression.
 */
public class NewExpr extends ASTList implements TokenId {
    protected boolean newArray;
    protected int arrayType;

    public NewExpr(ASTList className, ASTList args) {
        super(className, new ASTList(args));
        newArray = false;
        arrayType = CLASS;
    }

    public NewExpr(int type, ASTList arraySize, ASTree init) {
        super(null, new ASTList(arraySize));
        newArray = true;
        arrayType = type;
        if (init != null)
            append(this, init);
    }

    public static NewExpr makeObjectArray(ASTList className,
                                          ASTList arraySize, ASTree init) {
        NewExpr e = new NewExpr(className, arraySize);
        e.newArray = true;
        if (init != null)
            append(e, init);

        return e;
    }

    public boolean isArray() { return newArray; }

    /* TokenId.CLASS, TokenId.INT, ...
     */
    public int getArrayType() { return arrayType; }

    public ASTList getClassName() { return (ASTList)getLeft(); }

    public ASTList getArguments() { return (ASTList)getRight().getLeft(); }

    public ASTList getArraySize() { return getArguments(); }

    public ASTree getInitializer() {
        ASTree t = getRight().getRight();
        if (t == null)
            return null;
        else
            return t.getLeft();
    }

    public void accept(Visitor v) throws CompileError { v.atNewExpr(this); }

    protected String getTag() {
        return newArray ? "new[]" : "new";
    }
}
