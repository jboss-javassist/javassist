/*
 * This file is part of the Javassist toolkit.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * either http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is Javassist.
 *
 * The Initial Developer of the Original Code is Shigeru Chiba.  Portions
 * created by Shigeru Chiba are Copyright (C) 1999-2003 Shigeru Chiba.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * The development of this software is supported in part by the PRESTO
 * program (Sakigake Kenkyu 21) of Japan Science and Technology Corporation.
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
