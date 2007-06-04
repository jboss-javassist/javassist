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
