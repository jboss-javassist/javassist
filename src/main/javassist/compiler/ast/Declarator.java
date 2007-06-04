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
 * Variable declarator.
 */
public class Declarator extends ASTList implements TokenId {
    protected int varType;
    protected int arrayDim;
    protected int localVar;
    protected String qualifiedClass;    // JVM-internal representation

    public Declarator(int type, int dim) {
        super(null);
        varType = type;
        arrayDim = dim;
        localVar = -1;
        qualifiedClass = null;
    }

    public Declarator(ASTList className, int dim) {
        super(null);
        varType = CLASS;
        arrayDim = dim;
        localVar = -1;
        qualifiedClass = astToClassName(className, '/');
    }

    /* For declaring a pre-defined? local variable.
     */
    public Declarator(int type, String jvmClassName, int dim,
                      int var, Symbol sym) {
        super(null);
        varType = type;
        arrayDim = dim;
        localVar = var;
        qualifiedClass = jvmClassName;
        setLeft(sym);
        append(this, null);     // initializer
    }

    public Declarator make(Symbol sym, int dim, ASTree init) {
        Declarator d = new Declarator(this.varType, this.arrayDim + dim);
        d.qualifiedClass = this.qualifiedClass;
        d.setLeft(sym);
        append(d, init);
        return d;
    }

    /* Returns CLASS, BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, FLOAT,
     * or DOUBLE (or VOID)
     */
    public int getType() { return varType; }

    public int getArrayDim() { return arrayDim; }

    public void addArrayDim(int d) { arrayDim += d; }

    public String getClassName() { return qualifiedClass; }

    public void setClassName(String s) { qualifiedClass = s; }

    public Symbol getVariable() { return (Symbol)getLeft(); }

    public void setVariable(Symbol sym) { setLeft(sym); }

    public ASTree getInitializer() {
        ASTList t = tail();
        if (t != null)
            return t.head();
        else
            return null;
    }

    public void setLocalVar(int n) { localVar = n; }

    public int getLocalVar() { return localVar; }

    public String getTag() { return "decl"; }

    public void accept(Visitor v) throws CompileError {
        v.atDeclarator(this);
    }

    public static String astToClassName(ASTList name, char sep) {
        if (name == null)
            return null;

        StringBuffer sbuf = new StringBuffer();
        astToClassName(sbuf, name, sep);
        return sbuf.toString();
    }

    private static void astToClassName(StringBuffer sbuf, ASTList name,
                                       char sep) {
        for (;;) {
            ASTree h = name.head();
            if (h instanceof Symbol)
                sbuf.append(((Symbol)h).get());
            else if (h instanceof ASTList)
                astToClassName(sbuf, (ASTList)h, sep);

            name = name.tail();
            if (name == null)
                break;

            sbuf.append(sep);
        }
    }
}
