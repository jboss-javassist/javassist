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
 * Variable.
 */
public class Variable extends Symbol {
    protected Declarator declarator;

    public Variable(String sym, Declarator d) {
        super(sym);
        declarator = d;
    }

    public Declarator getDeclarator() { return declarator; }

    public String toString() {
        return identifier + ":" + declarator.getType();
    }

    public void accept(Visitor v) throws CompileError { v.atVariable(this); }
}
