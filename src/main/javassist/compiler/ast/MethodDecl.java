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

public class MethodDecl extends ASTList {
    public static final String initName = "<init>";

    public MethodDecl(ASTree _head, ASTList _tail) {
        super(_head, _tail);
    }

    public boolean isConstructor() {
        Symbol sym = getReturn().getVariable();
        return sym != null && initName.equals(sym.get());
    }

    public ASTList getModifiers() { return (ASTList)getLeft(); }

    public Declarator getReturn() { return (Declarator)tail().head(); }

    public ASTList getParams() { return (ASTList)sublist(2).head(); }

    public ASTList getThrows() { return (ASTList)sublist(3).head(); }

    public Stmnt getBody() { return (Stmnt)sublist(4).head(); }

    public void accept(Visitor v) throws CompileError {
        v.atMethodDecl(this);
    }
}
