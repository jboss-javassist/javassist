/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2004 Shigeru Chiba. All Rights Reserved.
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
import javassist.CtField;
import javassist.Modifier;
import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.compiler.ast.*;
import javassist.bytecode.*;

/**
 * This class does type checking and, if needed, transformes the original
 * abstract syntax tree.  The resulting tree is available from
 * this.modifiedExpr.
 */
public class TypeChecker extends Visitor implements Opcode, TokenId {
    static final String javaLangObject = "java.lang.Object";
    static final String jvmJavaLangObject = "java/lang/Object";
    static final String jvmJavaLangString = "java/lang/String";
    static final String jvmJavaLangClass = "java/lang/Class";

    /* The following fields are used by atXXX() methods
     * for returning the type of the compiled expression.
     */
    protected int exprType;     // VOID, NULL, CLASS, BOOLEAN, INT, ...
    protected int arrayDim;
    protected String className; // JVM-internal representation
    protected ASTree modifiedExpr; // null if the given expr was not changed

    protected MemberResolver resolver;
    protected CtClass   thisClass;
    protected MethodInfo thisMethod;

    public TypeChecker(CtClass cc, ClassPool cp) {
        resolver = new MemberResolver(cp);
        thisClass = cc;
        thisMethod = null;
    }

    /**
     * Records the currently compiled method.
     */
    public void setThisMethod(MethodInfo m) {
        thisMethod = m;
    }

    protected static void fatal() throws CompileError {
        throw new CompileError("fatal");
    }

    /**
     * Returns the JVM-internal representation of this class name.
     */
    protected String getThisName() {
        return MemberResolver.javaToJvmName(thisClass.getName());
    }

    /**
     * Returns the JVM-internal representation of this super class name.
     */
    protected String getSuperName() throws CompileError {
        return MemberResolver.javaToJvmName(
                        MemberResolver.getSuperclass(thisClass).getName());
    }

    /* Converts a class name into a JVM-internal representation.
     *
     * It may also expand a simple class name to java.lang.*.
     * For example, this converts Object into java/lang/Object.
     */
    protected String resolveClassName(ASTList name) throws CompileError {
        return resolver.resolveClassName(name);
    }

    /* Expands a simple class name to java.lang.*.
     * For example, this converts Object into java/lang/Object.
     */
    protected String resolveClassName(String jvmName) throws CompileError {
        return resolver.resolveJvmClassName(jvmName);
    }

    public void atNewExpr(NewExpr expr) throws CompileError {
        if (expr.isArray())
            atNewArrayExpr(expr);
        else {
            CtClass clazz = resolver.lookupClassByName(expr.getClassName());
            String cname = clazz.getName();
            ASTList args = expr.getArguments();
            atMethodCallCore(clazz, MethodInfo.nameInit, args);
            exprType = CLASS;
            arrayDim = 0;
            className = MemberResolver.javaToJvmName(cname);
            modifiedExpr = null;
        }
    }

    public void atNewArrayExpr(NewExpr expr) throws CompileError {
        int type = expr.getArrayType();
        ASTList size = expr.getArraySize();
        ASTList classname = expr.getClassName();
        if (size.length() > 1)
            atMultiNewArray(type, classname, size);
        else {
            size.head().accept(this);
            if (modifiedExpr != null)
                size.setHead(modifiedExpr);

            exprType = type;
            arrayDim = 1;
            modifiedExpr = null;
            if (type == CLASS)
                className = resolveClassName(classname);
            else
                className = null;
        }
    }

    protected void atMultiNewArray(int type, ASTList classname, ASTList size)
        throws CompileError
    {
        int count, dim;
        dim = size.length();
        for (count = 0; size != null; size = size.tail()) {
            ASTree s = size.head();
            if (s == null)
                break;          // int[][][] a = new int[3][4][];

            ++count;
            s.accept(this);
            if (modifiedExpr != null)
                size.setHead(modifiedExpr);
        }

        modifiedExpr = null;
        exprType = type;
        arrayDim = dim;
        if (type == CLASS)
            className = resolveClassName(classname);
        else
            className = null;
    }

