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
