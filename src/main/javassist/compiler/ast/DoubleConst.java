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

import javassist.compiler.CompileError;

/**
 * Double constant.
 */
public class DoubleConst extends ASTree {
    protected double value;
    protected int type;

    public DoubleConst(double v, int tokenId) { value = v; type = tokenId; }

    public double get() { return value; }

    /* Returns DoubleConstant or FloatConstant
     */
    public int getType() { return type; }

    public String toString() { return Double.toString(value); }

    public void accept(Visitor v) throws CompileError {
        v.atDoubleConst(this);
    }
}