    public void atAssignExpr(AssignExpr expr) throws CompileError {
        // =, %=, &=, *=, /=, +=, -=, ^=, |=, <<=, >>=, >>>=
        int op = expr.getOperator();
        ASTree left = expr.oprand1();
        ASTree right = expr.oprand2();
        if (left instanceof Variable)
            atVariableAssign(expr, op, (Variable)left,
                             ((Variable)left).getDeclarator(),
                             right);
        else {
            if (left instanceof Expr) {
                Expr e = (Expr)left;
                if (e.getOperator() == ARRAY) {
                    atArrayAssign(expr, op, (Expr)left, right);
                    return;
                }
            }

            atFieldAssign(expr, op, left, right);
        }
    }

    /* op is either =, %=, &=, *=, /=, +=, -=, ^=, |=, <<=, >>=, or >>>=.
     *
     * expr and var can be null.
     */
    private void atVariableAssign(Expr expr, int op, Variable var,
                                  Declarator d, ASTree right)
        throws CompileError
    {
        int varType = d.getType();
        int varArray = d.getArrayDim();
        String varClass = d.getClassName();

        if (op != '=') {
            atVariable(var);
            if (modifiedExpr != null)
                expr.setOprand1(modifiedExpr);
        }

        right.accept(this);
        if (modifiedExpr != null)
            expr.setOprand2(modifiedExpr);

        exprType = varType;
        arrayDim = varArray;
        className = varClass;
        modifiedExpr = null;
    }

    private void atArrayAssign(Expr expr, int op, Expr array,
                        ASTree right) throws CompileError
    {
        atArrayRead(array);
        if (modifiedExpr != null)
            expr.setOprand1(modifiedExpr);

        int aType = exprType;
        int aDim = arrayDim;
        String cname = className;
        right.accept(this);
        if (modifiedExpr != null)
            expr.setOprand2(modifiedExpr);

        exprType = aType;
        arrayDim = aDim;
        className = cname;
        modifiedExpr = null;
    }

    protected void atFieldAssign(Expr expr, int op, ASTree left, ASTree right)
        throws CompileError
    {
        CtField f = atFieldRead(left);
        int fType = exprType;
        int fDim = arrayDim;
        String cname = className;
        if (modifiedExpr != null)
            expr.setOprand1(modifiedExpr);

        if (Modifier.isFinal(f.getModifiers()))
            throw new CompileError("assignment to a final field");

        right.accept(this);
        if (modifiedExpr != null)
            expr.setOprand2(modifiedExpr);

        exprType = fType;
        arrayDim = fDim;
        className = cname;
        modifiedExpr = null;
    }

    public void atCondExpr(CondExpr expr) throws CompileError {
        booleanExpr(expr.condExpr());
        if (modifiedExpr != null)
            expr.setCond(modifiedExpr);

        expr.thenExpr().accept(this);
        if (modifiedExpr != null)
            expr.setThen(modifiedExpr);

        int type1 = exprType;
        int dim1 = arrayDim;
        String cname1 = className;
        expr.elseExpr().accept(this);
        if (modifiedExpr != null)
            expr.setElse(modifiedExpr);

        if (dim1 == 0 && dim1 == arrayDim)
            if (CodeGen.rightIsStrong(type1, exprType))
                expr.setThen(new CastExpr(exprType, 0, expr.thenExpr()));
            else if (CodeGen.rightIsStrong(exprType, type1)) {
                expr.setElse(new CastExpr(type1, 0, expr.elseExpr()));
                exprType = type1;
            }

        modifiedExpr = null;
    }

    public void atBinExpr(BinExpr expr) throws CompileError {
        int token = expr.getOperator();
        int k = CodeGen.lookupBinOp(token);
        if (k < 0) {
            /* equation: &&, ||, ==, !=, <=, >=, <, >
            */
            booleanExpr(expr);
        }
        else {
            /* arithmetic operators: +, -, *, /, %, |, ^, &, <<, >>, >>>
             */
            if (token != '+')
                atNonPlusExpr(expr, token);
            else {
                Expr e = atPlusExpr(expr);
                if (e != null) {
                    /* String concatenation has been translated into
                     * an expression using StringBuffer.
                     */
                    e = CallExpr.makeCall(Expr.make('.', e,
                                          new Member("toString")), null);
                    className = jvmJavaLangString;
                    modifiedExpr = e;       // expr will be replaced with e.
                }
            }
        }
    }

    private void atNonPlusExpr(BinExpr expr, int token) throws CompileError {
        ASTree left = expr.oprand1();
        ASTree right = expr.oprand2();

        left.accept(this);
        if (modifiedExpr != null) {
            left = modifiedExpr;
            expr.setOprand1(left);
        }

        int type1 = exprType;
        right.accept(this);
        if (modifiedExpr != null) {
            right = modifiedExpr;
            expr.setOprand2(right);
        }

        modifiedExpr = computeConstExpr(token, left, right);
        computeBinExprType(expr, token, type1);
    }

