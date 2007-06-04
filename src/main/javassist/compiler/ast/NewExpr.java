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

    public NewExpr(int type, ASTList arraySize, ArrayInit init) {
        super(null, new ASTList(arraySize));
        newArray = true;
        arrayType = type;
        if (init != null)
            append(this, init);
    }

    public static NewExpr makeObjectArray(ASTList className,
                                          ASTList arraySize, ArrayInit init) {
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

    public ArrayInit getInitializer() {
        ASTree t = getRight().getRight();
        if (t == null)
            return null;
        else
            return (ArrayInit)t.getLeft();
    }

    public void accept(Visitor v) throws CompileError { v.atNewExpr(this); }

    protected String getTag() {
        return newArray ? "new[]" : "new";
    }
}
