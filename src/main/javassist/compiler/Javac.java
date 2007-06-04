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

package javassist.compiler;

import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.CtMember;
import javassist.CtField;
import javassist.CtBehavior;
import javassist.CtMethod;
import javassist.CtConstructor;
import javassist.CannotCompileException;
import javassist.Modifier;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Opcode;
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
            else {
                CtBehavior cb = compileMethod(p, (MethodDecl)mem);
                CtClass decl = cb.getDeclaringClass();
                cb.getMethodInfo2()
                  .rebuildStackMapIf6(decl.getClassPool(),
                                      decl.getClassFile2());
                return cb;
            }
        }
        catch (BadBytecode bb) {
            throw new CompileError(bb.getMessage());
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
        f = new CtFieldWithInit(gen.resolver.lookupClass(d),
                                d.getVariable().get(), gen.getThisClass());
        f.setModifiers(MemberResolver.getModifiers(fd.getModifiers()));
        if (fd.getInit() != null)
            f.setInit(fd.getInit());

        return f;
    }

    private CtBehavior compileMethod(Parser p, MethodDecl md)
        throws CompileError
    {
        int mod = MemberResolver.getModifiers(md.getModifiers());
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
                CtClass rtype = gen.resolver.lookupClass(r);
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
     *          If null, this method produces a body returning zero or null.
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

            if (src == null)
                makeDefaultBody(bytecode, rtype);
            else {
                Parser p = new Parser(new Lex(src));
                SymbolTable stb = new SymbolTable(stable);
                Stmnt s = p.parseStatement(stb);
                if (p.hasMore())
                    throw new CompileError(
                        "the method/constructor body must be surrounded by {}");

                boolean callSuper = false;
                if (method instanceof CtConstructor)
                    callSuper = !((CtConstructor)method).isClassInitializer();

                gen.atMethodBody(s, callSuper, isVoid);
            }

            return bytecode;
        }
        catch (NotFoundException e) {
            throw new CompileError(e.toString());
        }
    }

    private static void makeDefaultBody(Bytecode b, CtClass type) {
        int op;
        int value;
        if (type instanceof CtPrimitiveType) {
            CtPrimitiveType pt = (CtPrimitiveType)type;
            op = pt.getReturnOp();
            if (op == Opcode.DRETURN)
                value = Opcode.DCONST_0;
            else if (op == Opcode.FRETURN)
                value = Opcode.FCONST_0;
            else if (op == Opcode.LRETURN)
                value = Opcode.LCONST_0;
            else if (op == Opcode.RETURN)
                value = Opcode.NOP;
            else
                value = Opcode.ICONST_0;
        }
        else {
            op = Opcode.ARETURN;
            value = Opcode.ACONST_NULL;
        }

        if (value != Opcode.NOP)
            b.addOpcode(value);

        b.addOpcode(op);
    }

    /**
     * Records local variables available at the specified program counter.
     * If the LocalVariableAttribute is not available, this method does not
     * record any local variable.  It only returns false.
     *
     * @param pc    program counter (&gt;= 0)
     * @return false if the CodeAttribute does not include a
     *              LocalVariableAttribute.
     */
    public boolean recordLocalVariables(CodeAttribute ca, int pc)
        throws CompileError
    {
        LocalVariableAttribute va
            = (LocalVariableAttribute)
              ca.getAttribute(LocalVariableAttribute.tag);
        if (va == null)
            return false;

        int n = va.tableLength();
        for (int i = 0; i < n; ++i) {
            int start = va.startPc(i);
            int len = va.codeLength(i);
            if (start <= pc && pc < start + len)
                gen.recordVariable(va.descriptor(i), va.variableName(i),
                                   va.index(i), stable);
        }

        return true;
    }

    /**
     * Records parameter names if the LocalVariableAttribute is available.
     * It returns false unless the LocalVariableAttribute is available.
     *
     * @param numOfLocalVars    the number of local variables used
     *                          for storing the parameters.
     * @return false if the CodeAttribute does not include a
     *              LocalVariableAttribute.
     */
    public boolean recordParamNames(CodeAttribute ca, int numOfLocalVars)
        throws CompileError
    {
        LocalVariableAttribute va
            = (LocalVariableAttribute)
              ca.getAttribute(LocalVariableAttribute.tag);
        if (va == null)
            return false;

        int n = va.tableLength();
        for (int i = 0; i < n; ++i) {
            int index = va.index(i);
            if (index < numOfLocalVars)
                gen.recordVariable(va.descriptor(i), va.variableName(i),
                                   index, stable);
        }

        return true;
    }


    /**
     * Makes variables $0 (this), $1, $2, ..., and $args represent method
     * parameters.  $args represents an array of all the parameters.
     * It also makes $$ available as a parameter list of method call.
     *
     * <p>This must be called before calling <code>compileStmnt()</code> and
     * <code>compileExpr()</code>.  The correct value of
     * <code>isStatic</code> must be recorded before compilation.
     * <code>maxLocals</code> is updated to include $0,...
     */
    public int recordParams(CtClass[] params, boolean isStatic)
        throws CompileError
    {
        return gen.recordParams(params, isStatic, "$", "$args", "$$", stable);
    }

    /**
     * Makes variables $0, $1, $2, ..., and $args represent method
     * parameters.  $args represents an array of all the parameters.
     * It also makes $$ available as a parameter list of method call.
     * $0 can represent a local variable other than THIS (variable 0).
     * $class is also made available.
     *
     * <p>This must be called before calling <code>compileStmnt()</code> and
     * <code>compileExpr()</code>.  The correct value of
     * <code>isStatic</code> must be recorded before compilation.
     * <code>maxLocals</code> is updated to include $0,...
     *
     * @paaram use0     true if $0 is used.
     * @param varNo     the register number of $0 (use0 is true)
     *                          or $1 (otherwise).
     * @param target    the type of $0 (it can be null if use0 is false).
     *                  It is used as the name of the type represented
     *                  by $class.
     * @param isStatic  true if the method in which the compiled bytecode
     *                  is embedded is static.
     */
    public int recordParams(String target, CtClass[] params,
                             boolean use0, int varNo, boolean isStatic)
        throws CompileError
    {
        return gen.recordParams(params, isStatic, "$", "$args", "$$",
                                use0, varNo, target, stable);
    }

    /**
     * Sets <code>maxLocals</code> to <code>max</code>.
     * This method tells the compiler the local variables that have been
     * allocated for the rest of the code.  When the compiler needs
     * new local variables, the local variables at the index <code>max</code>,
     * <code>max + 1</code>, ... are assigned.
     *
     * <p>This method is indirectly called by <code>recordParams</code>.
     */
    public void setMaxLocals(int max) {
        gen.setMaxLocals(max);
    }

    /**
     * Prepares to use cast $r, $w, $_, and $type.
     * $type is made to represent the specified return type.
     * It also enables to write a return statement with a return value
     * for void method.
     *
     * <p>If the return type is void, ($r) does nothing.
     * The type of $_ is java.lang.Object.
     *
     * @param type              the return type.
     * @param useResultVar      true if $_ is used.
     * @return          -1 or the variable index assigned to $_.
     * @see #recordType(CtClass)
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
     *
     * @param t     the type represented by $type.
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

                    expr = CallExpr.makeCall(expr, args);
                    gen.compileExpr(expr);
                    gen.addNullIfVoid();
                }

                public void setReturnType(JvstTypeChecker check, ASTList args)
                    throws CompileError
                {
                    ASTree expr = new Member(m);
                    if (texpr != null)
                        expr = Expr.make('.', texpr, expr);

                    expr = CallExpr.makeCall(expr, args);
                    expr.accept(check);
                    check.addNullIfVoid();
                }
            };

        gen.setProceedHandler(h, proceedName);
    }

    /**
     * Prepares to use $proceed() representing a static method.
     * If the return type of $proceed() is void, null is pushed on the
     * stack.
     *
     * @param targetClass    the fully-qualified dot-separated name
     *				of the class declaring the method.
     * @param method         the method name.
     */
    public void recordStaticProceed(String targetClass, String method)
        throws CompileError
    {
        final String c = targetClass;
        final String m = method;

        ProceedHandler h = new ProceedHandler() {
                public void doit(JvstCodeGen gen, Bytecode b, ASTList args)
                    throws CompileError
                {
                    Expr expr = Expr.make(TokenId.MEMBER,
                                          new Symbol(c), new Member(m));
                    expr = CallExpr.makeCall(expr, args);
                    gen.compileExpr(expr);
                    gen.addNullIfVoid();
                }

                public void setReturnType(JvstTypeChecker check, ASTList args)
                    throws CompileError
                {
                    Expr expr = Expr.make(TokenId.MEMBER,
                                          new Symbol(c), new Member(m));
                    expr = CallExpr.makeCall(expr, args);
                    expr.accept(check);
                    check.addNullIfVoid();
                }
            };

        gen.setProceedHandler(h, proceedName);
    }

    /**
     * Prepares to use $proceed() representing a private/super's method.
     * If the return type of $proceed() is void, null is pushed on the
     * stack.  This method is for methods invoked by INVOKESPECIAL.
     *
     * @param target    an expression specifying the target object.
     *                          if null, "this" is the target.
     * @param classname	    the class name declaring the method.
     * @param methodname    the method name.
     * @param descriptor    the method descriptor.
     */
    public void recordSpecialProceed(String target, String classname,
                                     String methodname, String descriptor)
        throws CompileError
    {
        Parser p = new Parser(new Lex(target));
        final ASTree texpr = p.parseExpression(stable);
        final String cname = classname;
        final String method = methodname;
        final String desc = descriptor;

        ProceedHandler h = new ProceedHandler() {
                public void doit(JvstCodeGen gen, Bytecode b, ASTList args)
                    throws CompileError
                {
                    gen.compileInvokeSpecial(texpr, cname, method, desc, args);
                }

                public void setReturnType(JvstTypeChecker c, ASTList args)
                    throws CompileError
                {
                    c.compileInvokeSpecial(texpr, cname, method, desc, args);
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
     * in the compiled source text might not be accessible within that
     * source text.  Fields and method parameters ($0, $1, ..) are available.
     */
    public void compileStmnt(String src) throws CompileError {
        Parser p = new Parser(new Lex(src));
        SymbolTable stb = new SymbolTable(stable);
        while (p.hasMore()) {
            Stmnt s = p.parseStatement(stb);
            if (s != null)
                s.accept(gen);
        }
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
        ASTree e = parseExpr(src, stable);
        compileExpr(e);
    }

    /**
     * Parsers an expression.
     */
    public static ASTree parseExpr(String src, SymbolTable st)
        throws CompileError
    {
        Parser p = new Parser(new Lex(src));
        return p.parseExpression(st);
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
            gen.compileExpr(e);
    }
}