    /* This method deals with string concatenation.  It converts a +
     * expression on String such as:
     *     "value:" + i + "."
     * into:
     *     new StringBuffer().append("value:").append(i).append(".")
     *                          .toString()
     *
     * This method also inserts a cast operator for the right operand
     * if needed.
     *
     * EXPR must be a + expression.
     *
     * atPlusExpr() returns null if the expression is not a string
     * concatenation.
     */
    private Expr atPlusExpr(BinExpr expr) throws CompileError {
        ASTree left = expr.oprand1();
        ASTree right = expr.oprand2();

        if (isPlusExpr(left)) {
            Expr newExpr = atPlusExpr((BinExpr)left);
            if (newExpr != null) {
                right.accept(this);
                if (modifiedExpr != null)
                    right = modifiedExpr;

                exprType = CLASS;
                arrayDim = 0;
                className = "java/lang/StringBuffer";
                modifiedExpr = null;
                return makeAppendCall(newExpr, right);
            }
        }
        else
            left.accept(this);

        int type1 = exprType;
        int dim1 = arrayDim;
        String cname = className;
        if (modifiedExpr != null) {
            left = modifiedExpr;
            expr.setOprand1(left);
        }

        right.accept(this);
        if (modifiedExpr != null) {
            right = modifiedExpr;
            expr.setOprand2(right);
        }

        modifiedExpr = computeConstExpr('+', left, right);
        if ((type1 == CLASS && dim1 == 0 && jvmJavaLangString.equals(cname))
            || (exprType == CLASS && arrayDim == 0
                && jvmJavaLangString.equals(className)))
        {
            exprType = CLASS;
            arrayDim = 0;
            if (modifiedExpr != null) {
                // this expression is constant.
                className = jvmJavaLangString;
                return null;
            }
            else {
                className = "java/lang/StringBuffer";
                ASTList sbufClass = ASTList.make(new Symbol("java"),
                                                 new Symbol("lang"),
                                                 new Symbol("StringBuffer"));
                ASTree e = new NewExpr(sbufClass, null);
                return makeAppendCall(makeAppendCall(e, left), right);
            }
        }
        else {
            computeBinExprType(expr, '+', type1);
            return null;
        }
    }

    private static boolean isPlusExpr(ASTree expr) {
        if (expr instanceof BinExpr) {
            BinExpr bexpr = (BinExpr)expr;
            int token = bexpr.getOperator();
            return token == '+';
        }

        return false;
    }

    private static Expr makeAppendCall(ASTree target, ASTree arg) {
        return CallExpr.makeCall(Expr.make('.', target, new Member("append")),
                                 new ASTList(arg));
    }

    private void computeBinExprType(BinExpr expr, int token, int type1)
        throws CompileError
    {
        // arrayDim should be 0.
        int type2 = exprType;
        if (token == LSHIFT || token == RSHIFT || token == ARSHIFT)
            exprType = type1;
        else
            insertCast(expr, type1, type2);

        if (CodeGen.isP_INT(exprType))
            exprType = INT;         // type1 may be BYTE, ...

        arrayDim = 0;
        // don't change the value of modifiedExpr.
    }

    private void booleanExpr(ASTree expr)
        throws CompileError
    {
        ASTree modExpr = null;
        int op = CodeGen.getCompOperator(expr);
        if (op == EQ) {         // ==, !=, ...
            BinExpr bexpr = (BinExpr)expr;
            bexpr.oprand1().accept(this);
            if (modifiedExpr != null)
                bexpr.setOprand1(modifiedExpr);

            int type1 = exprType;
            int dim1 = arrayDim;
            bexpr.oprand2().accept(this);
            if (modifiedExpr != null)
                bexpr.setOprand2(modifiedExpr);

            if (dim1 == 0 && arrayDim == 0)
                insertCast(bexpr, type1, exprType);
        }
        else if (op == '!') {
            ((Expr)expr).oprand1().accept(this);
            if (modifiedExpr != null)
                ((Expr)expr).setOprand1(modifiedExpr);
        }
        else if (op == ANDAND || op == OROR) {
            BinExpr bexpr = (BinExpr)expr;
            bexpr.oprand1().accept(this);
            if (modifiedExpr != null)
                bexpr.setOprand1(modifiedExpr);

            bexpr.oprand2().accept(this);
            if (modifiedExpr != null)
                bexpr.setOprand2(modifiedExpr);
        }
        else {               // others
            expr.accept(this);
            modExpr = modifiedExpr;
        }

        exprType = BOOLEAN;
        arrayDim = 0;
        modifiedExpr = modExpr;
    }

