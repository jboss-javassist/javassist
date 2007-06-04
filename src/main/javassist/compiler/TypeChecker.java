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
import javassist.CtField;
import javassist.ClassPool;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.compiler.ast.*;
import javassist.bytecode.*;

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

    protected MemberResolver resolver;
    protected CtClass   thisClass;
    protected MethodInfo thisMethod;

    public TypeChecker(CtClass cc, ClassPool cp) {
        resolver = new MemberResolver(cp);
        thisClass = cc;
        thisMethod = null;
    }

    /*
     * Converts an array of tuples of exprType, arrayDim, and className
     * into a String object.
     */
    protected static String argTypesToString(int[] types, int[] dims,
                                             String[] cnames) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append('(');
        int n = types.length;
        if (n > 0) {
            int i = 0;
            while (true) {
                typeToString(sbuf, types[i], dims[i], cnames[i]);
                if (++i < n)
                    sbuf.append(',');
                else
                    break;
            }
        }

        sbuf.append(')');
        return sbuf.toString();
    }

    /*
     * Converts a tuple of exprType, arrayDim, and className
     * into a String object.
     */
    protected static StringBuffer typeToString(StringBuffer sbuf,
                                        int type, int dim, String cname) {
        String s;
        if (type == CLASS)
            s = MemberResolver.jvmToJavaName(cname);
        else if (type == NULL)
            s = "Object";
        else
            try {
                s = MemberResolver.getTypeName(type);
            }
            catch (CompileError e) {
                s = "?";
            }

        sbuf.append(s);
        while (dim-- > 0)
            sbuf.append("[]");

        return sbuf;
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
        }
    }

    public void atNewArrayExpr(NewExpr expr) throws CompileError {
        int type = expr.getArrayType();
        ASTList size = expr.getArraySize();
        ASTList classname = expr.getClassName();
        ASTree init = expr.getInitializer();
        if (init != null)
            init.accept(this);

        if (size.length() > 1)
            atMultiNewArray(type, classname, size);
        else {
            ASTree sizeExpr = size.head();
            if (sizeExpr != null)
                sizeExpr.accept(this);

            exprType = type;
            arrayDim = 1;
            if (type == CLASS)
                className = resolveClassName(classname);
            else
                className = null;
        }
    }

    public void atArrayInit(ArrayInit init) throws CompileError {
        ASTList list = init;
        while (list != null) {
            ASTree h = list.head();
            list = list.tail();
            if (h != null)
                h.accept(this);
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
        }

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

        if (op != '=')
            atVariable(var);

        right.accept(this);
        exprType = varType;
        arrayDim = varArray;
        className = varClass;
    }

    private void atArrayAssign(Expr expr, int op, Expr array,
                        ASTree right) throws CompileError
    {
        atArrayRead(array.oprand1(), array.oprand2());
        int aType = exprType;
        int aDim = arrayDim;
        String cname = className;
        right.accept(this);
        exprType = aType;
        arrayDim = aDim;
        className = cname;
    }

    protected void atFieldAssign(Expr expr, int op, ASTree left, ASTree right)
        throws CompileError
    {
        CtField f = fieldAccess(left);
        atFieldRead(f);
        int fType = exprType;
        int fDim = arrayDim;
        String cname = className;
        right.accept(this);
        exprType = fType;
        arrayDim = fDim;
        className = cname;
    }

    public void atCondExpr(CondExpr expr) throws CompileError {
        booleanExpr(expr.condExpr());
        expr.thenExpr().accept(this);
        int type1 = exprType;
        int dim1 = arrayDim;
        String cname1 = className;
        expr.elseExpr().accept(this);

        if (dim1 == 0 && dim1 == arrayDim)
            if (CodeGen.rightIsStrong(type1, exprType))
                expr.setThen(new CastExpr(exprType, 0, expr.thenExpr()));
            else if (CodeGen.rightIsStrong(exprType, type1)) {
                expr.setElse(new CastExpr(type1, 0, expr.elseExpr()));
                exprType = type1;
            }
    }

    /*
     * If atBinExpr() substitutes a new expression for the original
     * binary-operator expression, it changes the operator name to '+'
     * (if the original is not '+') and sets the new expression to the
     * left-hand-side expression and null to the right-hand-side expression. 
     */
    public void atBinExpr(BinExpr expr) throws CompileError {
        int token = expr.getOperator();
        int k = CodeGen.lookupBinOp(token);
        if (k >= 0) {
            /* arithmetic operators: +, -, *, /, %, |, ^, &, <<, >>, >>>
             */
            if (token == '+') {
                Expr e = atPlusExpr(expr);
                if (e != null) {
                    /* String concatenation has been translated into
                     * an expression using StringBuffer.
                     */
                    e = CallExpr.makeCall(Expr.make('.', e,
                                            new Member("toString")), null);
                    expr.setOprand1(e);
                    expr.setOprand2(null);    // <---- look at this!
                    className = jvmJavaLangString;
                }
            }
            else {
                ASTree left = expr.oprand1();
                ASTree right = expr.oprand2();
                left.accept(this);
                int type1 = exprType;
                right.accept(this);
                if (!isConstant(expr, token, left, right))
                    computeBinExprType(expr, token, type1);
            }
        }
        else {
            /* equation: &&, ||, ==, !=, <=, >=, <, >
            */
            booleanExpr(expr);
        }
    }

    /* EXPR must be a + expression.
     * atPlusExpr() returns non-null if the given expression is string
     * concatenation.  The returned value is "new StringBuffer().append..".
     */
    private Expr atPlusExpr(BinExpr expr) throws CompileError {
        ASTree left = expr.oprand1();
        ASTree right = expr.oprand2();
        if (right == null) {
            // this expression has been already type-checked.
            // see atBinExpr() above.
            left.accept(this);
            return null;
        }

        if (isPlusExpr(left)) {
            Expr newExpr = atPlusExpr((BinExpr)left);
            if (newExpr != null) {
                right.accept(this);
                exprType = CLASS;
                arrayDim = 0;
                className = "java/lang/StringBuffer";
                return makeAppendCall(newExpr, right);
            }
        }
        else
            left.accept(this);

        int type1 = exprType;
        int dim1 = arrayDim;
        String cname = className;
        right.accept(this);

        if (isConstant(expr, '+', left, right))
            return null;

        if ((type1 == CLASS && dim1 == 0 && jvmJavaLangString.equals(cname))
            || (exprType == CLASS && arrayDim == 0
                && jvmJavaLangString.equals(className))) {
            ASTList sbufClass = ASTList.make(new Symbol("java"),
                            new Symbol("lang"), new Symbol("StringBuffer"));
            ASTree e = new NewExpr(sbufClass, null);
            exprType = CLASS;
            arrayDim = 0;
            className = "java/lang/StringBuffer";
            return makeAppendCall(makeAppendCall(e, left), right);
        }
        else {
            computeBinExprType(expr, '+', type1);
            return null;
        }
    }

    private boolean isConstant(BinExpr expr, int op, ASTree left,
                               ASTree right) throws CompileError
    {
        left = stripPlusExpr(left);
        right = stripPlusExpr(right);
        ASTree newExpr = null;
        if (left instanceof StringL && right instanceof StringL && op == '+')
            newExpr = new StringL(((StringL)left).get()
                                  + ((StringL)right).get());
        else if (left instanceof IntConst)
            newExpr = ((IntConst)left).compute(op, right);
        else if (left instanceof DoubleConst)
            newExpr = ((DoubleConst)left).compute(op, right);

        if (newExpr == null)
            return false;       // not a constant expression
        else {
            expr.setOperator('+');
            expr.setOprand1(newExpr);
            expr.setOprand2(null);
            newExpr.accept(this);   // for setting exprType, arrayDim, ...
            return true;
        }
    }

    /* CodeGen.atSwitchStmnt() also calls stripPlusExpr().
     */
    static ASTree stripPlusExpr(ASTree expr) {
        if (expr instanceof BinExpr) {
            BinExpr e = (BinExpr)expr;
            if (e.getOperator() == '+' && e.oprand2() == null)
                return e.getLeft();
        }
        else if (expr instanceof Expr) {    // note: BinExpr extends Expr.
            Expr e = (Expr)expr;
            int op = e.getOperator();
            if (op == MEMBER) {
                ASTree cexpr = getConstantFieldValue((Member)e.oprand2());
                if (cexpr != null)
                    return cexpr;
            }
            else if (op == '+' && e.getRight() == null)
                return e.getLeft();
        }
        else if (expr instanceof Member) {
            ASTree cexpr = getConstantFieldValue((Member)expr);
            if (cexpr != null)
                return cexpr;
        }

        return expr;
    }

    /**
     * If MEM is a static final field, this method returns a constant
     * expression representing the value of that field.
     */
    private static ASTree getConstantFieldValue(Member mem) {
        return getConstantFieldValue(mem.getField());
    }

    public static ASTree getConstantFieldValue(CtField f) {
        if (f == null)
            return null;

        Object value = f.getConstantValue();
        if (value == null)
            return null;

        if (value instanceof String)
            return new StringL((String)value);
        else if (value instanceof Double || value instanceof Float) {
            int token = (value instanceof Double)
                        ? DoubleConstant : FloatConstant;
            return new DoubleConst(((Number)value).doubleValue(), token);
        }
        else if (value instanceof Number) {
            int token = (value instanceof Long) ? LongConstant : IntConstant; 
            return new IntConst(((Number)value).longValue(), token);
        }
        else if (value instanceof Boolean)
            return new Keyword(((Boolean)value).booleanValue()
                               ? TokenId.TRUE : TokenId.FALSE);
        else
            return null;
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
    }

    private void booleanExpr(ASTree expr)
        throws CompileError
    {
        int op = CodeGen.getCompOperator(expr);
        if (op == EQ) {         // ==, !=, ...
            BinExpr bexpr = (BinExpr)expr;
            bexpr.oprand1().accept(this);
            int type1 = exprType;
            int dim1 = arrayDim;
            bexpr.oprand2().accept(this);
            if (dim1 == 0 && arrayDim == 0)
                insertCast(bexpr, type1, exprType);
        }
        else if (op == '!')
            ((Expr)expr).oprand1().accept(this);
        else if (op == ANDAND || op == OROR) {
            BinExpr bexpr = (BinExpr)expr;
            bexpr.oprand1().accept(this);
            bexpr.oprand2().accept(this);
        }
        else                // others
            expr.accept(this);

        exprType = BOOLEAN;
        arrayDim = 0;
    }

    private void insertCast(BinExpr expr, int type1, int type2)
        throws CompileError
    {
        if (CodeGen.rightIsStrong(type1, type2))
            expr.setLeft(new CastExpr(type2, 0, expr.oprand1()));
        else
            exprType = type1;
    }

    public void atCastExpr(CastExpr expr) throws CompileError {
        String cname = resolveClassName(expr.getClassName());
        expr.getOprand().accept(this);
        exprType = expr.getType();
        arrayDim = expr.getArrayDim();
        className = cname;
    }

    public void atInstanceOfExpr(InstanceOfExpr expr) throws CompileError {
        expr.getOprand().accept(this);
        exprType = BOOLEAN;
        arrayDim = 0;
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
            atArrayRead(oprand, expr.oprand2());
        else if (token == PLUSPLUS || token == MINUSMINUS)
            atPlusPlus(token, oprand, expr);
        else if (token == '!')
            booleanExpr(expr);
        else if (token == CALL)              // method call
            fatal();
        else {
            oprand.accept(this);
            if (!isConstant(expr, token, oprand))
                if (token == '-' || token == '~')
                    if (CodeGen.isP_INT(exprType))
                        exprType = INT;         // type may be BYTE, ...
        }
    }

    private boolean isConstant(Expr expr, int op, ASTree oprand) {
        oprand = stripPlusExpr(oprand);
        if (oprand instanceof IntConst) {
            IntConst c = (IntConst)oprand;
            long v = c.get();
            if (op == '-')
                v = -v;
            else if (op == '~')
                v = ~v;
            else
                return false;

            c.set(v);
        }
        else if (oprand instanceof DoubleConst) {
            DoubleConst c = (DoubleConst)oprand;
            if (op == '-')
                c.set(-c.get());
            else
                return false;
        }
        else
            return false;

        expr.setOperator('+');
        return true;
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
                }
                catch (NoFieldException nfe) {
                    if (nfe.getExpr() != target)
                        throw nfe;

                    // it should be a static method.
                    exprType = CLASS;
                    arrayDim = 0;
                    className = nfe.getField(); // JVM-internal
                    e.setOperator(MEMBER);
                    e.setOprand1(new Symbol(MemberResolver.jvmToJavaName(
                                                            className)));
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
    }

    private static void badMethod() throws CompileError {
        throw new CompileError("bad method");
    }

    /**
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
            = resolver.lookupMethod(targetClass, thisClass, thisMethod,
                                    mname, types, dims, cnames);
        if (found == null) {
            String clazz = targetClass.getName();
            String signature = argTypesToString(types, dims, cnames); 
            String msg;
            if (mname.equals(MethodInfo.nameInit))
                msg = "cannot find constructor " + clazz + signature;
            else
                msg = mname + signature +  " not found in " + clazz;

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
            types[i] = exprType;
            dims[i] = arrayDim;
            cnames[i] = className;
            ++i;
            args = args.tail();
        }
    }

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

    private void atFieldRead(ASTree expr) throws CompileError {
        atFieldRead(fieldAccess(expr));
    }

    private void atFieldRead(CtField f) throws CompileError {
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
    }

    /* if EXPR is to access a static field, fieldAccess() translates EXPR
     * into an expression using '#' (MEMBER).  For example, it translates
     * java.lang.Integer.TYPE into java.lang.Integer#TYPE.  This translation
     * speeds up type resolution by MemberCodeGen.
     */
    protected CtField fieldAccess(ASTree expr) throws CompileError {
        if (expr instanceof Member) {
            Member mem = (Member)expr;
            String name = mem.get();
            try {
                CtField f = thisClass.getField(name);
                if (Modifier.isStatic(f.getModifiers()))
                    mem.setField(f);

                return f;
            }
            catch (NotFoundException e) {
                // EXPR might be part of a static member access?
                throw new NoFieldException(name, expr);
            }
        }
        else if (expr instanceof Expr) {
            Expr e = (Expr)expr;
            int op = e.getOperator();
            if (op == MEMBER) {
                Member mem = (Member)e.oprand2();
                CtField f
                    = resolver.lookupField(((Symbol)e.oprand1()).get(), mem);
                mem.setField(f);
                return f;
            }
            else if (op == '.') {
                try {
                    e.oprand1().accept(this);
                }
                catch (NoFieldException nfe) {
                    if (nfe.getExpr() != e.oprand1())
                        throw nfe;

                    /* EXPR should be a static field.
                     * If EXPR might be part of a qualified class name,
                     * lookupFieldByJvmName2() throws NoFieldException.
                     */
                    return fieldAccess2(e, nfe.getField());
                }

                CompileError err = null;
                try {
                    if (exprType == CLASS && arrayDim == 0)
                        return resolver.lookupFieldByJvmName(className,
                                                    (Symbol)e.oprand2());
                }
                catch (CompileError ce) {
                    err = ce;
                }

                /* If a filed name is the same name as a package's,
                 * a static member of a class in that package is not
                 * visible.  For example,
                 *
                 * class Foo {
                 *   int javassist;
                 * }
                 *
                 * It is impossible to add the following method:
                 *
                 * String m() { return javassist.CtClass.intType.toString(); }
                 *
                 * because javassist is a field name.  However, this is
                 * often inconvenient, this compiler allows it.  The following
                 * code is for that.
                 */
                ASTree oprnd1 = e.oprand1(); 
                if (oprnd1 instanceof Symbol)
                    return fieldAccess2(e, ((Symbol)oprnd1).get());

                if (err != null)
                    throw err;
            }
        }

        throw new CompileError("bad filed access");
    }

    private CtField fieldAccess2(Expr e, String jvmClassName) throws CompileError {
        Member fname = (Member)e.oprand2();
        CtField f = resolver.lookupFieldByJvmName2(jvmClassName, fname, e);
        e.setOperator(MEMBER);
        e.setOprand1(new Symbol(MemberResolver.jvmToJavaName(jvmClassName)));
        fname.setField(f);
        return f;
    }

    public void atClassObject(Expr expr) throws CompileError {
        exprType = CLASS;
        arrayDim = 0;
        className =jvmJavaLangClass;
    }

    public void atArrayLength(Expr expr) throws CompileError {
        expr.oprand1().accept(this);
        exprType = INT;
        arrayDim = 0;
    }

    public void atArrayRead(ASTree array, ASTree index)
        throws CompileError
    {
        array.accept(this);
        int type = exprType;
        int dim = arrayDim;
        String cname = className;
        index.accept(this);
        exprType = type;
        arrayDim = dim - 1;
        className = cname;
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
        }
        else {
            if (oprand instanceof Expr) {
                Expr e = (Expr)oprand;
                if (e.getOperator() == ARRAY) {
                    atArrayRead(e.oprand1(), e.oprand2());
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
        CtField f = fieldAccess(oprand);
        atFieldRead(f);
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
