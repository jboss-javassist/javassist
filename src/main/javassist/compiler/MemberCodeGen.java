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

import javassist.*;
import javassist.bytecode.*;
import javassist.compiler.ast.*;

/* Code generator methods depending on javassist.* classes.
 */
public class MemberCodeGen extends CodeGen {
    protected MemberResolver resolver;
    protected CtClass   thisClass;
    protected MethodInfo thisMethod;

    protected boolean resultStatic;

    public MemberCodeGen(Bytecode b, CtClass cc, ClassPool cp)
    {
        super(b);
        resolver = new MemberResolver(cp);
        thisClass = cc;
        thisMethod = null;
    }

    /**
     * Records the currently compiled method.
     */
    public void setThisMethod(CtMethod m) {
        thisMethod = m.getMethodInfo2();
        if (typeChecker != null)
            typeChecker.setThisMethod(thisMethod);
    }

    public CtClass getThisClass() { return thisClass; }

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

    protected void insertDefaultSuperCall() throws CompileError {
        bytecode.addAload(0);
        bytecode.addInvokespecial(MemberResolver.getSuperclass(thisClass),
                                  "<init>", "()V");
    }

    protected void atTryStmnt(Stmnt st) throws CompileError {
        Stmnt body = (Stmnt)st.getLeft();
        if (body == null)
            return;

        int start = bytecode.currentPc();
        body.accept(this);
        int end = bytecode.currentPc();
        if (start == end)
            throw new CompileError("empty try block");

        bytecode.addOpcode(Opcode.GOTO);
        int pc = bytecode.currentPc();
        bytecode.addIndex(0);   // correct later

        int var = getMaxLocals();
        incMaxLocals(1);
        ASTList catchList = (ASTList)st.getRight().getLeft();
        while (catchList != null) {
            Pair p = (Pair)catchList.head();
            catchList = catchList.tail();
            Declarator decl = (Declarator)p.getLeft();
            Stmnt block = (Stmnt)p.getRight();

            decl.setLocalVar(var);

            CtClass type = resolver.lookupClassByJvmName(decl.getClassName());
            decl.setClassName(MemberResolver.javaToJvmName(type.getName()));
            bytecode.addExceptionHandler(start, end, bytecode.currentPc(),
                                         type);
            bytecode.growStack(1);
            bytecode.addAstore(var);
            if (block != null)
                block.accept(this);

            bytecode.addOpcode(Opcode.GOTO);
            bytecode.addIndex(pc - bytecode.currentPc());
        }

        Stmnt finallyBlock = (Stmnt)st.getRight().getRight().getLeft();
        if (finallyBlock != null)
            throw new CompileError(
                        "sorry, finally has not been supported yet");

        bytecode.write16bit(pc, bytecode.currentPc() - pc + 1);
        hasReturned = false;
    }

    public void atNewExpr(NewExpr expr) throws CompileError {
        if (expr.isArray())
            atNewArrayExpr(expr);
        else {
            CtClass clazz = resolver.lookupClassByName(expr.getClassName());
            String cname = clazz.getName();
            ASTList args = expr.getArguments();
            bytecode.addNew(cname);
            bytecode.addOpcode(DUP);

            atMethodCallCore(clazz, MethodInfo.nameInit, args,
                             false, true, -1, null);

            exprType = CLASS;
            arrayDim = 0;
            className = MemberResolver.javaToJvmName(cname);
        }
    }

    public void atNewArrayExpr(NewExpr expr) throws CompileError {
        if (expr.getInitializer() != null)
            throw new CompileError("array initializer is not supported");

        int type = expr.getArrayType();
        ASTList size = expr.getArraySize();
        ASTList classname = expr.getClassName();
        if (size.length() > 1) {
            atMultiNewArray(type, classname, size);
            return;
        }

        size.head().accept(this);
        exprType = type;
        arrayDim = 1;
        if (type == CLASS) {
            className = resolveClassName(classname);
            bytecode.addAnewarray(MemberResolver.jvmToJavaName(className));
        }
        else {
            className = null;
            int atype = 0;
            switch (type) {
            case BOOLEAN :
                atype = T_BOOLEAN;
                break;
            case CHAR :
                atype = T_CHAR;
                break;
            case FLOAT :
                atype = T_FLOAT;
                break;
            case DOUBLE :
                atype = T_DOUBLE;
                break;
            case BYTE :
                atype = T_BYTE;
                break;
            case SHORT :
                atype = T_SHORT;
                break;
            case INT :
                atype = T_INT;
                break;
            case LONG :
                atype = T_LONG;
                break;
            default :
                badNewExpr();
                break;
            }

            bytecode.addOpcode(NEWARRAY);
            bytecode.add(atype);
        }
    }