    private void insertCast(BinExpr expr, int type1, int type2)
        throws CompileError
    {
        if (CodeGen.rightIsStrong(type1, type2))
            expr.setLeft(new CastExpr(type2, 0, expr.oprand1()));
        else
            exprType = type1;
    }

    private ASTree computeConstExpr(int op, ASTree left, ASTree right) {
        if (left instanceof StringL && right instanceof StringL && op == '+')
            return new StringL(((StringL)left).get() + ((StringL)right).get());
        else if (left instanceof IntConst)
            return ((IntConst)left).compute(op, right);
        else if (left instanceof DoubleConst)
            return ((DoubleConst)left).compute(op, right);
        else
            return null;        // not constant expression
    }

    public void atCastExpr(CastExpr expr) throws CompileError {
        String cname = resolveClassName(expr.getClassName());
        expr.getOprand().accept(this);
        if (modifiedExpr != null)
            expr.setOprand(modifiedExpr);

        exprType = expr.getType();
        arrayDim = expr.getArrayDim();
        className = cname;
        modifiedExpr = null;
    }

    public void atInstanceOfExpr(InstanceOfExpr expr) throws CompileError {
        expr.getOprand().accept(this);
        if (modifiedExpr != null)
            expr.setOprand(modifiedExpr);

        exprType = BOOLEAN;
        arrayDim = 0;
        modifiedExpr = null;
    }

    public void atExpr(Expr expr) throws CompileError {
        // array access, member access,
        // (unary) +, (unary) -, ++, --, !, ~

        int token = expr.getOperator();
        ASTree oprand = expr.oprand1();
        if (token == '.') {
            String member = ((Symbol)expr.oprand2()).get();
            if (member.equals("length"))
                atArrayLength(expr);
            else if (member.equals("class"))                
                atClassObject(expr);  // .class
            else
                atFieldRead(expr);
        }
        else if (token == MEMBER) {     // field read
            String member = ((Symbol)expr.oprand2()).get();
            if (member.equals("class"))                
                atClassObject(expr);  // .class
            else
                atFieldRead(expr);
        }
        else if (token == ARRAY)
            atArrayRead(expr);
        else if (token == PLUSPLUS || token == MINUSMINUS)
            atPlusPlus(token, oprand, expr);
        else if (token == '!')
            booleanExpr(expr);
        else if (token == CALL)              // method call
            fatal();
        else {
            oprand.accept(this);
            if (modifiedExpr != null) {
                oprand = modifiedExpr;
                expr.setOprand1(oprand);
            }

            modifiedExpr = computeConstExpr(token, oprand);
            if (token == '-' || token == '~') {
                if (CodeGen.isP_INT(exprType))
                    exprType = INT;         // type may be BYTE, ...
            }
        }
    }

    private ASTree computeConstExpr(int op, ASTree oprand) {
        if (oprand instanceof IntConst) {
            IntConst c = (IntConst)oprand;
            long v = c.get();
            if (op == '-')
                v = -v;
            else if (op == '~')
                v = ~v;

            c.set(v);
        }
        else if (oprand instanceof DoubleConst) {
            DoubleConst c = (DoubleConst)oprand;
            if (op == '-')
                c.set(-c.get());
        }

        return null;
    }

