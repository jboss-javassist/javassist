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

import java.util.List;
import javassist.*;
import javassist.bytecode.*;
import javassist.compiler.ast.*;

/* Code generator methods depending on javassist.* classes.
 */
public class MemberCodeGen extends CodeGen {
    protected ClassPool classPool;
    protected CtClass   thisClass;
    protected MethodInfo thisMethod;

    protected boolean resultStatic;

    public MemberCodeGen(Bytecode b, CtClass cc, ClassPool cp) {
        super(b);
        classPool = cp;
        thisClass = cc;
        thisMethod = null;
    }

    /**
     * Records the currently compiled method.
     */
    public void setThisMethod(CtMethod m) {
        thisMethod = m.getMethodInfo2();
    }

    public CtClass getThisClass() { return thisClass; }

    /**
     * Returns the JVM-internal representation of this class name.
     */
    protected String getThisName() {
        return javaToJvmName(thisClass.getName());
    }

    /**
     * Returns the JVM-internal representation of this super class name.
     */
    protected String getSuperName() throws CompileError {
        return javaToJvmName(getSuperclass(thisClass).getName());
    }

    protected void insertDefaultSuperCall() throws CompileError {
        bytecode.addAload(0);
        bytecode.addInvokespecial(getSuperclass(thisClass), "<init>", "()V");
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

            CtClass type = lookupClass(decl.getClassName());
            decl.setClassName(javaToJvmName(type.getName()));
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
            CtClass clazz = lookupClass(expr.getClassName());
            String cname = clazz.getName();
            ASTList args = expr.getArguments();
            bytecode.addNew(cname);
            bytecode.addOpcode(DUP);

            atMethodCall2(clazz, MethodInfo.nameInit, args, false, true);

            exprType = CLASS;
            arrayDim = 0;
            className = javaToJvmName(cname);
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
            bytecode.addAnewarray(jvmToJavaName(className));
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

    protected void atMethodCall(Expr expr) throws CompileError {
        String mname = null;
        CtClass targetClass = null;
        ASTree method = expr.oprand1();
        ASTList args = (ASTList)expr.oprand2();
        boolean isStatic = false;
        boolean isSpecial = false;

        if (method instanceof Member) {
            mname = ((Member)method).get();
            targetClass = thisClass;
            if (inStaticMethod)
                isStatic = true;            // should be static
            else
                bytecode.addAload(0);       // this
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
                targetClass = getSuperclass(targetClass);
        }
        else if (method instanceof Expr) {
            Expr e = (Expr)method;
            mname = ((Symbol)e.oprand2()).get();
            int op = e.getOperator();
            if (op == MEMBER) {                 // static method
                targetClass = lookupClass((ASTList)e.oprand1());
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
                    isStatic = true;
                }

                if (arrayDim > 0)
                    targetClass = lookupClass2(javaLangObject);
                else if (exprType == CLASS /* && arrayDim == 0 */)
                    targetClass = lookupClass(className);
                else
                    badMethod();
            }
            else
                badMethod();
        }
        else
            fatal();

        atMethodCall2(targetClass, mname, args, isStatic, isSpecial);
    }

    private static void badMethod() throws CompileError {
        throw new CompileError("bad method");
    }

    private static CtClass getSuperclass(CtClass c) throws CompileError {
        try {
            return c.getSuperclass();
        }
        catch (NotFoundException e) {
            throw new CompileError("cannot find the super class of "
                                   + c.getName());
        }
    }

