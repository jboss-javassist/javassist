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

package javassist.compiler;

import javassist.CtClass;
import javassist.CtMember;
import javassist.CtField;
import javassist.CtBehavior;
import javassist.CtMethod;
import javassist.CtConstructor;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.Modifier;
import javassist.bytecode.Bytecode;
import javassist.NotFoundException;

import javassist.compiler.ast.*;

public class Javac {
    JvstCodeGen gen;
    SymbolTable stable;
    private Bytecode bytecode;

    public static final String param0Name = "$0";
    public static final String resultVarName = "$_";
    public static final String proceedName = "$proceed";

    /**
     * Constructs a compiler.
     *
     * @param thisClass         the class that a compiled method/field
     *                          belongs to.
     */
    public Javac(CtClass thisClass) {
        this(new Bytecode(thisClass.getClassFile2().getConstPool(), 0, 0),
             thisClass);
    }

    /**
     * Constructs a compiler.
     * The produced bytecode is stored in the <code>Bytecode</code> object
     * specified by <code>b</code>.
     *
     * @param thisClass         the class that a compiled method/field
     *                          belongs to.
     */
    public Javac(Bytecode b, CtClass thisClass) {
        gen = new JvstCodeGen(b, thisClass, thisClass.getClassPool());
        stable = new SymbolTable();
        bytecode = b;
    }

    /**
     * Returns the produced bytecode.
     */
    public Bytecode getBytecode() { return bytecode; }

    /**
     * Compiles a method, constructor, or field declaration
     * to a class.
     * A field declaration can declare only one field.
     *
     * <p>In a method or constructor body, $0, $1, ... and $_
     * are not available.
     *
     * @return          a <code>CtMethod</code>, <code>CtConstructor</code>,
     *                  or <code>CtField</code> object.
     * @see #recordProceed(String,String)
     */
    public CtMember compile(String src) throws CompileError {
        Parser p = new Parser(new Lex(src));
        ASTList mem = p.parseMember1(stable);
        try {
            if (mem instanceof FieldDecl)
                return compileField((FieldDecl)mem);
            else
                return compileMethod(p, (MethodDecl)mem);
        }
        catch (CannotCompileException e) {
            throw new CompileError(e.getMessage());
        }
    }

    public static class CtFieldWithInit extends CtField {
        private ASTree init;

        CtFieldWithInit(CtClass type, String name, CtClass declaring)
            throws CannotCompileException
        {
            super(type, name, declaring);
            init = null;
        }

        protected void setInit(ASTree i) { init = i; }

        protected ASTree getInitAST() {
            return init;
        }
    }

    private CtField compileField(FieldDecl fd)
        throws CompileError, CannotCompileException
    {
        CtFieldWithInit f;
        Declarator d = fd.getDeclarator();
        f = new CtFieldWithInit(gen.lookupClass(d), d.getVariable().get(),
                                gen.getThisClass());
        f.setModifiers(gen.getModifiers(fd.getModifiers()));
        if (fd.getInit() != null)
            f.setInit(fd.getInit());

        return f;
    }

    private CtMember compileMethod(Parser p, MethodDecl md)
        throws CompileError
    {
        int mod = gen.getModifiers(md.getModifiers());
        CtClass[] plist = gen.makeParamList(md);
        CtClass[] tlist = gen.makeThrowsList(md);
        recordParams(plist, Modifier.isStatic(mod));
        md = p.parseMethod2(stable, md);
        try {
            if (md.isConstructor()) {
                CtConstructor cons = new CtConstructor(plist,
                                                   gen.getThisClass());
                cons.setModifiers(mod);
                md.accept(gen);
                cons.getMethodInfo().setCodeAttribute(
                                        bytecode.toCodeAttribute());
                cons.setExceptionTypes(tlist);
                return cons;
            }
            else {
                Declarator r = md.getReturn();
                CtClass rtype = gen.lookupClass(r);
                recordReturnType(rtype, false);
                CtMethod method = new CtMethod(rtype, r.getVariable().get(),
                                           plist, gen.getThisClass());
                method.setModifiers(mod);
                gen.setThisMethod(method);
                md.accept(gen);
                if (md.getBody() != null)
                    method.getMethodInfo().setCodeAttribute(
                                        bytecode.toCodeAttribute());
                else
                    method.setModifiers(mod | Modifier.ABSTRACT);

                method.setExceptionTypes(tlist);
                return method;
            }
        }
        catch (NotFoundException e) {
            throw new CompileError(e.toString());
        }
    }

    /**
     * Compiles a method (or constructor) body.
     *
     * @src	a single statement or a block.
     */
    public Bytecode compileBody(CtBehavior method, String src)
        throws CompileError
    {
        try {
            int mod = method.getModifiers();
            recordParams(method.getParameterTypes(), Modifier.isStatic(mod));

            CtClass rtype;
            if (method instanceof CtMethod) {
                gen.setThisMethod((CtMethod)method);
                rtype = ((CtMethod)method).getReturnType();
            }
            else
                rtype = CtClass.voidType;

            recordReturnType(rtype, false);
            boolean isVoid = rtype == CtClass.voidType;

            Parser p = new Parser(new Lex(src));
            SymbolTable stb = new SymbolTable(stable);
            Stmnt s = p.parseStatement(stb);
            gen.atMethodBody(s, method instanceof CtConstructor, isVoid);
            return bytecode;
        }
        catch (NotFoundException e) {
            throw new CompileError(e.toString());
        }
    }