    public void atCallExpr(CallExpr expr) throws CompileError {
        String mname = null;
        CtClass targetClass = null;
        ASTree method = expr.oprand1();
        ASTList args = (ASTList)expr.oprand2();

        if (method instanceof Member) {
            mname = ((Member)method).get();
            targetClass = thisClass;
        }
        else if (method instanceof Keyword) {   // constructor
            mname = MethodInfo.nameInit;        // <init>
            if (((Keyword)method).get() == SUPER)
                targetClass = MemberResolver.getSuperclass(thisClass);
            else
                targetClass = thisClass;
        }
        else if (method instanceof Expr) {
            Expr e = (Expr)method;
            mname = ((Symbol)e.oprand2()).get();
            int op = e.getOperator();
            if (op == MEMBER)                // static method
                targetClass
                        = resolver.lookupClass(((Symbol)e.oprand1()).get(),
                                               false);
            else if (op == '.') {
                ASTree target = e.oprand1();
                try {
                    target.accept(this);
                    if (modifiedExpr != null)
                        e.setOprand1(modifiedExpr);
                }
                catch (NoFieldException nfe) {
                    if (nfe.getExpr() != target)
                        throw nfe;

                    // it should be a static method.
                    exprType = CLASS;
                    arrayDim = 0;
                    className = nfe.getField(); // JVM-internal
                }

                if (arrayDim > 0)
                    targetClass = resolver.lookupClass(javaLangObject, true);
                else if (exprType == CLASS /* && arrayDim == 0 */)
                    targetClass = resolver.lookupClassByJvmName(className);
                else
                    badMethod();
            }
            else
                badMethod();
        }
        else
            fatal();

        MemberResolver.Method minfo
            = atMethodCallCore(targetClass, mname, args);
        expr.setMethod(minfo);
        modifiedExpr = null;
    }

    private static void badMethod() throws CompileError {
        throw new CompileError("bad method");
    }

    /**
     * modifiedExpr is not set.
     *
     * @return  a pair of the class declaring the invoked method
     *          and the MethodInfo of that method.  Never null.
     */
    public MemberResolver.Method atMethodCallCore(CtClass targetClass,
                                                  String mname, ASTList args)
        throws CompileError
    {
        int nargs = getMethodArgsLength(args);
        int[] types = new int[nargs];
        int[] dims = new int[nargs];
        String[] cnames = new String[nargs];
        atMethodArgs(args, types, dims, cnames);

        MemberResolver.Method found
            = resolver.lookupMethod(targetClass, thisMethod, mname,
                                    types, dims, cnames, false);
        if (found == null) {
            String msg;
            if (mname.equals(MethodInfo.nameInit))
                msg = "constructor not found";
            else
                msg = "Method " + mname + " not found in "
                    + targetClass.getName();

            throw new CompileError(msg);
        }

        String desc = found.info.getDescriptor();
        setReturnType(desc);
        return found;
    }

    public int getMethodArgsLength(ASTList args) {
        return ASTList.length(args);
    }

    public void atMethodArgs(ASTList args, int[] types, int[] dims,
                             String[] cnames) throws CompileError {
        int i = 0;
        while (args != null) {
            ASTree a = args.head();
            a.accept(this);
            if (modifiedExpr != null)
                args.setHead(modifiedExpr);

            types[i] = exprType;
            dims[i] = arrayDim;
            cnames[i] = className;
            ++i;
            args = args.tail();
        }
    }

    /* modifiedExpr is not set.
     */
    void setReturnType(String desc) throws CompileError {
        int i = desc.indexOf(')');
        if (i < 0)
            badMethod();

        char c = desc.charAt(++i);
        int dim = 0;
        while (c == '[') {
            ++dim;
            c = desc.charAt(++i);
        }

        arrayDim = dim;
        if (c == 'L') {
            int j = desc.indexOf(';', i + 1);
            if (j < 0)
                badMethod();

            exprType = CLASS;
            className = desc.substring(i + 1, j);
        }
        else {
            exprType = MemberResolver.descToType(c);
            className = null;
        }
    }

    private CtField atFieldRead(ASTree expr) throws CompileError {
        CtField f = fieldAccess(expr);
        FieldInfo finfo = f.getFieldInfo2();
        String type = finfo.getDescriptor();

        int i = 0;
        int dim = 0;
        char c = type.charAt(i);
        while (c == '[') {
            ++dim;
            c = type.charAt(++i);
        }

        arrayDim = dim;
        exprType = MemberResolver.descToType(c);

        if (c == 'L')
            className = type.substring(i + 1, type.indexOf(';', i + 1));
        else
            className = null;

        modifiedExpr = null; ??
        return f;
    }

