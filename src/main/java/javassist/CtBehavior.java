/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.ExceptionsAttribute;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.LocalVariableTypeAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.StackMap;
import javassist.bytecode.StackMapTable;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import javassist.expr.ExprEditor;

/**
 * <code>CtBehavior</code> represents a method, a constructor,
 * or a static constructor (class initializer). 
 * It is the abstract super class of
 * <code>CtMethod</code> and <code>CtConstructor</code>.
 *
 * <p>To directly read or modify bytecode, obtain <code>MethodInfo</code>
 * objects.
 *
 * @see #getMethodInfo()
 */
public abstract class CtBehavior extends CtMember {
    protected MethodInfo methodInfo;

    protected CtBehavior(CtClass clazz, MethodInfo minfo) {
        super(clazz);
        methodInfo = minfo;
    }

    /**
     * @param isCons        true if this is a constructor.
     */
    void copy(CtBehavior src, boolean isCons, ClassMap map)
        throws CannotCompileException
    {
        CtClass declaring = declaringClass;
        MethodInfo srcInfo = src.methodInfo;
        CtClass srcClass = src.getDeclaringClass();
        ConstPool cp = declaring.getClassFile2().getConstPool();

        map = new ClassMap(map);
        map.put(srcClass.getName(), declaring.getName());
        try {
            boolean patch = false;
            CtClass srcSuper = srcClass.getSuperclass();
            CtClass destSuper = declaring.getSuperclass();
            String destSuperName = null;
            if (srcSuper != null && destSuper != null) {
                String srcSuperName = srcSuper.getName();
                destSuperName = destSuper.getName();
                if (!srcSuperName.equals(destSuperName))
                    if (srcSuperName.equals(CtClass.javaLangObject))
                        patch = true;
                    else
                        map.putIfNone(srcSuperName, destSuperName);
            }

            // a stack map table is copied from srcInfo.
            methodInfo = new MethodInfo(cp, srcInfo.getName(), srcInfo, map);
            if (isCons && patch)
                methodInfo.setSuperclass(destSuperName);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    @Override
    protected void extendToString(StringBuilder buffer) {
        buffer.append(' ');
        buffer.append(getName());
        buffer.append(' ');
        buffer.append(methodInfo.getDescriptor());
    }

    /**
     * Returns the method or constructor name followed by parameter types
     * such as <code>javassist.CtBehavior.stBody(String)</code>.
     *
     * @since 3.5
     */
    public abstract String getLongName();

    /**
     * Returns the <code>MethodInfo</code> representing this method/constructor in the
     * class file.
     *
     * <p>If you modify the bytecode through the returned
     * <code>MethodInfo</code> object, you might have to explicitly
     * rebuild a stack map table.  Javassist does not automatically
     * rebuild it for avoiding unnecessary rebuilding.
     *
     * @see javassist.bytecode.MethodInfo#rebuildStackMap(ClassPool)
     */
    public MethodInfo getMethodInfo() {
        declaringClass.checkModify();
        return methodInfo;
    }

    /**
     * Returns the <code>MethodInfo</code> representing the method/constructor in the
     * class file (read only).
     * Normal applications do not need calling this method.  Use
     * <code>getMethodInfo()</code>.
     *
     * <p>The <code>MethodInfo</code> object obtained by this method
     * is read only.  Changes to this object might not be reflected
     * on a class file generated by <code>toBytecode()</code>,
     * <code>toClass()</code>, etc in <code>CtClass</code>.
     *
     * <p>This method is available even if the <code>CtClass</code>
     * containing this method is frozen.  However, if the class is
     * frozen, the <code>MethodInfo</code> might be also pruned.
     *
     * @see #getMethodInfo()
     * @see CtClass#isFrozen()
     * @see CtClass#prune()
     */
    public MethodInfo getMethodInfo2() { return methodInfo; }

    /**
     * Obtains the modifiers of the method/constructor.
     *
     * @return          modifiers encoded with
     *                  <code>javassist.Modifier</code>.
     * @see Modifier
     */
    @Override
    public int getModifiers() {
        return AccessFlag.toModifier(methodInfo.getAccessFlags());
    }

    /**
     * Sets the encoded modifiers of the method/constructor.
     *
     * <p>Changing the modifiers may cause a problem.
     * For example, if a non-static method is changed to static,
     * the method will be rejected by the bytecode verifier.
     *
     * @see Modifier
     */
    @Override
    public void setModifiers(int mod) {
        declaringClass.checkModify();
        methodInfo.setAccessFlags(AccessFlag.of(mod));
    }

    /**
     * Returns true if the class has the specified annotation type.
     *
     * @param typeName      the name of annotation type.
     * @return <code>true</code> if the annotation is found,
     *         otherwise <code>false</code>.
     * @since 3.21
     */
    @Override
    public boolean hasAnnotation(String typeName) {
       MethodInfo mi = getMethodInfo2();
       AnnotationsAttribute ainfo = (AnnotationsAttribute)
                   mi.getAttribute(AnnotationsAttribute.invisibleTag);  
       AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                   mi.getAttribute(AnnotationsAttribute.visibleTag);  
       return CtClassType.hasAnnotationType(typeName,
                                            getDeclaringClass().getClassPool(),
                                            ainfo, ainfo2);
    }

    /**
     * Returns the annotation if the class has the specified annotation class.
     * For example, if an annotation <code>@Author</code> is associated
     * with this method/constructor, an <code>Author</code> object is returned.
     * The member values can be obtained by calling methods on
     * the <code>Author</code> object.
     *
     * @param clz the annotation class.
     * @return the annotation if found, otherwise <code>null</code>.
     * @since 3.11
     */
    @Override
    public Object getAnnotation(Class<?> clz) throws ClassNotFoundException {
       MethodInfo mi = getMethodInfo2();
       AnnotationsAttribute ainfo = (AnnotationsAttribute)
                   mi.getAttribute(AnnotationsAttribute.invisibleTag);  
       AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                   mi.getAttribute(AnnotationsAttribute.visibleTag);  
       return CtClassType.getAnnotationType(clz,
                                            getDeclaringClass().getClassPool(),
                                            ainfo, ainfo2);
    }

    /**
     * Returns the annotations associated with this method or constructor.
     *
     * @return an array of annotation-type objects.
     * @see #getAvailableAnnotations()
     * @since 3.1
     */
    @Override
    public Object[] getAnnotations() throws ClassNotFoundException {
       return getAnnotations(false);
   }

    /**
     * Returns the annotations associated with this method or constructor.
     * If any annotations are not on the classpath, they are not included
     * in the returned array.
     * 
     * @return an array of annotation-type objects.
     * @see #getAnnotations()
     * @since 3.3
     */
    @Override
    public Object[] getAvailableAnnotations(){
       try{
           return getAnnotations(true);
       }
       catch (ClassNotFoundException e){
           throw new RuntimeException("Unexpected exception", e);
       }
    }

    private Object[] getAnnotations(boolean ignoreNotFound)
       throws ClassNotFoundException
    {
       MethodInfo mi = getMethodInfo2();
       AnnotationsAttribute ainfo = (AnnotationsAttribute)
                   mi.getAttribute(AnnotationsAttribute.invisibleTag);  
       AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                   mi.getAttribute(AnnotationsAttribute.visibleTag);  
       return CtClassType.toAnnotationType(ignoreNotFound,
                                           getDeclaringClass().getClassPool(),
                                           ainfo, ainfo2);
    }

    /**
     * Returns the parameter annotations associated with this method or constructor.
     *
     * @return an array of annotation-type objects.  The length of the returned array is
     * equal to the number of the formal parameters.  If each parameter has no
     * annotation, the elements of the returned array are empty arrays.
     *
     * @see #getAvailableParameterAnnotations()
     * @see #getAnnotations()
     * @since 3.1
     */
    public Object[][] getParameterAnnotations() throws ClassNotFoundException {
        return getParameterAnnotations(false);
    }

    /**
     * Returns the parameter annotations associated with this method or constructor.
     * If any annotations are not on the classpath, they are not included in the
     * returned array.
     * 
     * @return an array of annotation-type objects.  The length of the returned array is
     * equal to the number of the formal parameters.  If each parameter has no
     * annotation, the elements of the returned array are empty arrays.
     *
     * @see #getParameterAnnotations()
     * @see #getAvailableAnnotations()
     * @since 3.3
     */
    public Object[][] getAvailableParameterAnnotations(){
        try {
            return getParameterAnnotations(true);
        }
        catch(ClassNotFoundException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    Object[][] getParameterAnnotations(boolean ignoreNotFound)
        throws ClassNotFoundException
    {
        MethodInfo mi = getMethodInfo2();
        ParameterAnnotationsAttribute ainfo = (ParameterAnnotationsAttribute)
                    mi.getAttribute(ParameterAnnotationsAttribute.invisibleTag);  
        ParameterAnnotationsAttribute ainfo2 = (ParameterAnnotationsAttribute)
                    mi.getAttribute(ParameterAnnotationsAttribute.visibleTag);  
        return CtClassType.toAnnotationType(ignoreNotFound,
                                            getDeclaringClass().getClassPool(),
                                            ainfo, ainfo2, mi);
    }

    /**
     * Obtains parameter types of this method/constructor.
     */
    public CtClass[] getParameterTypes() throws NotFoundException {
        return Descriptor.getParameterTypes(methodInfo.getDescriptor(),
                                            declaringClass.getClassPool());
    }

    /**
     * Obtains the type of the returned value.
     */
    CtClass getReturnType0() throws NotFoundException {
        return Descriptor.getReturnType(methodInfo.getDescriptor(),
                                        declaringClass.getClassPool());
    }

    /**
     * Returns the method signature (the parameter types
     * and the return type).
     * The method signature is represented by a character string
     * called method descriptor, which is defined in the JVM specification.
     * If two methods/constructors have
     * the same parameter types
     * and the return type, <code>getSignature()</code> returns the
     * same string (the return type of constructors is <code>void</code>).
     *
     * <p>Note that the returned string is not the type signature
     * contained in the <code>SignatureAttirbute</code>.  It is
     * a descriptor.
     *
     * @see javassist.bytecode.Descriptor
     * @see #getGenericSignature()
     */
    @Override
    public String getSignature() {
        return methodInfo.getDescriptor();
    }

    /**
     * Returns the generic signature of the method.
     * It represents parameter types including type variables.
     *
     * @see SignatureAttribute#toMethodSignature(String)
     * @since 3.17
     */
    @Override
    public String getGenericSignature() {
        SignatureAttribute sa
            = (SignatureAttribute)methodInfo.getAttribute(SignatureAttribute.tag);
        return sa == null ? null : sa.getSignature();
    }

    /**
     * Set the generic signature of the method.
     * It represents parameter types including type variables.
     * See {@link javassist.CtClass#setGenericSignature(String)}
     * for a code sample.
     *
     * @param sig       a new generic signature.
     * @see javassist.bytecode.SignatureAttribute.MethodSignature#encode()
     * @since 3.17
     */
    @Override
    public void setGenericSignature(String sig) {
        declaringClass.checkModify();
        methodInfo.addAttribute(new SignatureAttribute(methodInfo.getConstPool(), sig));
    }

    /**
     * Obtains exceptions that this method/constructor may throw.
     *
     * @return a zero-length array if there is no throws clause.
     */
    public CtClass[] getExceptionTypes() throws NotFoundException {
        String[] exceptions;
        ExceptionsAttribute ea = methodInfo.getExceptionsAttribute();
        if (ea == null)
            exceptions = null;
        else
            exceptions = ea.getExceptions();

        return declaringClass.getClassPool().get(exceptions);
    }

    /**
     * Sets exceptions that this method/constructor may throw.
     */
    public void setExceptionTypes(CtClass[] types) throws NotFoundException {
        declaringClass.checkModify();
        if (types == null || types.length == 0) {
            methodInfo.removeExceptionsAttribute();
            return;
        }

        String[] names = new String[types.length];
        for (int i = 0; i < types.length; ++i)
            names[i] = types[i].getName();

        ExceptionsAttribute ea = methodInfo.getExceptionsAttribute();
        if (ea == null) {
            ea = new ExceptionsAttribute(methodInfo.getConstPool());
            methodInfo.setExceptionsAttribute(ea);
        }

        ea.setExceptions(names);
    }

    /**
     * Returns true if the body is empty.
     */
    public abstract boolean isEmpty();

    /**
     * Sets a method/constructor body.
     *
     * @param src       the source code representing the body.
     *                  It must be a single statement or block.
     *                  If it is <code>null</code>, the substituted
     *                  body does nothing except returning zero or null.
     */
    public void setBody(String src) throws CannotCompileException {
        setBody(src, null, null);
    }

    /**
     * Sets a method/constructor body.
     *
     * @param src       the source code representing the body.
     *                  It must be a single statement or block.
     *                  If it is <code>null</code>, the substituted
     *                  body does nothing except returning zero or null.
     * @param delegateObj       the source text specifying the object
     *                          that is called on by <code>$proceed()</code>.
     * @param delegateMethod    the name of the method
     *                          that is called by <code>$proceed()</code>.
     */
    public void setBody(String src,
                        String delegateObj, String delegateMethod)
        throws CannotCompileException
    {
        CtClass cc = declaringClass;
        cc.checkModify();
        try {
            Javac jv = new Javac(cc);
            if (delegateMethod != null)
                jv.recordProceed(delegateObj, delegateMethod);

            Bytecode b = jv.compileBody(this, src);
            methodInfo.setCodeAttribute(b.toCodeAttribute());
            methodInfo.setAccessFlags(methodInfo.getAccessFlags()
                                      & ~AccessFlag.ABSTRACT);
            methodInfo.rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());
            declaringClass.rebuildClassFile();
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        } catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    static void setBody0(CtClass srcClass, MethodInfo srcInfo,
                         CtClass destClass, MethodInfo destInfo,
                         ClassMap map)
        throws CannotCompileException
    {
        destClass.checkModify();

        map = new ClassMap(map);
        map.put(srcClass.getName(), destClass.getName());
        try {
            CodeAttribute cattr = srcInfo.getCodeAttribute();
            if (cattr != null) {
                ConstPool cp = destInfo.getConstPool();
                CodeAttribute ca = (CodeAttribute)cattr.copy(cp, map);
                destInfo.setCodeAttribute(ca);
                // a stack map table is copied to destInfo.
            }
        }
        catch (CodeAttribute.RuntimeCopyException e) {
            /* the exception may be thrown by copy() in CodeAttribute.
             */
            throw new CannotCompileException(e);
        }

        destInfo.setAccessFlags(destInfo.getAccessFlags()
                                & ~AccessFlag.ABSTRACT);
        destClass.rebuildClassFile();
    }

    /**
     * Obtains an attribute with the given name.
     * If that attribute is not found in the class file, this
     * method returns null.
     *
     * <p>Note that an attribute is a data block specified by
     * the class file format.  It is not an annotation.
     * See {@link javassist.bytecode.AttributeInfo}.
     *
     * @param name              attribute name
     */
    @Override
    public byte[] getAttribute(String name)
    {
        AttributeInfo ai = methodInfo.getAttribute(name);
        if (ai == null)
            return null;
        return ai.get();
    }

    /**
     * Adds an attribute. The attribute is saved in the class file.
     *
     * <p>Note that an attribute is a data block specified by
     * the class file format.  It is not an annotation.
     * See {@link javassist.bytecode.AttributeInfo}.
     *
     * @param name      attribute name
     * @param data      attribute value
     */
    @Override
    public void setAttribute(String name, byte[] data)
    {
        declaringClass.checkModify();
        methodInfo.addAttribute(new AttributeInfo(methodInfo.getConstPool(),
                                                  name, data));
    }

    /**
     * Declares to use <code>$cflow</code> for this method/constructor.
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
    public void useCflow(String name) throws CannotCompileException
    {
        CtClass cc = declaringClass;
        cc.checkModify();
        ClassPool pool = cc.getClassPool();
        String fname;
        int i = 0;
        while (true) {
            fname = "_cflow$" + i++;
            try {
                cc.getDeclaredField(fname);
            }
            catch(NotFoundException e) {
                break;
            }
        }

        pool.recordCflow(name, declaringClass.getName(), fname);
        try {
            CtClass type = pool.get("javassist.runtime.Cflow");
            CtField field = new CtField(type, fname, cc);
            field.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            cc.addField(field, CtField.Initializer.byNew(type));
            insertBefore(fname + ".enter();", false);
            String src = fname + ".exit();";
            insertAfter(src, true);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
    }

    /**
     * Declares a new local variable.  The scope of this variable is the
     * whole method body.  The initial value of that variable is not set.
     * The declared variable can be accessed in the code snippet inserted
     * by <code>insertBefore()</code>, <code>insertAfter()</code>, etc.
     *
     * <p>If the second parameter <code>asFinally</code> to
     * <code>insertAfter()</code> is true, the declared local variable
     * is not visible from the code inserted by <code>insertAfter()</code>.
     *
     * @param name      the name of the variable
     * @param type      the type of the variable
     * @see #insertBefore(String)
     * @see #insertAfter(String)
     */
    public void addLocalVariable(String name, CtClass type)
        throws CannotCompileException
    {
        declaringClass.checkModify();
        ConstPool cp = methodInfo.getConstPool();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        if (ca == null)
            throw new CannotCompileException("no method body");

        LocalVariableAttribute va = (LocalVariableAttribute)ca.getAttribute(
                                                LocalVariableAttribute.tag);
        if (va == null) {
            va = new LocalVariableAttribute(cp);
            ca.getAttributes().add(va);
        }

        int maxLocals = ca.getMaxLocals();
        String desc = Descriptor.of(type);
        va.addEntry(0, ca.getCodeLength(),
                    cp.addUtf8Info(name), cp.addUtf8Info(desc), maxLocals);
        ca.setMaxLocals(maxLocals + Descriptor.dataSize(desc));
    }

    /**
     * Inserts a new parameter, which becomes the first parameter.
     */
    public void insertParameter(CtClass type)
        throws CannotCompileException
    {
        declaringClass.checkModify();
        String desc = methodInfo.getDescriptor();
        String desc2 = Descriptor.insertParameter(type, desc);
        try {
            addParameter2(Modifier.isStatic(getModifiers()) ? 0 : 1, type, desc);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }

        methodInfo.setDescriptor(desc2);
    }

    /**
     * Appends a new parameter, which becomes the last parameter.
     */
    public void addParameter(CtClass type)
        throws CannotCompileException
    {
        declaringClass.checkModify();
        String desc = methodInfo.getDescriptor();
        String desc2 = Descriptor.appendParameter(type, desc);
        int offset = Modifier.isStatic(getModifiers()) ? 0 : 1;
        try {
            addParameter2(offset + Descriptor.paramSize(desc), type, desc);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }

        methodInfo.setDescriptor(desc2);
    }

    private void addParameter2(int where, CtClass type, String desc)
        throws BadBytecode
    {
        CodeAttribute ca = methodInfo.getCodeAttribute();
        if (ca != null) {
            int size = 1;
            char typeDesc = 'L';
            int classInfo = 0;
            if (type.isPrimitive()) {
                CtPrimitiveType cpt = (CtPrimitiveType)type;
                size = cpt.getDataSize();
                typeDesc = cpt.getDescriptor();
            }
            else
                classInfo = methodInfo.getConstPool().addClassInfo(type);

            ca.insertLocalVar(where, size);
            LocalVariableAttribute va
                = (LocalVariableAttribute)ca.getAttribute(LocalVariableAttribute.tag);
            if (va != null)
                va.shiftIndex(where, size);

            LocalVariableTypeAttribute lvta
                = (LocalVariableTypeAttribute)ca.getAttribute(LocalVariableTypeAttribute.tag);
            if (lvta != null)
                lvta.shiftIndex(where, size);

            StackMapTable smt = (StackMapTable)ca.getAttribute(StackMapTable.tag);
            if (smt != null)
                smt.insertLocal(where, StackMapTable.typeTagOf(typeDesc), classInfo);

            StackMap sm = (StackMap)ca.getAttribute(StackMap.tag);
            if (sm != null)
                sm.insertLocal(where, StackMapTable.typeTagOf(typeDesc), classInfo);
        }
    }

    /**
     * Modifies the method/constructor body.
     *
     * @param converter         specifies how to modify.
     */
    public void instrument(CodeConverter converter)
        throws CannotCompileException
    {
        declaringClass.checkModify();
        ConstPool cp = methodInfo.getConstPool();
        converter.doit(getDeclaringClass(), methodInfo, cp);
    }

    /**
     * Modifies the method/constructor body.
     *
     * <p>While executing this method, only <code>replace()</code>
     * in <code>Expr</code> is available for bytecode modification.
     * Other methods such as <code>insertBefore()</code> may collapse
     * the bytecode because the <code>ExprEditor</code> loses
     * its current position.  
     *
     * @param editor            specifies how to modify.
     * @see javassist.expr.Expr#replace(String)
     * @see #insertBefore(String)
     */
    public void instrument(ExprEditor editor)
        throws CannotCompileException
    {
        // if the class is not frozen,
        // does not turn the modified flag on.
        if (declaringClass.isFrozen())
            declaringClass.checkModify();

        if (editor.doit(declaringClass, methodInfo))
            declaringClass.checkModify();
    }

    /**
     * Inserts bytecode at the beginning of the body.
     *
     * <p>If this object represents a constructor,
     * the bytecode is inserted before
     * a constructor in the super class or this class is called.
     * Therefore, the inserted bytecode is subject to constraints described
     * in Section 4.8.2 of The Java Virtual Machine Specification (2nd ed).
     * For example, it cannot access instance fields or methods although
     * it may assign a value to an instance field directly declared in this
     * class.  Accessing static fields and methods is allowed.
     * Use <code>insertBeforeBody()</code> in <code>CtConstructor</code>.
     *
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     * @see CtConstructor#insertBeforeBody(String)
     */
    public void insertBefore(String src) throws CannotCompileException {
        insertBefore(src, true);
    }

    private void insertBefore(String src, boolean rebuild)
        throws CannotCompileException
    {
        CtClass cc = declaringClass;
        cc.checkModify();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        if (ca == null)
            throw new CannotCompileException("no method body");

        CodeIterator iterator = ca.iterator();
        Javac jv = new Javac(cc);
        try {
            int nvars = jv.recordParams(getParameterTypes(),
                                        Modifier.isStatic(getModifiers()));
            jv.recordParamNames(ca, nvars);
            jv.recordLocalVariables(ca, 0);
            jv.recordReturnType(getReturnType0(), false);
            jv.compileStmnt(src);
            Bytecode b = jv.getBytecode();
            int stack = b.getMaxStack();
            int locals = b.getMaxLocals();

            if (stack > ca.getMaxStack())
                ca.setMaxStack(stack);

            if (locals > ca.getMaxLocals())
                ca.setMaxLocals(locals);

            int pos = iterator.insertEx(b.get());
            iterator.insert(b.getExceptionTable(), pos);
            if (rebuild)
                methodInfo.rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    /**
     * Inserts bytecode at the end of the body.
     * The bytecode is inserted just before every return instruction.
     * It is not executed when an exception is thrown.
     *
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     */
    public void insertAfter(String src)
        throws CannotCompileException
    {
        insertAfter(src, false, false);
    }

    /**
     * Inserts bytecode at the end of the body.
     * The bytecode is inserted just before every return instruction.
     *
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     * @param asFinally         true if the inserted bytecode is executed
     *                  not only when the control normally returns
     *                  but also when an exception is thrown.
     *                  If this parameter is true, the inserted code cannot
     *                  access local variables.
     */
    public void insertAfter(String src, boolean asFinally)
        throws CannotCompileException
    {
        insertAfter(src, asFinally, false);
    }

    /**
     * Inserts bytecode at the end of the body.
     * The bytecode is inserted just before every return instruction.
     *
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     * @param asFinally         true if the inserted bytecode is executed
     *                  not only when the control normally returns
     *                  but also when an exception is thrown.
     *                  If this parameter is true, the inserted code cannot
     *                  access local variables.
     * @param redundant if true, redundant bytecode will be generated.
     *                  the redundancy is necessary when some compilers (Kotlin?)
     *                  generate the original bytecode.
     *                  The other <code>insertAfter</code> methods calls this method
     *                  with <code>false</code> for this parameter.
     *                  A tip is to pass <code>this.getDeclaringClass().isKotlin()</code>
     *                  to this parameter.
     *
     * @see CtClass#isKotlin()
     * @see #getDeclaringClass()
     * @since 3.26
     */
    public void insertAfter(String src, boolean asFinally, boolean redundant)
        throws CannotCompileException
    {
        CtClass cc = declaringClass;
        cc.checkModify();
        ConstPool pool = methodInfo.getConstPool();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        if (ca == null)
            throw new CannotCompileException("no method body");

        CodeIterator iterator = ca.iterator();
        int retAddr = ca.getMaxLocals();
        Bytecode b = new Bytecode(pool, 0, retAddr + 1);
        b.setStackDepth(ca.getMaxStack() + 1);
        Javac jv = new Javac(b, cc);
        try {
            int nvars = jv.recordParams(getParameterTypes(),
                                        Modifier.isStatic(getModifiers()));
            jv.recordParamNames(ca, nvars);
            CtClass rtype = getReturnType0();
            int varNo = jv.recordReturnType(rtype, true);
            jv.recordLocalVariables(ca, 0);

            // finally clause for exceptions
            int handlerLen = insertAfterHandler(asFinally, b, rtype, varNo,
                                                jv, src);
            int handlerPos = iterator.getCodeLength();
            if (asFinally)
                ca.getExceptionTable().add(getStartPosOfBody(ca), handlerPos, handlerPos, 0); 

            int adviceLen = 0;
            int advicePos = 0;
            boolean noReturn = true;
            while (iterator.hasNext()) {
                int pos = iterator.next();
                if (pos >= handlerPos)
                    break;

                int c = iterator.byteAt(pos);
                if (c == Opcode.ARETURN || c == Opcode.IRETURN
                    || c == Opcode.FRETURN || c == Opcode.LRETURN
                    || c == Opcode.DRETURN || c == Opcode.RETURN) {
                    if (redundant) {
                        iterator.setMark2(handlerPos);
                        Bytecode bcode;
                        Javac jvc;
                        int retVarNo;
                        if (noReturn) {
                            noReturn = false;
                            bcode = b;
                            jvc = jv;
                            retVarNo = varNo;
                        }
                        else {
                            bcode = new Bytecode(pool, 0, retAddr + 1);
                            bcode.setStackDepth(ca.getMaxStack() + 1);
                            jvc = new Javac(bcode, cc);
                            int nvars2 = jvc.recordParams(getParameterTypes(),
                                                          Modifier.isStatic(getModifiers()));
                            jvc.recordParamNames(ca, nvars2);
                            retVarNo = jvc.recordReturnType(rtype, true);
                            jvc.recordLocalVariables(ca, 0);
                        }

                        int adviceLen2 = insertAfterAdvice(bcode, jvc, src, pool, rtype, retVarNo);
                        int offset = iterator.append(bcode.get());
                        iterator.append(bcode.getExceptionTable(), offset);
                        int advicePos2 = iterator.getCodeLength() - adviceLen2;
                        insertGoto(iterator, advicePos2, pos);
                        handlerPos = iterator.getMark2();
                    }
                    else {
                        if (noReturn) {
                            // finally clause for normal termination
                            adviceLen = insertAfterAdvice(b, jv, src, pool, rtype, varNo);
                            handlerPos = iterator.append(b.get());
                            iterator.append(b.getExceptionTable(), handlerPos);
                            advicePos = iterator.getCodeLength() - adviceLen;
                            handlerLen = advicePos - handlerPos;
                            noReturn = false;
                        }

                        insertGoto(iterator, advicePos, pos);
                        advicePos = iterator.getCodeLength() - adviceLen;
                        handlerPos = advicePos - handlerLen;
                    }
                }
            }

            if (noReturn) {
                handlerPos = iterator.append(b.get());
                iterator.append(b.getExceptionTable(), handlerPos);
            }

            ca.setMaxStack(b.getMaxStack());
            ca.setMaxLocals(b.getMaxLocals());
            methodInfo.rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    private int insertAfterAdvice(Bytecode code, Javac jv, String src,
                                  ConstPool cp, CtClass rtype, int varNo)
        throws CompileError
    {
        int pc = code.currentPc();
        if (rtype == CtClass.voidType) {
            code.addOpcode(Opcode.ACONST_NULL);
            code.addAstore(varNo);
            jv.compileStmnt(src);
            code.addOpcode(Opcode.RETURN);
            if (code.getMaxLocals() < 1)
                code.setMaxLocals(1);
        }
        else {
            code.addStore(varNo, rtype);
            jv.compileStmnt(src);
            code.addLoad(varNo, rtype);
            if (rtype.isPrimitive())
                code.addOpcode(((CtPrimitiveType)rtype).getReturnOp());
            else
                code.addOpcode(Opcode.ARETURN);
        }

        return code.currentPc() - pc;
    }

    /*
     * assert subr > pos
     */
    private void insertGoto(CodeIterator iterator, int subr, int pos)
        throws BadBytecode
    {
        iterator.setMark(subr);
        // the gap length might be a multiple of 4.
        iterator.writeByte(Opcode.NOP, pos);
        boolean wide = subr + 2 - pos > Short.MAX_VALUE;
        int len = wide ? 4 : 2;
        CodeIterator.Gap gap = iterator.insertGapAt(pos, len, false);
        pos = gap.position + gap.length - len;
        int offset = iterator.getMark() - pos;
        if (wide) {
            iterator.writeByte(Opcode.GOTO_W, pos);
            iterator.write32bit(offset, pos + 1);
        }
        else if (offset <= Short.MAX_VALUE) {
            iterator.writeByte(Opcode.GOTO, pos);
            iterator.write16bit(offset, pos + 1);
        }
        else {
            if (gap.length < 4) {
                CodeIterator.Gap gap2 =  iterator.insertGapAt(gap.position, 2, false);
                pos = gap2.position + gap2.length + gap.length - 4; 
            }

            iterator.writeByte(Opcode.GOTO_W, pos);
            iterator.write32bit(iterator.getMark() - pos, pos + 1);
        }
    }

    /* insert a finally clause
     */
    private int insertAfterHandler(boolean asFinally, Bytecode b,
                                   CtClass rtype, int returnVarNo,
                                   Javac javac, String src)
        throws CompileError
    {
        if (!asFinally)
            return 0;

        int var = b.getMaxLocals();
        b.incMaxLocals(1);
        int pc = b.currentPc();
        b.addAstore(var);   // store an exception
        if (rtype.isPrimitive()) {
            char c = ((CtPrimitiveType)rtype).getDescriptor();
            if (c == 'D') {
                b.addDconst(0.0);
                b.addDstore(returnVarNo);
            }
            else if (c == 'F') {
                b.addFconst(0);
                b.addFstore(returnVarNo);
            }
            else if (c == 'J') {
                b.addLconst(0);
                b.addLstore(returnVarNo);
            }
            else if (c == 'V') {
                b.addOpcode(Opcode.ACONST_NULL);
                b.addAstore(returnVarNo);
            }
            else { // int, boolean, char, short, ...
                b.addIconst(0);
                b.addIstore(returnVarNo);
            }
        }
        else {
            b.addOpcode(Opcode.ACONST_NULL);
            b.addAstore(returnVarNo);
        }

        javac.compileStmnt(src);
        b.addAload(var);
        b.addOpcode(Opcode.ATHROW);
        return b.currentPc() - pc;
    }

    /* -- OLD version --

    public void insertAfter(String src) throws CannotCompileException {
        declaringClass.checkModify();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator iterator = ca.iterator();
        Bytecode b = new Bytecode(methodInfo.getConstPool(),
                                  ca.getMaxStack(), ca.getMaxLocals());
        b.setStackDepth(ca.getMaxStack());
        Javac jv = new Javac(b, declaringClass);
        try {
            jv.recordParams(getParameterTypes(),
                            Modifier.isStatic(getModifiers()));
            CtClass rtype = getReturnType0();
            int varNo = jv.recordReturnType(rtype, true);
            boolean isVoid = rtype == CtClass.voidType;
            if (isVoid) {
                b.addOpcode(Opcode.ACONST_NULL);
                b.addAstore(varNo);
                jv.compileStmnt(src);
            }
            else {
                b.addStore(varNo, rtype);
                jv.compileStmnt(src);
                b.addLoad(varNo, rtype);
            }

            byte[] code = b.get();
            ca.setMaxStack(b.getMaxStack());
            ca.setMaxLocals(b.getMaxLocals());
            while (iterator.hasNext()) {
                int pos = iterator.next();
                int c = iterator.byteAt(pos);
                if (c == Opcode.ARETURN || c == Opcode.IRETURN
                    || c == Opcode.FRETURN || c == Opcode.LRETURN
                    || c == Opcode.DRETURN || c == Opcode.RETURN)
                    iterator.insert(pos, code);
            }
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }
    */

    /**
     * Adds a catch clause that handles an exception thrown in the
     * body.  The catch clause must end with a return or throw statement.
     *
     * @param src       the source code representing the catch clause.
     *                  It must be a single statement or block.
     * @param exceptionType     the type of the exception handled by the
     *                          catch clause.
     */
    public void addCatch(String src, CtClass exceptionType)
        throws CannotCompileException
    {
        addCatch(src, exceptionType, "$e");
    }

    /**
     * Adds a catch clause that handles an exception thrown in the
     * body.  The catch clause must end with a return or throw statement.
     *
     * @param src       the source code representing the catch clause.
     *                  It must be a single statement or block.
     * @param exceptionType     the type of the exception handled by the
     *                          catch clause.
     * @param exceptionName     the name of the variable containing the
     *                          caught exception, for example,
     *                          <code>$e</code>.
     */
    public void addCatch(String src, CtClass exceptionType,
                         String exceptionName)
        throws CannotCompileException
    {
        CtClass cc = declaringClass;
        cc.checkModify();
        ConstPool cp = methodInfo.getConstPool();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator iterator = ca.iterator();
        Bytecode b = new Bytecode(cp, ca.getMaxStack(), ca.getMaxLocals());
        b.setStackDepth(1);
        Javac jv = new Javac(b, cc);
        try {
            jv.recordParams(getParameterTypes(),
                            Modifier.isStatic(getModifiers()));
            int var = jv.recordVariable(exceptionType, exceptionName);
            b.addAstore(var);
            jv.compileStmnt(src);

            int stack = b.getMaxStack();
            int locals = b.getMaxLocals();

            if (stack > ca.getMaxStack())
                ca.setMaxStack(stack);

            if (locals > ca.getMaxLocals())
                ca.setMaxLocals(locals);

            int len = iterator.getCodeLength();
            int pos = iterator.append(b.get());
            ca.getExceptionTable().add(getStartPosOfBody(ca), len, len,
                                       cp.addClassInfo(exceptionType));
            iterator.append(b.getExceptionTable(), pos);
            methodInfo.rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        } catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    /* CtConstructor overrides this method.
     */
    int getStartPosOfBody(CodeAttribute ca) throws CannotCompileException {
        return 0;
    }

    /**
     * Inserts bytecode at the specified line in the body.
     * It is equivalent to:
     *
     * <br><code>insertAt(lineNum, true, src)</code>
     *
     * <br>See this method as well.
     *
     * @param lineNum   the line number.  The bytecode is inserted at the
     *                  beginning of the code at the line specified by this
     *                  line number.
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     * @return      the line number at which the bytecode has been inserted.
     *
     * @see CtBehavior#insertAt(int,boolean,String)
     */
    public int insertAt(int lineNum, String src)
        throws CannotCompileException
    {
        return insertAt(lineNum, true, src);
    }

    /**
     * Inserts bytecode at the specified line in the body.
     *
     * <p>If there is not
     * a statement at the specified line, the bytecode might be inserted
     * at the line including the first statement after that line specified.
     * For example, if there is only a closing brace at that line, the
     * bytecode would be inserted at another line below.
     * To know exactly where the bytecode will be inserted, call with
     * <code>modify</code> set to <code>false</code>. 
     *
     * @param lineNum   the line number.  The bytecode is inserted at the
     *                  beginning of the code at the line specified by this
     *                  line number.
     * @param modify    if false, this method does not insert the bytecode.
     *                  It instead only returns the line number at which
     *                  the bytecode would be inserted.
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     *                  If modify is false, the value of src can be null.
     * @return      the line number at which the bytecode has been inserted.
     */
    public int insertAt(int lineNum, boolean modify, String src)
        throws CannotCompileException
    {
        CodeAttribute ca = methodInfo.getCodeAttribute();
        if (ca == null)
            throw new CannotCompileException("no method body");

        LineNumberAttribute ainfo
            = (LineNumberAttribute)ca.getAttribute(LineNumberAttribute.tag);
        if (ainfo == null)
            throw new CannotCompileException("no line number info");

        LineNumberAttribute.Pc pc = ainfo.toNearPc(lineNum);
        lineNum = pc.line;
        int index = pc.index;
        if (!modify)
            return lineNum;

        CtClass cc = declaringClass;
        cc.checkModify();
        CodeIterator iterator = ca.iterator();
        Javac jv = new Javac(cc);
        try {
            jv.recordLocalVariables(ca, index);
            jv.recordParams(getParameterTypes(),
                            Modifier.isStatic(getModifiers()));
            jv.setMaxLocals(ca.getMaxLocals());
            jv.compileStmnt(src);
            Bytecode b = jv.getBytecode();
            int locals = b.getMaxLocals();
            int stack = b.getMaxStack();
            ca.setMaxLocals(locals);

            /* We assume that there is no values in the operand stack
             * at the position where the bytecode is inserted.
             */
            if (stack > ca.getMaxStack())
                ca.setMaxStack(stack);

            index = iterator.insertAt(index, b.get());
            iterator.insert(b.getExceptionTable(), index);
            methodInfo.rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());
            return lineNum;
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }
}
