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

package javassist;

import javassist.bytecode.*;
import javassist.expr.ExprEditor;

/* Some methods do nothing except calling the super's method.
 * They might seem redundant but they are necessary so that javadoc
 * includes the description of those methods in the page of this class.
 */

/**
 * An instance of <code>CtMethod</code> represents a method.
 *
 * @see CtClass#getDeclaredMethods()
 * @see CtNewMethod
 */
public final class CtMethod extends CtBehavior {
    protected CtMethod next;
    protected int cachedHashCode;

    CtMethod(MethodInfo minfo, CtClass declaring) {
        super(declaring, minfo);
        next = null;
        cachedHashCode = 0;
    }

    /**
     * Creates a public abstract method.  The created method must be
     * added to a class with <code>CtClass.addMethod()</code>.
     *
     * @param declaring         the class to which the created method is added.
     * @param returnType        the type of the returned value
     * @param mname             the method name
     * @param parameters        a list of the parameter types
     *
     * @see CtClass#addMethod(CtMethod)
     */
    public CtMethod(CtClass returnType, String mname,
                    CtClass[] parameters, CtClass declaring) {
        this(null, declaring);
        ConstPool cp = declaring.getClassFile2().getConstPool();
        String desc = Descriptor.ofMethod(returnType, parameters);
        methodInfo = new MethodInfo(cp, mname, desc);
        setModifiers(Modifier.PUBLIC | Modifier.ABSTRACT);
    }