    protected CtField fieldAccess(ASTree expr) throws CompileError {
        if (expr instanceof Member) {
            String name = ((Member)expr).get();
            try {
                return thisClass.getField(name);
            }
            catch (NotFoundException e) {
                // EXPR might be part of a static member access?
                throw new NoFieldException(name, expr);
            }
        }
        else if (expr instanceof Expr) {
            Expr e = (Expr)expr;
            int op = e.getOperator();
            if (op == MEMBER)
                return resolver.lookupField(((Symbol)e.oprand1()).get(),
                                            (Symbol)e.oprand2());
            else if (op == '.')
                try {
                    e.oprand1().accept(this);
                    if (modifiedExpr != null)
                        e.setOprand1(modifiedExpr);

                    if (exprType == CLASS && arrayDim == 0)
                        return resolver.lookupFieldByJvmName(className,
                                                    (Symbol)e.oprand2());
                }
                catch (NoFieldException nfe) {
                    if (nfe.getExpr() != e.oprand1())
                        throw nfe;

                    /* EXPR should be a static field.
                     * If EXPR might be part of a qualified class name,
                     * lookupFieldByJvmName2() throws NoFieldException.
                     */
                    Symbol fname = (Symbol)e.oprand2();
                    return resolver.lookupFieldByJvmName2(nfe.getField(),
                                                          fname, expr);
                }
        }

        throw new CompileError("bad filed access");
    }

    public void atClassObject(Expr expr) throws CompileError {
        exprType = CLASS;
        arrayDim = 0;
        className =jvmJavaLangClass;
        modifiedExpr = null;
    }

    public void atArrayLength(Expr expr) throws CompileError {
        expr.oprand1().accept(this);
        if (modifiedExpr != null)
            expr.setOprand1(modifiedExpr);

        exprType = INT;
        arrayDim = 0;
        modifiedExpr = null;
    }

    public void atArrayRead(Expr expr) throws CompileError {
        ASTree array = expr.oprand1();
        array.accept(this);
        if (modifiedExpr != null)
            expr.setOprand1(modifiedExpr);

        int type = exprType;
        int dim = arrayDim;
        String cname = className;

        ASTree index = expr.oprand2();
        index.accept(this);
        if (modifiedExpr != null)
            expr.setOprand2(modifiedExpr);

        exprType = type;
        arrayDim = dim - 1;
        className = cname;
        modifiedExpr = null;
    }

    private void atPlusPlus(int token, ASTree oprand, Expr expr)
        throws CompileError
    {
        boolean isPost = oprand == null;        // ++i or i++?
        if (isPost)
            oprand = expr.oprand2();

        if (oprand instanceof Variable) {
            Declarator d = ((Variable)oprand).getDeclarator();
            exprType = d.getType();
            arrayDim = d.getArrayDim();
            modifiedExpr = null;
        }
        else {
            if (oprand instanceof Expr) {
                Expr e = (Expr)oprand;
                if (e.getOperator() == ARRAY) {
                    atArrayRead(e);
                    // arrayDim should be 0.
                    int t = exprType;
                    if (t == INT || t == BYTE || t == CHAR || t == SHORT)
                        exprType = INT;

                    return;
                }
            }

            atFieldPlusPlus(oprand);
        }
    }

    protected void atFieldPlusPlus(ASTree oprand) throws CompileError
    {
        atFieldRead(oprand);
        int t = exprType;
        if (t == INT || t == BYTE || t == CHAR || t == SHORT)
            exprType = INT;
    }

    public void atMember(Member mem) throws CompileError {
        atFieldRead(mem);
    }

    public void atVariable(Variable v) throws CompileError {
        Declarator d = v.getDeclarator();
        exprType = d.getType();
        arrayDim = d.getArrayDim();
        className = d.getClassName();
    }

    public void atKeyword(Keyword k) throws CompileError {
        arrayDim = 0;
        int token = k.get();
        switch (token) {
        case TRUE :
        case FALSE :
            exprType = BOOLEAN;
            break;
        case NULL :
            exprType = NULL;
            break;
        case THIS :
        case SUPER :
            exprType = CLASS;
            if (token == THIS)
                className = getThisName();
            else
                className = getSuperName();             
            break;
        default :
            fatal();
        }
    }

    public void atStringL(StringL s) throws CompileError {
        exprType = CLASS;
        arrayDim = 0;
        className = jvmJavaLangString;
    }

    public void atIntConst(IntConst i) throws CompileError {
        arrayDim = 0;
        int type = i.getType();
        if (type == IntConstant || type == CharConstant)
            exprType = (type == IntConstant ? INT : CHAR);
        else
            exprType = LONG;
    }

    public void atDoubleConst(DoubleConst d) throws CompileError {
        arrayDim = 0;
        if (d.getType() == DoubleConstant)
            exprType = DOUBLE;
        else
            exprType = FLOAT;
    }
}
