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

package javassist;

import javassist.bytecode.*;

/**
 * An instance of <code>CtMethod</code> represents a method.
 *
 * <p>See the super class <code>CtBehavior</code> since
 * a number of useful methods are in <code>CtBehavior</code>.
 * A number of useful factory methods are in <code>CtNewMethod</code>.
 *
 * @see CtClass#getDeclaredMethods()
 * @see CtNewMethod
 */
public final class CtMethod extends CtBehavior {
    protected String cachedStringRep;

    /**
     * @see #make(MethodInfo minfo, CtClass declaring)
     */
    CtMethod(MethodInfo minfo, CtClass declaring) {
        super(declaring, minfo);
        cachedStringRep = null;
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
     * To prevent this replacement, call <code>ClassMap.fix()</code>
     * or <code>put()</code> to explicitly specify replacement.
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
        copy(src, false, map);
    }

    /**
     * Compiles the given source code and creates a method.
     * This method simply delegates to <code>make()</code> in
     * <code>CtNewMethod</code>.  See it for more details.
     * <code>CtNewMethod</code> has a number of useful factory methods.
     *
     * @param src               the source text. 
     * @param declaring    the class to which the created method is added.
     * @see CtNewMethod#make(String, CtClass)
     */
    public static CtMethod make(String src, CtClass declaring)
        throws CannotCompileException
    {
        return CtNewMethod.make(src, declaring);
    }

    /**
     * Creates a method from a <code>MethodInfo</code> object.
     *
     * @param declaring     the class declaring the method.
     * @throws CannotCompileException       if the the <code>MethodInfo</code>
     *          object and the declaring class have different
     *          <code>ConstPool</code> objects
     * @since 3.6
     */
    public static CtMethod make(MethodInfo minfo, CtClass declaring)
        throws CannotCompileException
    {
        if (declaring.getClassFile2().getConstPool() != minfo.getConstPool())
            throw new CannotCompileException("bad declaring class");

        return new CtMethod(minfo, declaring);
    }

    /**
     * Returns a hash code value for the method.
     * If two methods have the same name and signature, then
     * the hash codes for the two methods are equal.
     */
    public int hashCode() {
        return getStringRep().hashCode();
    }

    /**
     * This method is invoked when setName() or replaceClassName()
     * in CtClass is called.
     */
    void nameReplaced() {
        cachedStringRep = null;
    }

    /* This method is also called by CtClassType.getMethods0(). 
     */
    final String getStringRep() {
        if (cachedStringRep == null)
            cachedStringRep = methodInfo.getName()
                + Descriptor.getParamDescriptor(methodInfo.getDescriptor());

        return cachedStringRep;
    }

    /**
     * Indicates whether <code>obj</code> has the same name and the
     * same signature as this method.
     */
    public boolean equals(Object obj) {
        return obj != null && obj instanceof CtMethod
               && ((CtMethod)obj).getStringRep().equals(getStringRep());
    }

    /**
     * Returns the method name followed by parameter types
     * such as <code>javassist.CtMethod.setBody(String)</code>.
     *
     * @since 3.5
     */
    public String getLongName() {
        return getDeclaringClass().getName() + "."
               + getName() + Descriptor.toString(getSignature());
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
     * Obtains the type of the returned value.
     */
    public CtClass getReturnType() throws NotFoundException {
        return getReturnType0();
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
        // rebuilding a stack map table is not needed.
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
