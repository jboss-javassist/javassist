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
 * The visitor pattern.
 *
 * @see ast.ASTree#accept(Visitor)
 */
public class Visitor {
    public void atASTList(ASTList n) throws CompileError {}
    public void atPair(Pair n) throws CompileError {}

    public void atFieldDecl(FieldDecl n) throws CompileError {}
    public void atMethodDecl(MethodDecl n) throws CompileError {}
    public void atStmnt(Stmnt n) throws CompileError {}
    public void atDeclarator(Declarator n) throws CompileError {}

    public void atAssignExpr(AssignExpr n) throws CompileError {}
    public void atCondExpr(CondExpr n) throws CompileError {}
    public void atBinExpr(BinExpr n) throws CompileError {}
    public void atExpr(Expr n) throws CompileError {}
    public void atCastExpr(CastExpr n) throws CompileError {}
    public void atInstanceOfExpr(InstanceOfExpr n) throws CompileError {}
    public void atNewExpr(NewExpr n) throws CompileError {}

    public void atSymbol(Symbol n) throws CompileError {}
    public void atMember(Member n) throws CompileError {}
    public void atVariable(Variable n) throws CompileError {}
    public void atKeyword(Keyword n) throws CompileError {}
    public void atStringL(StringL n) throws CompileError {}
    public void atIntConst(IntConst n) throws CompileError {}
    public void atDoubleConst(DoubleConst n) throws CompileError {}
}
