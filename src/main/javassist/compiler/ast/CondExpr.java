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

import javassist.compiler.CompileError;

/**
 * Conditional expression.
 */
public class CondExpr extends ASTList {
    public CondExpr(ASTree cond, ASTree thenp, ASTree elsep) {
	super(cond, new ASTList(thenp, new ASTList(elsep)));
    }

    public ASTree condExpr() { return head(); }

    public ASTree thenExpr() { return tail().head(); }

    public ASTree elseExpr() { return tail().tail().head(); }

    public String getTag() { return "?:"; }

    public void accept(Visitor v) throws CompileError { v.atCondExpr(this); }
}