    public void atMethodCall2(CtClass targetClass, String mname,
                        ASTList args, boolean isStatic, boolean isSpecial)
        throws CompileError
    {
        int nargs = atMethodArgsLength(args);
        int[] types = new int[nargs];
        int[] dims = new int[nargs];
        String[] cnames = new String[nargs];

        int stack = bytecode.getStackDepth();

        atMethodArgs(args, types, dims, cnames);

        // used by invokeinterface
        int count = bytecode.getStackDepth() - stack + 1;

        Object[] found = lookupMethod(targetClass, thisMethod, mname,
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

        CtClass declClass = (CtClass)found[0];
        MethodInfo minfo = (MethodInfo)found[1];
        String desc = minfo.getDescriptor();
        int acc = minfo.getAccessFlags();

        if (mname.equals(MethodInfo.nameInit)) {
            isSpecial = true;
            if (declClass != targetClass)
                throw new CompileError("no such a constructor");
        }
        else if ((acc & AccessFlag.PRIVATE) != 0) {
            isSpecial = true;
            if (declClass != targetClass)
                throw new CompileError("Method " + mname + "is private");
        }

        boolean popTarget = false;
        if ((acc & AccessFlag.STATIC) != 0) {
            if (!isStatic) {
                /* this method is static but the target object is
                   on stack.  It must be popped out.
                */
                isStatic = true;
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

    public int atMethodArgsLength(ASTList args) {
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

    private void setReturnType(String desc, boolean isStatic,
                               boolean popTarget)
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
            exprType = descToType(c);
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

    private Object[] lookupMethod(CtClass clazz, MethodInfo current,
                                  String methodName,
                                  int[] argTypes, int[] argDims,
                                  String[] argClassNames, boolean onlyExact)
        throws CompileError
    {
        Object[] maybe = null;

        if (current != null)
            if (current.getName().equals(methodName)) {
                int res = compareSignature(current.getDescriptor(),
                                           argTypes, argDims, argClassNames);
                Object[] r = new Object[] { clazz, current };
                if (res == YES)
                    return r;
                else if (res == MAYBE && maybe == null)
                    maybe = r;
            }

        List list = clazz.getClassFile2().getMethods();
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            if (minfo.getName().equals(methodName)) {
                int res = compareSignature(minfo.getDescriptor(),
                                           argTypes, argDims, argClassNames);
                Object[] r = new Object[] { clazz, minfo };
                if (res == YES)
                    return r;
                else if (res == MAYBE && maybe == null)
                    maybe = r;
            }
        }

        try {
            CtClass pclazz = clazz.getSuperclass();
            if (pclazz != null) {
                Object[] r = lookupMethod(pclazz, null, methodName, argTypes,
                                          argDims, argClassNames,
                                          (onlyExact || maybe != null));
                if (r != null)
                    return r;
            }
        }
        catch (NotFoundException e) {}

        /* -- not necessary to search implemented interfaces.
        try {
            CtClass[] ifs = clazz.getInterfaces();
            int size = ifs.length;
            for (int i = 0; i < size; ++i) {
                Object[] r = lookupMethod(ifs[i], methodName, argTypes,
                                          argDims, argClassNames);
                if (r != null)
                    return r;
            }
        }
        catch (NotFoundException e) {}
        */

        if (onlyExact)
            return null;
        else
            return maybe;
    }

    private static final int YES = 2;
    private static final int MAYBE = 1;
    private static final int NO = 0;

    /*
     * Returns YES if actual parameter types matches the given signature.
     *
     * argTypes, argDims, and argClassNames represent actual parameters.
     *
     * This method does not correctly implement the Java method dispatch
     * algorithm.
     */
    private int compareSignature(String desc, int[] argTypes,
                                 int[] argDims, String[] argClassNames)
        throws CompileError
    {
        int result = YES;
        int i = 1;
        int nArgs = argTypes.length;
        if (nArgs != Descriptor.numOfParameters(desc))
            return NO;

        int len = desc.length();
        for (int n = 0; i < len; ++n) {
            char c = desc.charAt(i++);
            if (c == ')')
                return (n == nArgs ? result : NO);
            else if (n >= nArgs)
                return NO;

            int dim = 0;
            while (c == '[') {
                ++dim;
                c = desc.charAt(i++);
            }

            if (argTypes[n] == NULL) {
                if (dim == 0 && c != 'L')
                    return NO;
            }
            else if (argDims[n] != dim) {
                if (!(dim == 0 && c == 'L'
                      && desc.startsWith("java/lang/Object;", i)))
                    return NO;

                // if the thread reaches here, c must be 'L'.
                i = desc.indexOf(';', i) + 1;
                result = MAYBE;
                if (i <= 0)
                    return NO;  // invalid descriptor?
            }
            else if (c == 'L') {        // not compare
                int j = desc.indexOf(';', i);
                if (j < 0 || argTypes[n] != CLASS)
                    return NO;

                String cname = desc.substring(i, j);
                if (!cname.equals(argClassNames[n])) {
                    CtClass clazz = lookupClass(argClassNames[n]);
                    try {
                        if (clazz.subtypeOf(lookupClass(cname)))
                            result = MAYBE;
                        else
                            return NO;
                    }
                    catch (NotFoundException e) {
                        result = MAYBE; // should be NO?
                    }
                }

                i = j + 1;
            }
            else {
                int t = descToType(c);
                int at = argTypes[n];
                if (t != at)
                    if (t == INT
                        && (at == SHORT || at == BYTE || at == CHAR))
                        result = MAYBE;
                    else
                        return NO;
            }
        }

        return NO;
    }

    protected static int descToType(char c) throws CompileError {
        switch (c) {
        case 'Z' :
            return BOOLEAN;
        case 'C' :
            return CHAR;
        case 'B' :
            return  BYTE;
        case 'S' :
            return SHORT;
        case 'I' :
            return INT;
        case 'J' :
            return LONG;
        case 'F' :
            return FLOAT;
        case 'D' :
            return DOUBLE;
        case 'V' :
            return VOID;
        case 'L' :
        case '[' :
            return CLASS;
        default :
            fatal();
            return VOID;
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
        exprType = descToType(c);

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
        CtField f = null;
        boolean is_static = false;
        if (expr instanceof Member) {
            String name = ((Member)expr).get();
            try {
                f = thisClass.getField(name);
            }
            catch (NotFoundException e) {
                // EXPR might be part of a static member access?
                throw new NoFieldException(name, expr);
            }

            is_static = Modifier.isStatic(f.getModifiers());
            if (!is_static)
                if (inStaticMethod)
                    throw new CompileError(
                                "not available in a static method: " + name);
                else
                    bytecode.addAload(0);       // this
        }
        else if (expr instanceof Expr) {
            Expr e = (Expr)expr;
            int op = e.getOperator();
            if (op == MEMBER) {
                f = lookupField((ASTList)e.oprand1(), (Symbol)e.oprand2());
                is_static = true;
            }
            else if (op == '.') {
                try {
                    e.oprand1().accept(this);
                    if (exprType == CLASS && arrayDim == 0)
                        f = lookupField(className, (Symbol)e.oprand2());
                    else
                        badLvalue();

                    is_static = Modifier.isStatic(f.getModifiers());
                    if (is_static)
                        bytecode.addOpcode(POP);
                }
                catch (NoFieldException nfe) {
                    if (nfe.getExpr() != e.oprand1())
                        throw nfe;

                    Symbol fname = (Symbol)e.oprand2();
                    // it should be a static field.
                    try {
                        f = lookupField(nfe.getField(), fname);
                        is_static = true;
                    }
                    catch (CompileError ce) {
                        // EXPR might be part of a qualified class name.
                        throw new NoFieldException(nfe.getField() + "/"
                                                   + fname.get(), expr);
                    }
                }
            }
            else
                badLvalue();
        }
        else
            badLvalue();

        resultStatic = is_static;
        return f;
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
                params[i++] = lookupClass((Declarator)plist.head());
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
                clist[i++] = lookupClass((ASTList)list.head());
                list = list.tail();
            }

            return clist;
        }
    }

    public static int getModifiers(ASTList mods) {
        int m = 0;
        while (mods != null) {
            Keyword k = (Keyword)mods.head();
            mods = mods.tail();
            switch (k.get()) {
            case STATIC :
                m |= Modifier.STATIC;
                break;
            case FINAL :
                m |= Modifier.FINAL;
                break;
            case SYNCHRONIZED :
                m |= Modifier.SYNCHRONIZED;
                break;
            case ABSTRACT :
                m |= Modifier.ABSTRACT;
                break;
            case PUBLIC :
                m |= Modifier.PUBLIC;
                break;
            case PROTECTED :
                m |= Modifier.PROTECTED;
                break;
            case PRIVATE :
                m |= Modifier.PRIVATE;
                break;
            case VOLATILE :
                m |= Modifier.VOLATILE;
                break;
            case TRANSIENT :
                m |= Modifier.TRANSIENT;
                break;
            case STRICT :
                m |= Modifier.STRICT;
                break;
            }
        }

        return m;
    }

    /* Converts a class name into a JVM-internal representation.
     *
     * It may also expand a simple class name to java.lang.*.
     * For example, this converts Object into java/lang/Object.
     */
    protected String resolveClassName(ASTList name) throws CompileError {
        if (name == null)
            return null;
        else
            return javaToJvmName(lookupClass(name).getName());
    }

    /* Expands a simple class name to java.lang.*.
     * For example, this converts Object into java/lang/Object.
     */
    protected String resolveClassName(String jvmName) throws CompileError {
        if (jvmName == null)
            return null;
        else
            return javaToJvmName(lookupClass(jvmName).getName());
    }

    protected CtClass lookupClass(Declarator decl) throws CompileError {
        return lookupClass(decl.getType(), decl.getArrayDim(),
                           decl.getClassName());
    }

    protected CtClass lookupClass(int type, int dim, String classname)
        throws CompileError
    {
        String cname = "";
        CtClass clazz;
        switch (type) {
        case CLASS :
            clazz = lookupClass(classname);
            if (dim > 0)
                cname = clazz.getName();
            else
                return clazz;

            break;
        case BOOLEAN :
            cname = "boolean";
            break;
        case CHAR :
            cname = "char";
            break;
        case BYTE :
            cname = "byte";
            break;
        case SHORT :
            cname = "short";
            break;
        case INT :
            cname = "int";
            break;
        case LONG :
            cname = "long";
            break;
        case FLOAT :
            cname = "float";
            break;
        case DOUBLE :
            cname = "double";
            break;
        case VOID :
            cname = "void";
            break;
        default :
            fatal();
        }

        while (dim-- > 0)
            cname += "[]";

        return lookupClass2(cname);
    }

    protected CtClass lookupClass(ASTList name) throws CompileError {
        return lookupClass2(Declarator.astToClassName(name, '.'));
    }

    protected CtClass lookupClass(String jvmName) throws CompileError {
        return lookupClass2(jvmToJavaName(jvmName));
    }

    /**
     * @param name      a qualified class name. e.g. java.lang.String
     */
    private CtClass lookupClass2(String name) throws CompileError {
        try {
            return classPool.get(name);
        }
        catch (NotFoundException e) {}

        try {
            if (name.indexOf('.') < 0)
                return classPool.get("java.lang." + name);
        }
        catch (NotFoundException e) {}

        throw new CompileError("no such class: " + name);
    }

    public CtField lookupField(ASTList className, Symbol fieldName)
        throws CompileError
    {
        return lookupField2(Declarator.astToClassName(className, '.'),
                            fieldName);
    }

    public CtField lookupField(String className, Symbol fieldName)
        throws CompileError
    {
        return lookupField2(jvmToJavaName(className), fieldName);
    }

    /**
     * @param name      a qualified class name. e.g. java.lang.String
     */
    private CtField lookupField2(String className, Symbol fieldName)
        throws CompileError
    {
        CtClass cc = lookupClass(className);
        try {
            return cc.getField(fieldName.get());
        }
        catch (NotFoundException e) {}
        throw new CompileError("no such field: " + fieldName.get());
    }

    protected static String javaToJvmName(String classname) {
        return classname.replace('.', '/');
    }

    protected static String jvmToJavaName(String classname) {
        return classname.replace('/', '.');
    }
}
