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

/**
 * Conditional expression.
 */
public class CondExpr extends ASTList {
    public CondExpr(ASTree cond, ASTree thenp, ASTree elsep) {
        super(cond, new ASTList(thenp, new ASTList(elsep)));
    }

    public ASTree condExpr() { return head(); }

    public void setCond(ASTree t) { setHead(t); }

    public ASTree thenExpr() { return tail().head(); }

    public void setThen(ASTree t) { tail().setHead(t); } 

    public ASTree elseExpr() { return tail().tail().head(); }

    public void setElse(ASTree t) { tail().tail().setHead(t); } 

    public String getTag() { return "?:"; }

    public void accept(Visitor v) throws CompileError { v.atCondExpr(this); }
}