    /**
     * Creates a copy of a <code>CtMethod</code> object.
     * The created method must be
     * added to a class with <code>CtClass.addMethod()</code>.
     *
     * <p>All occurrences of class names in the created method
     * are replaced with names specified by
     * <code>map</code> if <code>map</code> is not <code>null</code>.
     *
     * <p>For example, suppose that a method <code>at()</code> is as
     * follows:
     *
     * <ul><pre>public X at(int i) {
     *     return (X)super.elementAt(i);
     * }</pre></ul>
     *
     * <p>(<code>X</code> is a class name.)  If <code>map</code> substitutes
     * <code>String</code> for <code>X</code>, then the created method is:
     *
     * <ul><pre>public String at(int i) {
     *     return (String)super.elementAt(i);
     * }</pre></ul>
     *
     * <p>By default, all the occurrences of the names of the class
     * declaring <code>at()</code> and the superclass are replaced
     * with the name of the class and the superclass that the
     * created method is added to.
     * This is done whichever <code>map</code> is null or not.
     * To prevent this replacement, call <code>ClassMap.fix()</code>.
     *
     * <p><b>Note:</b> if the <code>.class</code> notation (for example,
     * <code>String.class</code>) is included in an expression, the
     * Javac compiler may produce a helper method.
     * Since this constructor never
     * copies this helper method, the programmers have the responsiblity of
     * copying it.  Otherwise, use <code>Class.forName()</code> in the
     * expression.
     *
     * @param src       the source method.
     * @param declaring    the class to which the created method is added.
     * @param map       the hashtable associating original class names
     *                  with substituted names.
     *                  It can be <code>null</code>.
     *
     * @see CtClass#addMethod(CtMethod)
     * @see ClassMap#fix(String)
     */
    public CtMethod(CtMethod src, CtClass declaring, ClassMap map)
        throws CannotCompileException
    {
        this(null, declaring);
        MethodInfo srcInfo = src.methodInfo;
        CtClass srcClass = src.getDeclaringClass();
        ConstPool cp = declaring.getClassFile2().getConstPool();
        if (map == null)
            map = new ClassMap();

        map.put(srcClass.getName(), declaring.getName());
        try {
            CtClass srcSuper = srcClass.getSuperclass();
            if (srcSuper != null) {
                String srcSuperName = srcSuper.getName();
                if (!srcSuperName.equals(CtClass.javaLangObject))
                    map.put(srcSuperName,
                            declaring.getSuperclass().getName());
            }

            methodInfo = new MethodInfo(cp, srcInfo.getName(), srcInfo, map);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    static CtMethod append(CtMethod list, CtMethod tail) {
        tail.next = null;
        if (list == null)
            return tail;
        else {
            CtMethod lst = list;
            while (lst.next != null)
                lst = lst.next;

            lst.next = tail;
            return list;
        }
    }

    static int count(CtMethod m) {
        int n = 0;
        while (m != null) {
            ++n;
            m = m.next;
        }

        return n;
    }

    /**
     * Returns a hash code value for the method.
     * If two methods have the same name and signature, then
     * the hash codes for the two methods are equal.
     */
    public int hashCode() {
        /* This method is overridden in ExistingMethod for optimization.
         */
        if (cachedHashCode == 0) {
            String signature
                = methodInfo.getName() + ':' + methodInfo.getDescriptor();

            // System.identityHashCode() returns 0 only for null.
            cachedHashCode = System.identityHashCode(signature.intern());
        }

        return cachedHashCode;
    }

    /**
     * Indicates whether <code>obj</code> has the same name and the
     * same signature as this method.
     */
    public boolean equals(Object obj) {
        return obj != null && obj instanceof CtMethod
               && obj.hashCode() == hashCode();
    }

    /**
     * Returns the MethodInfo representing the method in the class file.
     */
    public MethodInfo getMethodInfo() {
        return super.getMethodInfo();
    }

    /**
     * Obtains the modifiers of the method.
     *
     * @return          modifiers encoded with
     *                  <code>javassist.Modifier</code>.
     * @see Modifier
     */
    public int getModifiers() {
        return super.getModifiers();
    }

    /**
     * Sets the encoded modifiers of the method.
     *
     * <p>Changing the modifiers may cause a problem.
     * For example, if a non-static method is changed to static,
     * the method will be rejected by the bytecode verifier.
     *
     * @see Modifier
     */
    public void setModifiers(int mod) {
        super.setModifiers(mod);
    }

    /**
     * Obtains the name of this method.
     */
    public String getName() {
        return methodInfo.getName();
    }

    /**
     * Changes the name of this method.
     */
    public void setName(String newname) {
        declaringClass.checkModify();
        methodInfo.setName(newname);
    }

    /**
     * Returns the class that declares this method.
     */
    public CtClass getDeclaringClass() {
        return super.getDeclaringClass();
    }

    /**
     * Obtains parameter types of this method.
     */
    public CtClass[] getParameterTypes() throws NotFoundException {
        return super.getParameterTypes();
    }

    /**
     * Obtains the type of the returned value.
     */
    public CtClass getReturnType() throws NotFoundException {
        return getReturnType0();
    }

    /**
     * Returns the character string representing the parameter types
     * and the return type.  If two methods have the same parameter types
     * and the return type, <code>getSignature()</code> returns the
     * same string.
     */
    public String getSignature() {
        return super.getSignature();
    }

    /**
     * Obtains exceptions that this method may throw.
     */
    public CtClass[] getExceptionTypes() throws NotFoundException {
        return super.getExceptionTypes();
    }

    /**
     * Sets exceptions that this method may throw.
     *
     * @param types     exception types (or null)
     */
    public void setExceptionTypes(CtClass[] types) throws NotFoundException {
        super.setExceptionTypes(types);
    }

    /**
     * Returns true if the method body is empty, that is, <code>{}</code>.
     * It also returns true if the method is an abstract method.
     */
    public boolean isEmpty() {
        CodeAttribute ca = getMethodInfo2().getCodeAttribute();
        if (ca == null)         // abstract or native
            return (getModifiers() & Modifier.ABSTRACT) != 0;

        CodeIterator it = ca.iterator();
        try {
            return it.hasNext() && it.byteAt(it.next()) == Opcode.RETURN
                && !it.hasNext();
        }
        catch (BadBytecode e) {}
        return false;
    }

    /**
     * Sets a method body.
     *
     * @param src       the source code representing the method body.
     *                  It must be a single statement or block.
     *                  If it is <code>null</code>, the substituted method
     *                  body does nothing except returning zero or null.
     */
    public void setBody(String src) throws CannotCompileException {
        super.setBody(src);
    }

    /**
     * Copies a method body from another method.
     * If this method is abstract, the abstract modifier is removed
     * after the method body is copied.
     *
     * <p>All occurrences of the class names in the copied method body
     * are replaced with the names specified by
     * <code>map</code> if <code>map</code> is not <code>null</code>.
     *
     * @param src       the method that the body is copied from.
     * @param map       the hashtable associating original class names
     *                  with substituted names.
     *                  It can be <code>null</code>.
     */
    public void setBody(CtMethod src, ClassMap map)
        throws CannotCompileException
    {
        setBody0(src.declaringClass, src.methodInfo,
                 declaringClass, methodInfo, map);
    }

    /**
     * Replace a method body with a new method body wrapping the
     * given method.
     *
     * @param mbody             the wrapped method
     * @param constParam        the constant parameter given to
     *                          the wrapped method
     *                          (maybe <code>null</code>).
     *
     * @see CtNewMethod#wrapped(CtClass,String,CtClass[],CtClass[],CtMethod,CtMethod.ConstParameter,CtClass)
     */
    public void setWrappedBody(CtMethod mbody, ConstParameter constParam)
        throws CannotCompileException
    {
        declaringClass.checkModify();

        CtClass clazz = getDeclaringClass();
        CtClass[] params;
        CtClass retType;
        try {
            params = getParameterTypes();
            retType = getReturnType();
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }

        Bytecode code = CtNewWrappedMethod.makeBody(clazz,
                                                    clazz.getClassFile2(),
                                                    mbody,
                                                    params, retType,
                                                    constParam);
        CodeAttribute cattr = code.toCodeAttribute();
        methodInfo.setCodeAttribute(cattr);
        methodInfo.setAccessFlags(methodInfo.getAccessFlags()
                                  & ~AccessFlag.ABSTRACT);
    }

    /**
     * Obtains an attribute with the given name.
     * If that attribute is not found in the class file, this
     * method returns null.
     *
     * @param name              attribute name
     */
    public byte[] getAttribute(String name) {
        return super.getAttribute(name);
    }

    /**
     * Adds an attribute. The attribute is saved in the class file.
     *
     * @param name      attribute name
     * @param data      attribute value
     */
    public void setAttribute(String name, byte[] data) {
        super.setAttribute(name, data);
    }

    /**
     * Declares to use <code>$cflow</code> for this method.
     * If <code>$cflow</code> is used, the class files modified
     * with Javassist requires a support class
     * <code>javassist.runtime.Cflow</code> at runtime
     * (other Javassist classes are not required at runtime).
     *
     * <p>Every <code>$cflow</code> variable is given a unique name.
     * For example, if the given name is <code>"Point.paint"</code>,
     * then the variable is indicated by <code>$cflow(Point.paint)</code>.
     *
     * @param name      <code>$cflow</code> name.  It can include
     *                  alphabets, numbers, <code>_</code>,
     *                  <code>$</code>, and <code>.</code> (dot).
     *
     * @see javassist.runtime.Cflow
     */
    public void useCflow(String name) throws CannotCompileException {
        super.useCflow(name);
    }

    /**
     * Modifies the method body.
     *
     * @param converter         specifies how to modify.
     */
    public void instrument(CodeConverter converter)
        throws CannotCompileException
    {
        super.instrument(converter);
    }

    /**
     * Modifies the method body.
     *
     * @param editor            specifies how to modify.
     */
    public void instrument(ExprEditor editor)
        throws CannotCompileException
    {
        super.instrument(editor);
    }

    /**
     * Inserts bytecode at the beginning of the method body.
     *
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     */
    public void insertBefore(String src) throws CannotCompileException {
        super.insertBefore(src);
    }

    /**
     * Inserts bytecode at the end of the method body.
     * The bytecode is inserted just before every return insturction.
     * It is not executed when an exception is thrown.
     *
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     */
    public void insertAfter(String src)
        throws CannotCompileException
    {
        super.insertAfter(src);
    }

    /**
     * Inserts bytecode at the end of the method body.
     * The bytecode is inserted just before every return insturction.
     *
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     * @param asFinally         true if the inserted bytecode is executed
     *                  not only when the transfer normally returns
     *                  but also when an exception is thrown.
     */
    public void insertAfter(String src, boolean asFinally)
        throws CannotCompileException
    {
        super.insertAfter(src, asFinally);
    }

    /**
     * Adds a catch clause that handles an exception thrown in the
     * method body.
     * The catch clause must end with a return or throw statement.
     *
     * @param src       the source code representing the catch clause.
     *                  It must be a single statement or block.
     * @param exceptionType     the type of the exception handled by the
     *                          catch clause.
     */
    public void addCatch(String src, CtClass exceptionType)
        throws CannotCompileException
    {
        super.addCatch(src, exceptionType);
    }

    // inner classes

    /**
     * Instances of this class represent a constant parameter.
     * They are used to specify the parameter given to the methods
     * created by <code>CtNewMethod.wrapped()</code>.
     *
     * @see CtMethod#setWrappedBody(CtMethod,CtMethod.ConstParameter)
     * @see CtNewMethod#wrapped(CtClass,String,CtClass[],CtClass[],CtMethod,CtMethod.ConstParameter,CtClass)
     * @see CtNewConstructor#make(CtClass[],CtClass[],int,CtMethod,CtMethod.ConstParameter,CtClass)
     */
    public static class ConstParameter {
        /**
         * Makes an integer constant.
         *
         * @param i             the constant value.
         */
        public static ConstParameter integer(int i) {
            return new IntConstParameter(i);
        }

        /**
         * Makes a long integer constant.
         *
         * @param i             the constant value.
         */
        public static ConstParameter integer(long i) {
            return new LongConstParameter(i);
        }

        /**
         * Makes an <code>String</code> constant.
         *
         * @param s             the constant value.
         */
        public static ConstParameter string(String s) {
            return new StringConstParameter(s);
        }

        ConstParameter() {}

        /**
         * @return      the size of the stack consumption.
         */
        int compile(Bytecode code) throws CannotCompileException {
            return 0;
        }

        String descriptor() {
            return defaultDescriptor();
        }

        /**
         * @see CtNewWrappedMethod
         */
        static String defaultDescriptor() {
            return "([Ljava/lang/Object;)Ljava/lang/Object;";
        }

        /**
         * Returns the descriptor for constructors.
         *
         * @see CtNewWrappedConstructor
         */
        String constDescriptor() {
            return defaultConstDescriptor();
        }

        /**
         * Returns the default descriptor for constructors.
         */
        static String defaultConstDescriptor() {
            return "([Ljava/lang/Object;)V";
        }
    }

    static class IntConstParameter extends ConstParameter {
        int param;

        IntConstParameter(int i) {
            param = i;
        }

        int compile(Bytecode code) throws CannotCompileException {
            code.addIconst(param);
            return 1;
        }

        String descriptor() {
            return "([Ljava/lang/Object;I)Ljava/lang/Object;";
        }

        String constDescriptor() {
            return "([Ljava/lang/Object;I)V";
        }
    }

    static class LongConstParameter extends ConstParameter {
        long param;

        LongConstParameter(long l) {
            param = l;
        }

        int compile(Bytecode code) throws CannotCompileException {
            code.addLconst(param);
            return 2;
        }

        String descriptor() {
            return "([Ljava/lang/Object;J)Ljava/lang/Object;";
        }

        String constDescriptor() {
            return "([Ljava/lang/Object;J)V";
        }
    }

    static class StringConstParameter extends ConstParameter {
        String param;

        StringConstParameter(String s) {
            param = s;
        }

        int compile(Bytecode code) throws CannotCompileException {
            code.addLdc(param);
            return 1;
        }

        String descriptor() {
            return "([Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;";
        }

        String constDescriptor() {
            return "([Ljava/lang/Object;Ljava/lang/String;)V";
        }
    }
}