    private static void badNewExpr() throws CompileError {
        throw new CompileError("bad new expression");
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
            if (exprType != INT)
                throw new CompileError("bad type for array size");
        }

        String desc;
        exprType = type;
        arrayDim = dim;
        if (type == CLASS) {
            className = resolveClassName(classname);
            desc = toJvmArrayName(className, dim);
        }
        else
            desc = toJvmTypeName(type, dim);

        bytecode.addMultiNewarray(desc, count);
    }

    public void atCallExpr(CallExpr expr) throws CompileError {
        String mname = null;
        CtClass targetClass = null;
        ASTree method = expr.oprand1();
        ASTList args = (ASTList)expr.oprand2();
        boolean isStatic = false;
        boolean isSpecial = false;
        int aload0pos = -1;

        MemberResolver.Method cached = expr.getMethod();
        if (method instanceof Member) {
            mname = ((Member)method).get();
            targetClass = thisClass;
            if (inStaticMethod || (cached != null && cached.isStatic()))
                isStatic = true;            // should be static
            else {
                aload0pos = bytecode.currentPc();
                bytecode.addAload(0);       // this
            }
        }
        else if (method instanceof Keyword) {   // constructor
            isSpecial = true;
            mname = MethodInfo.nameInit;        // <init>
            targetClass = thisClass;
            if (inStaticMethod)
                throw new CompileError("a constructor cannot be static");
            else
                bytecode.addAload(0);   // this

            if (((Keyword)method).get() == SUPER)
                targetClass = MemberResolver.getSuperclass(targetClass);
        }
        else if (method instanceof Expr) {
            Expr e = (Expr)method;
            mname = ((Symbol)e.oprand2()).get();
            int op = e.getOperator();
            if (op == MEMBER) {                 // static method
                targetClass
                    = resolver.lookupClass(((Symbol)e.oprand1()).get(), false);
                isStatic = true;
            }
            else if (op == '.') {
                ASTree target = e.oprand1();
                if (target instanceof Keyword)
                    if (((Keyword)target).get() == SUPER)
                        isSpecial = true;

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
                    resolver.recordPackage(className);
                    isStatic = true;
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

        atMethodCallCore(targetClass, mname, args, isStatic, isSpecial,
                         aload0pos, cached);
    }

    private static void badMethod() throws CompileError {
        throw new CompileError("bad method");
    }

    /*
     * atMethodCallCore() is also called by doit() in NewExpr.ProceedForNew
     *
     * @param targetClass       the class at which method lookup starts.
     * @param found         not null if the method look has been already done.
     */
    public void atMethodCallCore(CtClass targetClass, String mname,
                        ASTList args, boolean isStatic, boolean isSpecial,
                        int aload0pos, MemberResolver.Method found)
        throws CompileError
    {
        int nargs = getMethodArgsLength(args);
        int[] types = new int[nargs];
        int[] dims = new int[nargs];
        String[] cnames = new String[nargs];

        if (!isStatic && found != null && found.isStatic()) {
            bytecode.addOpcode(POP);
            isStatic = true;
        }

        int stack = bytecode.getStackDepth();

        atMethodArgs(args, types, dims, cnames);

        // used by invokeinterface
        int count = bytecode.getStackDepth() - stack + 1;

        if (found == null)
            found = resolver.lookupMethod(targetClass, thisMethod, mname,
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

        CtClass declClass = found.declaring;
        MethodInfo minfo = found.info;
        String desc = minfo.getDescriptor();
        int acc = minfo.getAccessFlags();

        if (mname.equals(MethodInfo.nameInit)) {
            isSpecial = true;
            if (declClass != targetClass)
                throw new CompileError("no such a constructor");
        }
        else if ((acc & AccessFlag.PRIVATE) != 0) {
            isSpecial = true;
            String orgName = mname;
            mname = getAccessiblePrivate(mname, declClass);
            if (mname == null)
                throw new CompileError("Method " + orgName + " is private");
        }

        boolean popTarget = false;
        if ((acc & AccessFlag.STATIC) != 0) {
            if (!isStatic) {
                /* this method is static but the target object is
                   on stack.  It must be popped out.  If aload0pos >= 0,
                   then the target object was pushed by aload_0.  It is
                   overwritten by NOP.
                */
                isStatic = true;
                if (aload0pos >= 0)
                    bytecode.write(aload0pos, NOP);
                else
                    popTarget = true;
            }

            bytecode.addInvokestatic(declClass, mname, desc);
        }
        else if (isSpecial)
            bytecode.addInvokespecial(declClass, mname, desc);
        else if (declClass.isInterface())
            bytecode.addInvokeinterface(declClass, mname, desc, count);
        else
            if (isStatic)
                throw new CompileError(mname + " is not static");
            else
                bytecode.addInvokevirtual(declClass, mname, desc);

        setReturnType(desc, isStatic, popTarget);
    }

    protected String getAccessiblePrivate(String methodName,
                                          CtClass declClass) {
        if (declClass == thisClass)
            return methodName;
        else if (isEnclosing(declClass, thisClass))
            return null;
        else
            return null;    // cannot access this private method.
    }

    private boolean isEnclosing(CtClass outer, CtClass inner) {
        try {
            while (inner != null) {
                inner = inner.getDeclaringClass();
                if (inner == outer)
                    return true;
            }
        }
        catch (NotFoundException e) {}
        return false;   
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

    void setReturnType(String desc, boolean isStatic, boolean popTarget)
        throws CompileError
    {
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

        int etype = exprType;
        if (isStatic) {
            if (popTarget) {
                if (is2word(etype, dim)) {
                    bytecode.addOpcode(DUP2_X1);
                    bytecode.addOpcode(POP2);
                    bytecode.addOpcode(POP);
                }
                else if (etype == VOID)
                    bytecode.addOpcode(POP);
                else {
                    bytecode.addOpcode(SWAP);
                    bytecode.addOpcode(POP);
                }
            }
        }
    }

    protected void atFieldAssign(Expr expr, int op, ASTree left,
                        ASTree right, boolean doDup) throws CompileError
    {
        CtField f = fieldAccess(left);
        boolean is_static = resultStatic;
        if (op != '=' && !is_static)
            bytecode.addOpcode(DUP);

        int fi = atFieldRead(f, is_static, op == '=');
        int fType = exprType;
        int fDim = arrayDim;
        String cname = className;

        atAssignCore(expr, op, right, fType, fDim, cname);

        boolean is2w = is2word(fType, fDim);
        if (doDup) {
            int dup_code;
            if (is_static)
                dup_code = (is2w ? DUP2 : DUP);
            else
                dup_code = (is2w ? DUP2_X1 : DUP_X1);

            bytecode.addOpcode(dup_code);
        }

        if (is_static) {
            bytecode.add(PUTSTATIC);
            bytecode.growStack(is2w ? -2 : -1);
        }
        else {
            bytecode.add(PUTFIELD);
            bytecode.growStack(is2w ? -3 : -2);
        }

        bytecode.addIndex(fi);
        exprType = fType;
        arrayDim = fDim;
        className = cname;
    }

    /* overwritten in JvstCodeGen.
     */
    public void atMember(Member mem) throws CompileError {
        atFieldRead(mem);
    }

    protected void atFieldRead(ASTree expr) throws CompileError
    {
        CtField f = fieldAccess(expr);
        boolean is_static = resultStatic;
        atFieldRead(f, is_static, false);
    }

    private int atFieldRead(CtField f, boolean isStatic, boolean noRead)
        throws CompileError
    {
        FieldInfo finfo = f.getFieldInfo2();
        String type = finfo.getDescriptor();

        int fi = addFieldrefInfo(f, finfo, type);

        int i = 0;
        int dim = 0;
        char c = type.charAt(i);
        while (c == '[') {
            ++dim;
            c = type.charAt(++i);
        }

        arrayDim = dim;
        boolean is2byte = (c == 'J' || c == 'D');
        exprType = MemberResolver.descToType(c);

        if (c == 'L')
            className = type.substring(i + 1, type.indexOf(';', i + 1));
        else
            className = null;

        if (noRead)
            return fi;

        if (isStatic) {
            bytecode.add(GETSTATIC);
            bytecode.growStack(is2byte ? 2 : 1);
        }
        else {
            bytecode.add(GETFIELD);
            bytecode.growStack(is2byte ? 1 : 0);
        }

        bytecode.addIndex(fi);
        return fi;
    }

    protected int addFieldrefInfo(CtField f, FieldInfo finfo, String type) {
        ConstPool cp = bytecode.getConstPool();
        String cname = f.getDeclaringClass().getName();
        int ci = cp.addClassInfo(cname);
        String name = finfo.getName();
        return cp.addFieldrefInfo(ci, name, type);
    }

    protected void atFieldPlusPlus(int token, boolean isPost,
                                   ASTree oprand, Expr expr, boolean doDup)
        throws CompileError
    {
        CtField f = fieldAccess(oprand);
        boolean is_static = resultStatic;
        if (!is_static)
            bytecode.addOpcode(DUP);

        int fi = atFieldRead(f, is_static, false);
        int t = exprType;
        boolean is2w = is2word(t, arrayDim);

        int dup_code;
        if (is_static)
            dup_code = (is2w ? DUP2 : DUP);
        else
            dup_code = (is2w ? DUP2_X1 : DUP_X1);

        atPlusPlusCore(dup_code, doDup, token, isPost, expr);

        if (is_static) {
            bytecode.add(PUTSTATIC);
            bytecode.growStack(is2w ? -2 : -1);
        }
        else {
            bytecode.add(PUTFIELD);
            bytecode.growStack(is2w ? -3 : -2);
        }

        bytecode.addIndex(fi);
    }

    /* This method also returns a value in resultStatic.
     */
    protected CtField fieldAccess(ASTree expr) throws CompileError {
        if (expr instanceof Member) {
            String name = ((Member)expr).get();
            CtField f = null;
            try {
                f = thisClass.getField(name);
            }
            catch (NotFoundException e) {
                // EXPR might be part of a static member access?
                throw new NoFieldException(name, expr);
            }

            boolean is_static = Modifier.isStatic(f.getModifiers());
            if (!is_static)
                if (inStaticMethod)
                    throw new CompileError(
                                "not available in a static method: " + name);
                else
                    bytecode.addAload(0);       // this

            resultStatic = is_static;
            return f;
        }
        else if (expr instanceof Expr) {
            Expr e = (Expr)expr;
            int op = e.getOperator();
            if (op == MEMBER) {
                // static member by # (extension by Javassist)
                CtField f = resolver.lookupField(((Symbol)e.oprand1()).get(),
                                         (Symbol)e.oprand2());
                resultStatic = true;
                return f;
            }
            else if (op == '.') {
                CtField f = null;
                try {
                    e.oprand1().accept(this);
                    /* Don't call lookupFieldByJvmName2().
                     * The left operand of . is not a class name but
                     * a normal expression.
                     */
                    if (exprType == CLASS && arrayDim == 0)
                        f = resolver.lookupFieldByJvmName(className,
                                                    (Symbol)e.oprand2());
                    else
                        badLvalue();

                    boolean is_static = Modifier.isStatic(f.getModifiers());
                    if (is_static)
                        bytecode.addOpcode(POP);

                    resultStatic = is_static;
                    return f;
                }
                catch (NoFieldException nfe) {
                    if (nfe.getExpr() != e.oprand1())
                        throw nfe;

                    /* EXPR should be a static field.
                     * If EXPR might be part of a qualified class name,
                     * lookupFieldByJvmName2() throws NoFieldException.
                     */
                    Symbol fname = (Symbol)e.oprand2();
                    String cname = nfe.getField();
                    f = resolver.lookupFieldByJvmName2(cname, fname, expr);
                    resolver.recordPackage(cname);
                    resultStatic = true;
                    return f;
                }
            }
            else
                badLvalue();
        }
        else
            badLvalue();

        resultStatic = false;
        return null;    // never reach
    }

    private static void badLvalue() throws CompileError {
        throw new CompileError("bad l-value");
    }

    public CtClass[] makeParamList(MethodDecl md) throws CompileError {
        CtClass[] params;
        ASTList plist = md.getParams();
        if (plist == null)
            params = new CtClass[0];
        else {
            int i = 0;
            params = new CtClass[plist.length()];
            while (plist != null) {
                params[i++] = resolver.lookupClass((Declarator)plist.head());
                plist = plist.tail();
            }
        }

        return params;
    }

    public CtClass[] makeThrowsList(MethodDecl md) throws CompileError {
        CtClass[] clist;
        ASTList list = md.getThrows();
        if (list == null)
            return null;
        else {
            int i = 0;
            clist = new CtClass[list.length()];
            while (list != null) {
                clist[i++] = resolver.lookupClassByName((ASTList)list.head());
                list = list.tail();
            }

            return clist;
        }
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
}