    /**
     * Makes variables $0 (this), $1, $2, ..., and $args represent method
     * parameters.  $args represents an array of all the parameters.
     * It also makes $$ available as a parameter list of method call.
     *
     * <p>This must be called before calling <code>compileStmnt()</code> and
     * <code>compileExpr()</code>.  The correct value of
     * <code>isStatic</code> must be recorded before compilation.
     */
    public void recordParams(CtClass[] params, boolean isStatic)
        throws CompileError
    {
        gen.recordParams(params, isStatic, "$", "$args", "$$", stable);
    }

    /**
     * Makes variables $0, $1, $2, ..., and $args represent method
     * parameters.  $args represents an array of all the parameters.
     * It also makes $$ available as a parameter list of method call.
     * $0 can represent a local variable other than THIS (variable 0).
     *
     * <p>This must be called before calling <code>compileStmnt()</code> and
     * <code>compileExpr()</code>.  The correct value of
     * <code>isStatic</code> must be recorded before compilation.
     *
     * @paaram use0     true if $0 is used.
     * @param varNo     the register number of $0 (use0 is true)
     *                          or $1 (otherwise).
     * @param target    the type of $0 (it can be null if use0 is false).
     * @param isStatic  true if the method in which the compiled bytecode
     *                  is embedded is static.
     */
    public void recordParams(String target, CtClass[] params,
                             boolean use0, int varNo, boolean isStatic)
        throws CompileError
    {
        gen.recordParams(params, isStatic, "$", "$args", "$$",
                         use0, varNo, target, stable);
    }

    /**
     * Prepares to use cast $r, $w, $_, and $type.
     * It also enables to write a return statement with a return value
     * for void method.
     *
     * <p>If the return type is void, ($r) does nothing.
     * The type of $_ is java.lang.Object.
     *
     * @param useResultVar      true if $_ is used.
     * @return          -1 or the variable index assigned to $_.
     */
    public int recordReturnType(CtClass type, boolean useResultVar)
        throws CompileError
    {
        gen.recordType(type);
        return gen.recordReturnType(type, "$r",
                        (useResultVar ? resultVarName : null), stable);
    }

    /**
     * Prepares to use $type.  Note that recordReturnType() overwrites
     * the value of $type.
     */
    public void recordType(CtClass t) {
        gen.recordType(t);
    }

    /**
     * Makes the given variable available.
     *
     * @param type      variable type
     * @param name      variable name
     */
    public int recordVariable(CtClass type, String name)
        throws CompileError
    {
        return gen.recordVariable(type, name, stable);
    }

    /**
     * Prepares to use $proceed().
     * If the return type of $proceed() is void, null is pushed on the
     * stack.
     *
     * @param target    an expression specifying the target object.
     *                          if null, "this" is the target.
     * @param method    the method name.
     */
    public void recordProceed(String target, String method)
        throws CompileError
    {
        Parser p = new Parser(new Lex(target));
        final ASTree texpr = p.parseExpression(stable);
        final String m = method;

        ProceedHandler h = new ProceedHandler() {
                public void doit(JvstCodeGen gen, Bytecode b, ASTList args)
                    throws CompileError
                {
                    ASTree expr = new Member(m);
                    if (texpr != null)
                        expr = Expr.make('.', texpr, expr);

                    expr = Expr.make(TokenId.CALL, expr, args);
                    expr.accept(gen);
                    gen.addNullIfVoid();
                }
            };

        gen.setProceedHandler(h, proceedName);
    }

    /**
     * Prepares to use $proceed().
     */
    public void recordProceed(ProceedHandler h) {
        gen.setProceedHandler(h, proceedName);
    }

    /**
     * Compiles a statement (or a block).
     * <code>recordParams()</code> must be called before invoking
     * this method.
     *
     * <p>Local variables that are not declared
     * in the compiled source text are not accessible within that
     * source text.  Fields and method parameters
     * ($0, $1, ..) are available.
     */
    public void compileStmnt(String src) throws CompileError {
        Parser p = new Parser(new Lex(src));
        SymbolTable stb = new SymbolTable(stable);
     // while (p.hasMore()) {
            Stmnt s = p.parseStatement(stb);
            if (s != null)
                s.accept(gen);
     // }
    }

    /**
     * Compiles an exression.  <code>recordParams()</code> must be
     * called before invoking this method.
     *
     * <p>Local variables are not accessible
     * within the compiled source text.  Fields and method parameters
     * ($0, $1, ..) are available if <code>recordParams()</code>
     * have been invoked.
     */
    public void compileExpr(String src) throws CompileError {
        Parser p = new Parser(new Lex(src));
        ASTree e = p.parseExpression(stable);
        compileExpr(e);
    }

    /**
     * Compiles an exression.  <code>recordParams()</code> must be
     * called before invoking this method.
     *
     * <p>Local variables are not accessible
     * within the compiled source text.  Fields and method parameters
     * ($0, $1, ..) are available if <code>recordParams()</code>
     * have been invoked.
     */
    public void compileExpr(ASTree e) throws CompileError {
        if (e != null)
            e.accept(gen);
    }
}
