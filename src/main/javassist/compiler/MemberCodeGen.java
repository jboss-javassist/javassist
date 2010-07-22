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

import javassist.*;
import javassist.bytecode.*;
import javassist.compiler.ast.*;

import java.util.ArrayList;

/* Code generator methods depending on javassist.* classes.
 */
public class MemberCodeGen extends CodeGen {
    protected MemberResolver resolver;
    protected CtClass   thisClass;
    protected MethodInfo thisMethod;

    protected boolean resultStatic;

    public MemberCodeGen(Bytecode b, CtClass cc, ClassPool cp) {
        super(b);
        resolver = new MemberResolver(cp);
        thisClass = cc;
        thisMethod = null;
    }

    /**
     * Returns the major version of the class file
     * targeted by this compilation.
     */
    public int getMajorVersion() {
        ClassFile cf = thisClass.getClassFile2();
        if (cf == null)
            return ClassFile.MAJOR_VERSION;     // JDK 1.3
        else
            return cf.getMajorVersion();
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

    static class JsrHook extends ReturnHook {
        ArrayList jsrList;
        CodeGen cgen;
        int var;

        JsrHook(CodeGen gen) {
            super(gen);
            jsrList = new ArrayList();
            cgen = gen;
            var = -1;
        }

        private int getVar(int size) {
            if (var < 0) {
                var = cgen.getMaxLocals();
                cgen.incMaxLocals(size);
            }

            return var;
        }

        private void jsrJmp(Bytecode b) {
            b.addOpcode(Opcode.GOTO);
            jsrList.add(new int[] {b.currentPc(), var});
            b.addIndex(0);
        }

        protected boolean doit(Bytecode b, int opcode) {
            switch (opcode) {
            case Opcode.RETURN :
                jsrJmp(b);
                break;
            case ARETURN :
                b.addAstore(getVar(1));
                jsrJmp(b);
                b.addAload(var);
                break;
            case IRETURN :
                b.addIstore(getVar(1));
                jsrJmp(b);
                b.addIload(var);
                break;
            case LRETURN :
                b.addLstore(getVar(2));
                jsrJmp(b);
                b.addLload(var);
                break;
            case DRETURN :
                b.addDstore(getVar(2));
                jsrJmp(b);
                b.addDload(var);
                break;
            case FRETURN :
                b.addFstore(getVar(1));
                jsrJmp(b);
                b.addFload(var);
                break;
            default :
                throw new RuntimeException("fatal");
            }

            return false;
        }
    }

    static class JsrHook2 extends ReturnHook {
        int var;
        int target;

        JsrHook2(CodeGen gen, int[] retTarget) {
            super(gen);
            target = retTarget[0];
            var = retTarget[1];
        }

        protected boolean doit(Bytecode b, int opcode) {
            switch (opcode) {
            case Opcode.RETURN :
                break;
            case ARETURN :
                b.addAstore(var);
                break;
            case IRETURN :
                b.addIstore(var);
                break;
            case LRETURN :
                b.addLstore(var);
                break;
            case DRETURN :
                b.addDstore(var);
                break;
            case FRETURN :
                b.addFstore(var);
                break;
            default :
                throw new RuntimeException("fatal");
            }

            b.addOpcode(Opcode.GOTO);
            b.addIndex(target - b.currentPc() + 3);
            return true;
        }
    }

    protected void atTryStmnt(Stmnt st) throws CompileError {
        Bytecode bc = bytecode;
        Stmnt body = (Stmnt)st.getLeft();
        if (body == null)
            return;

        ASTList catchList = (ASTList)st.getRight().getLeft();
        Stmnt finallyBlock = (Stmnt)st.getRight().getRight().getLeft();
        ArrayList gotoList = new ArrayList(); 

        JsrHook jsrHook = null;
        if (finallyBlock != null)
            jsrHook = new JsrHook(this);

        int start = bc.currentPc();
        body.accept(this);
        int end = bc.currentPc();
        if (start == end)
            throw new CompileError("empty try block");

        boolean tryNotReturn = !hasReturned;
        if (tryNotReturn) {
            bc.addOpcode(Opcode.GOTO);
            gotoList.add(new Integer(bc.currentPc()));
            bc.addIndex(0);   // correct later
        }

        int var = getMaxLocals();
        incMaxLocals(1);
        while (catchList != null) {
            // catch clause
            Pair p = (Pair)catchList.head();
            catchList = catchList.tail();
            Declarator decl = (Declarator)p.getLeft();
            Stmnt block = (Stmnt)p.getRight();

            decl.setLocalVar(var);

            CtClass type = resolver.lookupClassByJvmName(decl.getClassName());
            decl.setClassName(MemberResolver.javaToJvmName(type.getName()));
            bc.addExceptionHandler(start, end, bc.currentPc(), type);
            bc.growStack(1);
            bc.addAstore(var);
            hasReturned = false;
            if (block != null)
                block.accept(this);

            if (!hasReturned) {
                bc.addOpcode(Opcode.GOTO);
                gotoList.add(new Integer(bc.currentPc()));
                bc.addIndex(0);   // correct later
                tryNotReturn = true;
            }
        }

        if (finallyBlock != null) {
            jsrHook.remove(this);
            // catch (any) clause
            int pcAnyCatch = bc.currentPc();
            bc.addExceptionHandler(start, pcAnyCatch, pcAnyCatch, 0);
            bc.growStack(1);
            bc.addAstore(var);
            hasReturned = false;
            finallyBlock.accept(this);
            if (!hasReturned) {
                bc.addAload(var);
                bc.addOpcode(ATHROW);
            }

            addFinally(jsrHook.jsrList, finallyBlock);
        }

        int pcEnd = bc.currentPc();
        patchGoto(gotoList, pcEnd);
        hasReturned = !tryNotReturn;
        if (finallyBlock != null) {
            if (tryNotReturn)
                finallyBlock.accept(this);
        }
    }

    /**
     * Adds a finally clause for earch return statement.
     */
    private void addFinally(ArrayList returnList, Stmnt finallyBlock)
        throws CompileError
    {
        Bytecode bc = bytecode;
        int n = returnList.size();
        for (int i = 0; i < n; ++i) {
            final int[] ret = (int[])returnList.get(i);
            int pc = ret[0];
            bc.write16bit(pc, bc.currentPc() - pc + 1);
            ReturnHook hook = new JsrHook2(this, ret);
            finallyBlock.accept(this);
            hook.remove(this);
            if (!hasReturned) {
                bc.addOpcode(Opcode.GOTO);
                bc.addIndex(pc + 3 - bc.currentPc());
            }
        }
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
        int type = expr.getArrayType();
        ASTList size = expr.getArraySize();
        ASTList classname = expr.getClassName();
        ArrayInit init = expr.getInitializer();
        if (size.length() > 1) {
            if (init != null)
                throw new CompileError(
                        "sorry, multi-dimensional array initializer " +
                        "for new is not supported");

            atMultiNewArray(type, classname, size);
            return;
        }

        ASTree sizeExpr = size.head();
        atNewArrayExpr2(type, sizeExpr, Declarator.astToClassName(classname, '/'), init);
    }

    private void atNewArrayExpr2(int type, ASTree sizeExpr,
                        String jvmClassname, ArrayInit init) throws CompileError {
        if (init == null)
            if (sizeExpr == null)
                throw new CompileError("no array size");
            else
                sizeExpr.accept(this);
        else
            if (sizeExpr == null) {
                int s = init.length();
                bytecode.addIconst(s);
            }
            else
                throw new CompileError("unnecessary array size specified for new");

        String elementClass;
        if (type == CLASS) {
            elementClass = resolveClassName(jvmClassname);
            bytecode.addAnewarray(MemberResolver.jvmToJavaName(elementClass));
        }
        else {
            elementClass = null;
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

        if (init != null) {
            int s = init.length();
            ASTList list = init;
            for (int i = 0; i < s; i++) {
                bytecode.addOpcode(DUP);
                bytecode.addIconst(i);
                list.head().accept(this);
                if (!isRefType(type))
                    atNumCastExpr(exprType, type);

                bytecode.addOpcode(getArrayWriteOp(type, 0));
                list = list.tail();
            }
        }

        exprType = type;
        arrayDim = 1;
        className = elementClass;
    }

    private static void badNewExpr() throws CompileError {
        throw new CompileError("bad new expression");
    }

    protected void atArrayVariableAssign(ArrayInit init, int varType,
                                         int varArray, String varClass) throws CompileError {
        atNewArrayExpr2(varType, null, varClass, init);
    }

    public void atArrayInit(ArrayInit init) throws CompileError {
        throw new CompileError("array initializer is not supported");
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

        // generate code for evaluating arguments.
        atMethodArgs(args, types, dims, cnames);

        // used by invokeinterface
        int count = bytecode.getStackDepth() - stack + 1;

        if (found == null)
            found = resolver.lookupMethod(targetClass, thisClass, thisMethod,
                                          mname, types, dims, cnames);

        if (found == null) {
            String msg;
            if (mname.equals(MethodInfo.nameInit))
                msg = "constructor not found";
            else
                msg = "Method " + mname + " not found in "
                    + targetClass.getName();

            throw new CompileError(msg);
        }

        atMethodCallCore2(targetClass, mname, isStatic, isSpecial,
                          aload0pos, count, found);
    }

    private void atMethodCallCore2(CtClass targetClass, String mname,
                                   boolean isStatic, boolean isSpecial,
                                   int aload0pos, int count,
                                   MemberResolver.Method found)
        throws CompileError
    {
        CtClass declClass = found.declaring;
        MethodInfo minfo = found.info;
        String desc = minfo.getDescriptor();
        int acc = minfo.getAccessFlags();

        if (mname.equals(MethodInfo.nameInit)) {
            isSpecial = true;
            if (declClass != targetClass)
                throw new CompileError("no such constructor");

            if (declClass != thisClass && AccessFlag.isPrivate(acc)) {
                desc = getAccessibleConstructor(desc, declClass, minfo);
                bytecode.addOpcode(Opcode.ACONST_NULL); // the last parameter
            }
        }
        else if (AccessFlag.isPrivate(acc))
            if (declClass == thisClass)
                isSpecial = true;
            else {
                isSpecial = false;
                isStatic = true;
                String origDesc = desc;
                if ((acc & AccessFlag.STATIC) == 0)
                    desc = Descriptor.insertParameter(declClass.getName(),
                                                      origDesc);

                acc = AccessFlag.setPackage(acc) | AccessFlag.STATIC;
                mname = getAccessiblePrivate(mname, origDesc, desc,
                                             minfo, declClass);
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
        else if (isSpecial)    // if (isSpecial && notStatic(acc))
            bytecode.addInvokespecial(declClass, mname, desc);
        else {
            if (!Modifier.isPublic(declClass.getModifiers())
                || declClass.isInterface() != targetClass.isInterface())
                declClass = targetClass;

            if (declClass.isInterface())
                bytecode.addInvokeinterface(declClass, mname, desc, count);
            else
                if (isStatic)
                    throw new CompileError(mname + " is not static");
                else
                    bytecode.addInvokevirtual(declClass, mname, desc);
        }

        setReturnType(desc, isStatic, popTarget);
    }

    /*
     * Finds (or adds if necessary) a hidden accessor if the method
     * is in an enclosing class.
     *
     * @param desc          the descriptor of the method.
     * @param declClass     the class declaring the method.
     */
    protected String getAccessiblePrivate(String methodName, String desc,
                                          String newDesc, MethodInfo minfo,
                                          CtClass declClass)
        throws CompileError
    {
        if (isEnclosing(declClass, thisClass)) {
            AccessorMaker maker = declClass.getAccessorMaker();
            if (maker != null)
                return maker.getMethodAccessor(methodName, desc, newDesc,
                                               minfo);
        }

        throw new CompileError("Method " + methodName
                               + " is private");
    }

    /*
     * Finds (or adds if necessary) a hidden constructor if the given
     * constructor is in an enclosing class.
     *
     * @param desc          the descriptor of the constructor.
     * @param declClass     the class declaring the constructor.
     * @param minfo         the method info of the constructor.
     * @return the descriptor of the hidden constructor.
     */
    protected String getAccessibleConstructor(String desc, CtClass declClass,
                                              MethodInfo minfo)
        throws CompileError
    {
        if (isEnclosing(declClass, thisClass)) {
            AccessorMaker maker = declClass.getAccessorMaker();
            if (maker != null)
                return maker.getConstructor(declClass, desc, minfo);
        }

        throw new CompileError("the called constructor is private in "
                               + declClass.getName());
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
        CtField f = fieldAccess(left, false);
        boolean is_static = resultStatic;
        if (op != '=' && !is_static)
            bytecode.addOpcode(DUP);

        int fi;
        if (op == '=') {
            FieldInfo finfo = f.getFieldInfo2();
            setFieldType(finfo);
            AccessorMaker maker = isAccessibleField(f, finfo);            
            if (maker == null)
                fi = addFieldrefInfo(f, finfo);
            else
                fi = 0;
        }
        else
            fi = atFieldRead(f, is_static);

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

        atFieldAssignCore(f, is_static, fi, is2w);

        exprType = fType;
        arrayDim = fDim;
        className = cname;
    }

    /* If fi == 0, the field must be a private field in an enclosing class.
     */
    private void atFieldAssignCore(CtField f, boolean is_static, int fi,
                                   boolean is2byte) throws CompileError {
        if (fi != 0) {
            if (is_static) {
               bytecode.add(PUTSTATIC);
               bytecode.growStack(is2byte ? -2 : -1);
            }
            else {
                bytecode.add(PUTFIELD);
                bytecode.growStack(is2byte ? -3 : -2);
            }
        
            bytecode.addIndex(fi);
        }
        else {
            CtClass declClass = f.getDeclaringClass();
            AccessorMaker maker = declClass.getAccessorMaker();
            // make should be non null.
            FieldInfo finfo = f.getFieldInfo2();
            MethodInfo minfo = maker.getFieldSetter(finfo, is_static);
            bytecode.addInvokestatic(declClass, minfo.getName(),
                                     minfo.getDescriptor());
        }
    }

    /* overwritten in JvstCodeGen.
     */
    public void atMember(Member mem) throws CompileError {
        atFieldRead(mem);
    }

    protected void atFieldRead(ASTree expr) throws CompileError
    {
        CtField f = fieldAccess(expr, true);
        if (f == null) {
            atArrayLength(expr);
            return;
        }

        boolean is_static = resultStatic;
        ASTree cexpr = TypeChecker.getConstantFieldValue(f);
        if (cexpr == null)
            atFieldRead(f, is_static);
        else {
            cexpr.accept(this);
            setFieldType(f.getFieldInfo2());
        }
    }

    private void atArrayLength(ASTree expr) throws CompileError {
        if (arrayDim == 0)
            throw new CompileError(".length applied to a non array");

        bytecode.addOpcode(ARRAYLENGTH);
        exprType = INT;
        arrayDim = 0;
    }

    /**
     * Generates bytecode for reading a field value.
     * It returns a fieldref_info index or zero if the field is a private
     * one declared in an enclosing class. 
     */
    private int atFieldRead(CtField f, boolean isStatic) throws CompileError {
        FieldInfo finfo = f.getFieldInfo2();
        boolean is2byte = setFieldType(finfo);
        AccessorMaker maker = isAccessibleField(f, finfo);
        if (maker != null) {
            MethodInfo minfo = maker.getFieldGetter(finfo, isStatic);
            bytecode.addInvokestatic(f.getDeclaringClass(), minfo.getName(),
                                     minfo.getDescriptor());
            return 0;
        }
        else {
            int fi = addFieldrefInfo(f, finfo);
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
    }

    /**
     * Returns null if the field is accessible.  Otherwise, it throws
     * an exception or it returns AccessorMaker if the field is a private
     * one declared in an enclosing class.
     */
    private AccessorMaker isAccessibleField(CtField f, FieldInfo finfo)
        throws CompileError
    {
        if (AccessFlag.isPrivate(finfo.getAccessFlags())
            && f.getDeclaringClass() != thisClass) {
            CtClass declClass = f.getDeclaringClass(); 
            if (isEnclosing(declClass, thisClass)) {
                AccessorMaker maker = declClass.getAccessorMaker();
                if (maker != null)
                    return maker;
                else
                    throw new CompileError("fatal error.  bug?");
            }
            else
                throw new CompileError("Field " + f.getName() + " in "
                                       + declClass.getName() + " is private.");
        }

        return null;    // accessible field
    }

    /**
     * Sets exprType, arrayDim, and className.
     *
     * @return true if the field type is long or double. 
     */
    private boolean setFieldType(FieldInfo finfo) throws CompileError {
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

        boolean is2byte = (c == 'J' || c == 'D');
        return is2byte;
    }

    private int addFieldrefInfo(CtField f, FieldInfo finfo) {
        ConstPool cp = bytecode.getConstPool();
        String cname = f.getDeclaringClass().getName();
        int ci = cp.addClassInfo(cname);
        String name = finfo.getName();
        String type = finfo.getDescriptor();
        return cp.addFieldrefInfo(ci, name, type);
    }

    protected void atClassObject2(String cname) throws CompileError {
        if (getMajorVersion() < ClassFile.JAVA_5)
            super.atClassObject2(cname);
        else
            bytecode.addLdc(bytecode.getConstPool().addClassInfo(cname));
    }

    protected void atFieldPlusPlus(int token, boolean isPost,
                                   ASTree oprand, Expr expr, boolean doDup)
        throws CompileError
    {
        CtField f = fieldAccess(oprand, false);
        boolean is_static = resultStatic;
        if (!is_static)
            bytecode.addOpcode(DUP);

        int fi = atFieldRead(f, is_static);
        int t = exprType;
        boolean is2w = is2word(t, arrayDim);

        int dup_code;
        if (is_static)
            dup_code = (is2w ? DUP2 : DUP);
        else
            dup_code = (is2w ? DUP2_X1 : DUP_X1);

        atPlusPlusCore(dup_code, doDup, token, isPost, expr);
        atFieldAssignCore(f, is_static, fi, is2w);
    }

    /* This method also returns a value in resultStatic.
     *
     * @param acceptLength      true if array length is acceptable
     */
    protected CtField fieldAccess(ASTree expr, boolean acceptLength)
            throws CompileError
    {
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
                /* static member by # (extension by Javassist)
                 * For example, if int.class is parsed, the resulting tree
                 * is (# "java.lang.Integer" "TYPE"). 
                 */
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
                    else if (acceptLength && arrayDim > 0
                             && ((Symbol)e.oprand2()).get().equals("length"))
                        return null;    // expr is an array length.
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
