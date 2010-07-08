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
import javassist.compiler.Javac;
import javassist.compiler.SymbolTable;
import javassist.compiler.CompileError;
import javassist.compiler.ast.ASTree;
import javassist.compiler.ast.IntConst;
import javassist.compiler.ast.DoubleConst;
import javassist.compiler.ast.StringL;

/**
 * An instance of CtField represents a field.
 *
 * @see CtClass#getDeclaredFields()
 */
public class CtField extends CtMember {
    static final String javaLangString = "java.lang.String";

    protected FieldInfo fieldInfo;

    /**
     * Creates a <code>CtField</code> object.
     * The created field must be added to a class
     * with <code>CtClass.addField()</code>.
     * An initial value of the field is specified
     * by a <code>CtField.Initializer</code> object.
     *
     * <p>If getter and setter methods are needed,
     * call <code>CtNewMethod.getter()</code> and 
     * <code>CtNewMethod.setter()</code>.
     *
     * @param type              field type
     * @param name              field name
     * @param declaring         the class to which the field will be added.
     *
     * @see CtClass#addField(CtField)
     * @see CtNewMethod#getter(String,CtField)
     * @see CtNewMethod#setter(String,CtField)
     * @see CtField.Initializer
     */
    public CtField(CtClass type, String name, CtClass declaring)
        throws CannotCompileException
    {
        this(Descriptor.of(type), name, declaring);
    }

    /**
     * Creates a copy of the given field.
     * The created field must be added to a class
     * with <code>CtClass.addField()</code>.
     * An initial value of the field is specified
     * by a <code>CtField.Initializer</code> object.
     *
     * <p>If getter and setter methods are needed,
     * call <code>CtNewMethod.getter()</code> and 
     * <code>CtNewMethod.setter()</code>.
     *
     * @param src               the original field
     * @param declaring         the class to which the field will be added.
     * @see CtNewMethod#getter(String,CtField)
     * @see CtNewMethod#setter(String,CtField)
     * @see CtField.Initializer
     */
    public CtField(CtField src, CtClass declaring)
        throws CannotCompileException
    {
        this(src.fieldInfo.getDescriptor(), src.fieldInfo.getName(),
             declaring);
        java.util.ListIterator iterator
            = src.fieldInfo.getAttributes().listIterator();
        FieldInfo fi = fieldInfo;
        fi.setAccessFlags(src.fieldInfo.getAccessFlags());
        ConstPool cp = fi.getConstPool();
        while (iterator.hasNext()) {
            AttributeInfo ainfo = (AttributeInfo)iterator.next();
            fi.addAttribute(ainfo.copy(cp, null));
        }
    }

    private CtField(String typeDesc, String name, CtClass clazz)
        throws CannotCompileException
    {
        super(clazz);
        ClassFile cf = clazz.getClassFile2();
        if (cf == null)
            throw new CannotCompileException("bad declaring class: "
                                             + clazz.getName());

        fieldInfo = new FieldInfo(cf.getConstPool(), name, typeDesc);
    }

    CtField(FieldInfo fi, CtClass clazz) {
        super(clazz);
        fieldInfo = fi;
    }

    /**
     * Returns a String representation of the object.
     */
    public String toString() {
        return getDeclaringClass().getName() + "." + getName()
               + ":" + fieldInfo.getDescriptor();
    }

    protected void extendToString(StringBuffer buffer) {
        buffer.append(' ');
        buffer.append(getName());
        buffer.append(' ');
        buffer.append(fieldInfo.getDescriptor());
    }

    /* Javac.CtFieldWithInit overrides.
     */
    protected ASTree getInitAST() { return null; }

    /* Called by CtClassType.addField().
     */
    Initializer getInit() {
        ASTree tree = getInitAST();
        if (tree == null)
            return null;
        else
            return Initializer.byExpr(tree);
    }

    /**
     * Compiles the given source code and creates a field.
     * Examples of the source code are:
     *
     * <ul><pre>
     * "public String name;"
     * "public int k = 3;"</pre></ul>
     *
     * <p>Note that the source code ends with <code>';'</code>
     * (semicolon).
     *
     * @param src               the source text.
     * @param declaring    the class to which the created field is added.
     */
    public static CtField make(String src, CtClass declaring)
        throws CannotCompileException
    {
        Javac compiler = new Javac(declaring);
        try {
            CtMember obj = compiler.compile(src);
            if (obj instanceof CtField)
                return (CtField)obj; // an instance of Javac.CtFieldWithInit
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }

        throw new CannotCompileException("not a field");
    }

    /**
     * Returns the FieldInfo representing the field in the class file.
     */
    public FieldInfo getFieldInfo() {
        declaringClass.checkModify();
        return fieldInfo;
    }

    /**
     * Returns the FieldInfo representing the field in the class
     * file (read only).
     * Normal applications do not need calling this method.  Use
     * <code>getFieldInfo()</code>.
     *
     * <p>The <code>FieldInfo</code> object obtained by this method
     * is read only.  Changes to this object might not be reflected
     * on a class file generated by <code>toBytecode()</code>,
     * <code>toClass()</code>, etc in <code>CtClass</code>.
     *
     * <p>This method is available even if the <code>CtClass</code>
     * containing this field is frozen.  However, if the class is
     * frozen, the <code>FieldInfo</code> might be also pruned.
     *
     * @see #getFieldInfo()
     * @see CtClass#isFrozen()
     * @see CtClass#prune()
     */
    public FieldInfo getFieldInfo2() { return fieldInfo; }

    /**
     * Returns the class declaring the field.
     */
    public CtClass getDeclaringClass() {
        // this is redundant but for javadoc.
        return super.getDeclaringClass();
    }

    /**
     * Returns the name of the field.
     */
    public String getName() {
        return fieldInfo.getName();
    }

    /**
     * Changes the name of the field.
     */
    public void setName(String newName) {
        declaringClass.checkModify();
        fieldInfo.setName(newName);
    }

    /**
     * Returns the encoded modifiers of the field.
     *
     * @see Modifier
     */
    public int getModifiers() {
        return AccessFlag.toModifier(fieldInfo.getAccessFlags());
    }

    /**
     * Sets the encoded modifiers of the field.
     *
     * @see Modifier
     */
    public void setModifiers(int mod) {
        declaringClass.checkModify();
        fieldInfo.setAccessFlags(AccessFlag.of(mod));
    }

    /**
     * Returns true if the class has the specified annotation class.
     *
     * @param clz the annotation class.
     * @return <code>true</code> if the annotation is found, otherwise <code>false</code>.
     * @since 3.11
     */
    public boolean hasAnnotation(Class clz) {
        FieldInfo fi = getFieldInfo2();
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
                    fi.getAttribute(AnnotationsAttribute.invisibleTag);  
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                    fi.getAttribute(AnnotationsAttribute.visibleTag);  
        return CtClassType.hasAnnotationType(clz, getDeclaringClass().getClassPool(),
                                             ainfo, ainfo2);
    }

    /**
     * Returns the annotation if the class has the specified annotation class.
     * For example, if an annotation <code>@Author</code> is associated
     * with this field, an <code>Author</code> object is returned.
     * The member values can be obtained by calling methods on
     * the <code>Author</code> object.
     *
     * @param clz the annotation class.
     * @return the annotation if found, otherwise <code>null</code>.
     * @since 3.11
     */
    public Object getAnnotation(Class clz) throws ClassNotFoundException {
        FieldInfo fi = getFieldInfo2();
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
                    fi.getAttribute(AnnotationsAttribute.invisibleTag);  
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                    fi.getAttribute(AnnotationsAttribute.visibleTag);  
        return CtClassType.getAnnotationType(clz, getDeclaringClass().getClassPool(),
                                             ainfo, ainfo2);
    }

    /**
     * Returns the annotations associated with this field.
     *
     * @return an array of annotation-type objects.
     * @see #getAvailableAnnotations()
     * @since 3.1
     */
    public Object[] getAnnotations() throws ClassNotFoundException {
        return getAnnotations(false);
    }

    /**
     * Returns the annotations associated with this field.
     * If any annotations are not on the classpath, they are not included
     * in the returned array.
     *
     * @return an array of annotation-type objects.
     * @see #getAnnotations()
     * @since 3.3
     */
    public Object[] getAvailableAnnotations(){
        try {
            return getAnnotations(true);
        }
        catch (ClassNotFoundException e) {
           throw new RuntimeException("Unexpected exception", e);
        }
    }

    private Object[] getAnnotations(boolean ignoreNotFound) throws ClassNotFoundException {
        FieldInfo fi = getFieldInfo2();
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
                    fi.getAttribute(AnnotationsAttribute.invisibleTag);  
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                    fi.getAttribute(AnnotationsAttribute.visibleTag);  
        return CtClassType.toAnnotationType(ignoreNotFound, getDeclaringClass().getClassPool(),
                                            ainfo, ainfo2);
    }

    /**
     * Returns the character string representing the type of the field.
     * The field signature is represented by a character string
     * called a field descriptor, which is defined in the JVM specification.
     * If two fields have the same type,
     * <code>getSignature()</code> returns the same string.
     *
     * <p>Note that the returned string is not the type signature
     * contained in the <code>SignatureAttirbute</code>.  It is
     * a descriptor.  To obtain a type signature, call the following
     * methods:
     * 
     * <ul><pre>getFieldInfo().getAttribute(SignatureAttribute.tag)
     * </pre></ul>
     *
     * @see javassist.bytecode.Descriptor
     * @see javassist.bytecode.SignatureAttribute
     */
    public String getSignature() {
        return fieldInfo.getDescriptor();
    }

    /**
     * Returns the type of the field.
     */
    public CtClass getType() throws NotFoundException {
        return Descriptor.toCtClass(fieldInfo.getDescriptor(),
                                    declaringClass.getClassPool());
    }

    /**
     * Sets the type of the field.
     */
    public void setType(CtClass clazz) {
        declaringClass.checkModify();
        fieldInfo.setDescriptor(Descriptor.of(clazz));
    }

    /**
     * Returns the value of this field if it is a constant field.
     * This method works only if the field type is a primitive type
     * or <code>String</code> type.  Otherwise, it returns <code>null</code>.
     * A constant field is <code>static</code> and <code>final</code>.
     *
     * @return  a <code>Integer</code>, <code>Long</code>, <code>Float</code>,
     *          <code>Double</code>, <code>Boolean</code>,
     *          or <code>String</code> object
     *          representing the constant value. 
     *          <code>null</code> if it is not a constant field
     *          or if the field type is not a primitive type
     *          or <code>String</code>.
     */
    public Object getConstantValue() {
        // When this method is modified,
        // see also getConstantFieldValue() in TypeChecker.

        int index = fieldInfo.getConstantValue();
        if (index == 0)
            return null;

        ConstPool cp = fieldInfo.getConstPool();
        switch (cp.getTag(index)) {
            case ConstPool.CONST_Long :
                return new Long(cp.getLongInfo(index));
            case ConstPool.CONST_Float :
                return new Float(cp.getFloatInfo(index));
            case ConstPool.CONST_Double :
                return new Double(cp.getDoubleInfo(index));
            case ConstPool.CONST_Integer :
                int value = cp.getIntegerInfo(index);
                // "Z" means boolean type.
                if ("Z".equals(fieldInfo.getDescriptor()))
                    return new Boolean(value != 0);
                else
                    return new Integer(value);
            case ConstPool.CONST_String :
                return cp.getStringInfo(index);
            default :
                throw new RuntimeException("bad tag: " + cp.getTag(index)
                                           + " at " + index);
        }
    }

    /**
     * Obtains an attribute with the given name.
     * If that attribute is not found in the class file, this
     * method returns null.
     *
     * <p>Note that an attribute is a data block specified by
     * the class file format.
     * See {@link javassist.bytecode.AttributeInfo}.
     *
     * @param name              attribute name
     */
    public byte[] getAttribute(String name) {
        AttributeInfo ai = fieldInfo.getAttribute(name);
        if (ai == null)
            return null;
        else
            return ai.get();
    }

    /**
     * Adds an attribute. The attribute is saved in the class file.
     *
     * <p>Note that an attribute is a data block specified by
     * the class file format.
     * See {@link javassist.bytecode.AttributeInfo}.
     *
     * @param name      attribute name
     * @param data      attribute value
     */
    public void setAttribute(String name, byte[] data) {
        declaringClass.checkModify();
        fieldInfo.addAttribute(new AttributeInfo(fieldInfo.getConstPool(),
                                                 name, data));
    }

    // inner classes

    /**
     * Instances of this class specify how to initialize a field.
     * <code>Initializer</code> is passed to
     * <code>CtClass.addField()</code> with a <code>CtField</code>.
     *
     * <p>This class cannot be instantiated with the <code>new</code> operator.
     * Factory methods such as <code>byParameter()</code> and
     * <code>byNew</code>
     * must be used for the instantiation.  They create a new instance with
     * the given parameters and return it.
     *
     * @see CtClass#addField(CtField,CtField.Initializer)
     */
    public static abstract class Initializer {
        /**
         * Makes an initializer that assigns a constant integer value.
         * The field must be integer, short, char, or byte type.
         */
        public static Initializer constant(int i) {
            return new IntInitializer(i);
        }

        /**
         * Makes an initializer that assigns a constant boolean value.
         * The field must be boolean type.
         */
        public static Initializer constant(boolean b) {
            return new IntInitializer(b ? 1 : 0);
        }

        /**
         * Makes an initializer that assigns a constant long value.
         * The field must be long type.
         */
        public static Initializer constant(long l) {
            return new LongInitializer(l);
        }

        /**
         * Makes an initializer that assigns a constant float value.
         * The field must be float type.
         */
        public static Initializer constant(float l) {
            return new FloatInitializer(l);
        }

        /**
         * Makes an initializer that assigns a constant double value.
         * The field must be double type.
         */
        public static Initializer constant(double d) {
            return new DoubleInitializer(d);
        }

        /**
         * Makes an initializer that assigns a constant string value.
         * The field must be <code>java.lang.String</code> type.
         */
        public static Initializer constant(String s) {
            return new StringInitializer(s);
        }

        /**
         * Makes an initializer using a constructor parameter.
         *
         * <p>The initial value is the
         * N-th parameter given to the constructor of the object including
         * the field.  If the constructor takes less than N parameters,
         * the field is not initialized.
         * If the field is static, it is never initialized.
         *
         * @param nth           the n-th (&gt;= 0) parameter is used as
         *                      the initial value.
         *                      If nth is 0, then the first parameter is
         *                      used.
         */
        public static Initializer byParameter(int nth) {
            ParamInitializer i = new ParamInitializer();
            i.nthParam = nth;
            return i;
        }

        /**
         * Makes an initializer creating a new object.
         *
         * <p>This initializer creates a new object and uses it as the initial
         * value of the field.  The constructor of the created object receives
         * the parameter:
         *
         * <ul><code>Object obj</code> - the object including the field.<br>
         * </ul>
         *
         * <p>If the initialized field is static, then the constructor does
         * not receive any parameters.
         *
         * @param objectType    the class instantiated for the initial value.
         */
        public static Initializer byNew(CtClass objectType) {
            NewInitializer i = new NewInitializer();
            i.objectType = objectType;
            i.stringParams = null;
            i.withConstructorParams = false;
            return i;
        }

        /**
         * Makes an initializer creating a new object.
         *
         * <p>This initializer creates a new object and uses it as the initial
         * value of the field.  The constructor of the created object receives
         * the parameters:
         *
         * <ul><code>Object obj</code> - the object including the field.<br>
         *     <code>String[] strs</code> - the character strings specified
         *                              by <code>stringParams</code><br>
         * </ul>
         *
         * <p>If the initialized field is static, then the constructor
         * receives only <code>strs</code>.
         *
         * @param objectType    the class instantiated for the initial value.
         * @param stringParams  the array of strings passed to the
         *                      constructor.
         */
        public static Initializer byNew(CtClass objectType,
                                             String[] stringParams) {
            NewInitializer i = new NewInitializer();
            i.objectType = objectType;
            i.stringParams = stringParams;
            i.withConstructorParams = false;
            return i;
        }

        /**
         * Makes an initializer creating a new object.
         *
         * <p>This initializer creates a new object and uses it as the initial
         * value of the field.  The constructor of the created object receives
         * the parameters:
         *
         * <ul><code>Object obj</code> - the object including the field.<br>
         *     <code>Object[] args</code> - the parameters passed to the
         *                      constructor of the object including the
         *                      filed.
         * </ul>
         *
         * <p>If the initialized field is static, then the constructor does
         * not receive any parameters.
         *
         * @param objectType    the class instantiated for the initial value.
         *
         * @see javassist.CtField.Initializer#byNewArray(CtClass,int)
         * @see javassist.CtField.Initializer#byNewArray(CtClass,int[])
         */
        public static Initializer byNewWithParams(CtClass objectType) {
            NewInitializer i = new NewInitializer();
            i.objectType = objectType;
            i.stringParams = null;
            i.withConstructorParams = true;
            return i;
        }

        /**
         * Makes an initializer creating a new object.
         *
         * <p>This initializer creates a new object and uses it as the initial
         * value of the field.  The constructor of the created object receives
         * the parameters:
         *
         * <ul><code>Object obj</code> - the object including the field.<br>
         *     <code>String[] strs</code> - the character strings specified
         *                              by <code>stringParams</code><br>
         *     <code>Object[] args</code> - the parameters passed to the
         *                      constructor of the object including the
         *                      filed.
         * </ul>
         *
         * <p>If the initialized field is static, then the constructor receives
         * only <code>strs</code>.
         *
         * @param objectType    the class instantiated for the initial value.
         * @param stringParams  the array of strings passed to the
         *                              constructor.
         */
        public static Initializer byNewWithParams(CtClass objectType,
                                               String[] stringParams) {
            NewInitializer i = new NewInitializer();
            i.objectType = objectType;
            i.stringParams = stringParams;
            i.withConstructorParams = true;
            return i;
        }

        /**
         * Makes an initializer calling a static method.
         *
         * <p>This initializer calls a static method and uses the returned
         * value as the initial value of the field.
         * The called method receives the parameters:
         *
         * <ul><code>Object obj</code> - the object including the field.<br>
         * </ul>
         *
         * <p>If the initialized field is static, then the method does
         * not receive any parameters.
         *
         * <p>The type of the returned value must be the same as the field
         * type.
         *
         * @param methodClass   the class that the static method is
         *                              declared in.
         * @param methodName    the name of the satic method.
         */
        public static Initializer byCall(CtClass methodClass,
                                              String methodName) {
            MethodInitializer i = new MethodInitializer();
            i.objectType = methodClass;
            i.methodName = methodName;
            i.stringParams = null;
            i.withConstructorParams = false;
            return i;
        }

        /**
         * Makes an initializer calling a static method.
         *
         * <p>This initializer calls a static method and uses the returned
         * value as the initial value of the field.  The called method
         * receives the parameters:
         *
         * <ul><code>Object obj</code> - the object including the field.<br>
         *     <code>String[] strs</code> - the character strings specified
         *                              by <code>stringParams</code><br>
         * </ul>
         *
         * <p>If the initialized field is static, then the method
         * receive only <code>strs</code>.
         *
         * <p>The type of the returned value must be the same as the field
         * type.
         *
         * @param methodClass   the class that the static method is
         *                              declared in.
         * @param methodName    the name of the satic method.
         * @param stringParams  the array of strings passed to the
         *                              static method.
         */
        public static Initializer byCall(CtClass methodClass,
                                              String methodName,
                                              String[] stringParams) {
            MethodInitializer i = new MethodInitializer();
            i.objectType = methodClass;
            i.methodName = methodName;
            i.stringParams = stringParams;
            i.withConstructorParams = false;
            return i;
        }

        /**
         * Makes an initializer calling a static method.
         *
         * <p>This initializer calls a static method and uses the returned
         * value as the initial value of the field.  The called method
         * receives the parameters:
         *
         * <ul><code>Object obj</code> - the object including the field.<br>
         *     <code>Object[] args</code> - the parameters passed to the
         *                      constructor of the object including the
         *                      filed.
         * </ul>
         *
         * <p>If the initialized field is static, then the method does
         * not receive any parameters.
         *
         * <p>The type of the returned value must be the same as the field
         * type.
         *
         * @param methodClass   the class that the static method is
         *                              declared in.
         * @param methodName    the name of the satic method.
         */
        public static Initializer byCallWithParams(CtClass methodClass,
                                                        String methodName) {
            MethodInitializer i = new MethodInitializer();
            i.objectType = methodClass;
            i.methodName = methodName;
            i.stringParams = null;
            i.withConstructorParams = true;
            return i;
        }

        /**
         * Makes an initializer calling a static method.
         *
         * <p>This initializer calls a static method and uses the returned
         * value as the initial value of the field.  The called method
         * receives the parameters:
         *
         * <ul><code>Object obj</code> - the object including the field.<br>
         *     <code>String[] strs</code> - the character strings specified
         *                              by <code>stringParams</code><br>
         *     <code>Object[] args</code> - the parameters passed to the
         *                      constructor of the object including the
         *                      filed.
         * </ul>
         *
         * <p>If the initialized field is static, then the method
         * receive only <code>strs</code>.
         *
         * <p>The type of the returned value must be the same as the field
         * type.
         *
         * @param methodClass   the class that the static method is
         *                              declared in.
         * @param methodName    the name of the satic method.
         * @param stringParams  the array of strings passed to the
         *                              static method.
         */
        public static Initializer byCallWithParams(CtClass methodClass,
                                String methodName, String[] stringParams) {
            MethodInitializer i = new MethodInitializer();
            i.objectType = methodClass;
            i.methodName = methodName;
            i.stringParams = stringParams;
            i.withConstructorParams = true;
            return i;
        }

        /**
         * Makes an initializer creating a new array.
         *
         * @param type  the type of the array.
         * @param size  the size of the array.
         * @throws NotFoundException    if the type of the array components
         *                              is not found.
         */
        public static Initializer byNewArray(CtClass type, int size) 
            throws NotFoundException
        {
            return new ArrayInitializer(type.getComponentType(), size);
        }

        /**
         * Makes an initializer creating a new multi-dimensional array.
         *
         * @param type  the type of the array.
         * @param sizes an <code>int</code> array of the size in every
         *                      dimension.
         *                      The first element is the size in the first
         *                      dimension.  The second is in the second, etc.
         */
        public static Initializer byNewArray(CtClass type, int[] sizes) {
            return new MultiArrayInitializer(type, sizes);
        }

        /**
         * Makes an initializer.
         *
         * @param source        initializer expression.
         */
        public static Initializer byExpr(String source) {
            return new CodeInitializer(source);
        }

        static Initializer byExpr(ASTree source) {
            return new PtreeInitializer(source);
        }

        // Check whether this initializer is valid for the field type.
        // If it is invaild, this method throws an exception.
        void check(String desc) throws CannotCompileException {}

        // produce codes for initialization
        abstract int compile(CtClass type, String name, Bytecode code,
                             CtClass[] parameters, Javac drv)
            throws CannotCompileException;

        // produce codes for initialization
        abstract int compileIfStatic(CtClass type, String name,
                Bytecode code, Javac drv) throws CannotCompileException;

        // returns the index of CONSTANT_Integer_info etc
        // if the value is constant.  Otherwise, 0.
        int getConstantValue(ConstPool cp, CtClass type) { return 0; }
    }

    static abstract class CodeInitializer0 extends Initializer {
        abstract void compileExpr(Javac drv) throws CompileError;

        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            try {
                code.addAload(0);
                compileExpr(drv);
                code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
                return code.getMaxStack();
            }
            catch (CompileError e) {
                throw new CannotCompileException(e);
            }
        }

        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            try {
                compileExpr(drv);
                code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
                return code.getMaxStack();
            }
            catch (CompileError e) {
                throw new CannotCompileException(e);
            }
        }

        int getConstantValue2(ConstPool cp, CtClass type, ASTree tree) {
            if (type.isPrimitive()) {
                if (tree instanceof IntConst) {
                    long value = ((IntConst)tree).get();
                    if (type == CtClass.doubleType)
                        return cp.addDoubleInfo((double)value);
                    else if (type == CtClass.floatType)
                        return cp.addFloatInfo((float)value);
                    else if (type == CtClass.longType)
                        return cp.addLongInfo(value);
                    else  if (type != CtClass.voidType)
                        return cp.addIntegerInfo((int)value);
                }
                else if (tree instanceof DoubleConst) {
                    double value = ((DoubleConst)tree).get();
                    if (type == CtClass.floatType)
                        return cp.addFloatInfo((float)value);
                    else if (type == CtClass.doubleType)
                        return cp.addDoubleInfo(value);
                }
            }
            else if (tree instanceof StringL
                     && type.getName().equals(javaLangString))
                return cp.addStringInfo(((StringL)tree).get());

            return 0;
        }
    }

    static class CodeInitializer extends CodeInitializer0 {
        private String expression;

        CodeInitializer(String expr) { expression = expr; }

        void compileExpr(Javac drv) throws CompileError {
            drv.compileExpr(expression);
        }

        int getConstantValue(ConstPool cp, CtClass type) {
            try {
                ASTree t = Javac.parseExpr(expression, new SymbolTable());
                return getConstantValue2(cp, type, t);
            }
            catch (CompileError e) {
                return 0;
            }
        }
    }

    static class PtreeInitializer extends CodeInitializer0 {
        private ASTree expression;

        PtreeInitializer(ASTree expr) { expression = expr; }

        void compileExpr(Javac drv) throws CompileError {
            drv.compileExpr(expression);
        }

        int getConstantValue(ConstPool cp, CtClass type) {
            return getConstantValue2(cp, type, expression);
        }
    }

    /**
     * A field initialized with a parameter passed to the constructor
     * of the class containing that field.
     */
    static class ParamInitializer extends Initializer {
        int nthParam;

        ParamInitializer() {}

        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            if (parameters != null && nthParam < parameters.length) {
                code.addAload(0);
                int nth = nthParamToLocal(nthParam, parameters, false);
                int s = code.addLoad(nth, type) + 1;
                code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
                return s;       // stack size
            }
            else
                return 0;       // do not initialize
        }

        /**
         * Computes the index of the local variable that the n-th parameter
         * is assigned to.
         *
         * @param nth           n-th parameter
         * @param params                list of parameter types
         * @param isStatic              true if the method is static.
         */
        static int nthParamToLocal(int nth, CtClass[] params,
                                   boolean isStatic) {
            CtClass longType = CtClass.longType;
            CtClass doubleType = CtClass.doubleType;
            int k;
            if (isStatic)
                k = 0;
            else
                k = 1;  // 0 is THIS.

            for (int i = 0; i < nth; ++i) {
                CtClass type = params[i];
                if (type == longType || type == doubleType)
                    k += 2;
                else
                    ++k;
            }

            return k;
        }

        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            return 0;
        }
    }

    /**
     * A field initialized with an object created by the new operator.
     */
    static class NewInitializer extends Initializer {
        CtClass objectType;
        String[] stringParams;
        boolean withConstructorParams;

        NewInitializer() {}

        /**
         * Produces codes in which a new object is created and assigned to
         * the field as the initial value.
         */
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            int stacksize;

            code.addAload(0);
            code.addNew(objectType);
            code.add(Bytecode.DUP);
            code.addAload(0);

            if (stringParams == null)
                stacksize = 4;
            else
                stacksize = compileStringParameter(code) + 4;

            if (withConstructorParams)
                stacksize += CtNewWrappedMethod.compileParameterList(code,
                                                            parameters, 1);

            code.addInvokespecial(objectType, "<init>", getDescriptor());
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return stacksize;
        }

        private String getDescriptor() {
            final String desc3
        = "(Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/Object;)V";

            if (stringParams == null)
                if (withConstructorParams)
                    return "(Ljava/lang/Object;[Ljava/lang/Object;)V";
                else
                    return "(Ljava/lang/Object;)V";
            else
                if (withConstructorParams)
                    return desc3;
                else
                    return "(Ljava/lang/Object;[Ljava/lang/String;)V";
        }

        /**
         * Produces codes for a static field.
         */
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            String desc;

            code.addNew(objectType);
            code.add(Bytecode.DUP);

            int stacksize = 2;
            if (stringParams == null)
                desc = "()V";
            else {
                desc = "([Ljava/lang/String;)V";
                stacksize += compileStringParameter(code);
            }

            code.addInvokespecial(objectType, "<init>", desc);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return stacksize;
        }

        protected final int compileStringParameter(Bytecode code)
            throws CannotCompileException
        {
            int nparam = stringParams.length;
            code.addIconst(nparam);
            code.addAnewarray(javaLangString);
            for (int j = 0; j < nparam; ++j) {
                code.add(Bytecode.DUP);         // dup
                code.addIconst(j);                      // iconst_<j>
                code.addLdc(stringParams[j]);   // ldc ...
                code.add(Bytecode.AASTORE);             // aastore
            }

            return 4;
        }

    }

    /**
     * A field initialized with the result of a static method call.
     */
    static class MethodInitializer extends NewInitializer {
        String methodName;
        // the method class is specified by objectType.

        MethodInitializer() {}

        /**
         * Produces codes in which a new object is created and assigned to
         * the field as the initial value.
         */
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            int stacksize;

            code.addAload(0);
            code.addAload(0);

            if (stringParams == null)
                stacksize = 2;
            else
                stacksize = compileStringParameter(code) + 2;

            if (withConstructorParams)
                stacksize += CtNewWrappedMethod.compileParameterList(code,
                                                            parameters, 1);

            String typeDesc = Descriptor.of(type);
            String mDesc = getDescriptor() + typeDesc;
            code.addInvokestatic(objectType, methodName, mDesc);
            code.addPutfield(Bytecode.THIS, name, typeDesc);
            return stacksize;
        }

        private String getDescriptor() {
            final String desc3
                = "(Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/Object;)";

            if (stringParams == null)
                if (withConstructorParams)
                    return "(Ljava/lang/Object;[Ljava/lang/Object;)";
                else
                    return "(Ljava/lang/Object;)";
            else
                if (withConstructorParams)
                    return desc3;
                else
                    return "(Ljava/lang/Object;[Ljava/lang/String;)";
        }

        /**
         * Produces codes for a static field.
         */
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            String desc;

            int stacksize = 1;
            if (stringParams == null)
                desc = "()";
            else {
                desc = "([Ljava/lang/String;)";
                stacksize += compileStringParameter(code);
            }

            String typeDesc = Descriptor.of(type);
            code.addInvokestatic(objectType, methodName, desc + typeDesc);
            code.addPutstatic(Bytecode.THIS, name, typeDesc);
            return stacksize;
        }
    }

    static class IntInitializer extends Initializer {
        int value;

        IntInitializer(int v) { value = v; }

        void check(String desc) throws CannotCompileException {
            char c = desc.charAt(0);
            if (c != 'I' && c != 'S' && c != 'B' && c != 'C' && c != 'Z')
                throw new CannotCompileException("type mismatch");
        }

        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            code.addIconst(value);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return 2;   // stack size
        }

        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            code.addIconst(value);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return 1;   // stack size
        }

        int getConstantValue(ConstPool cp, CtClass type) {
            return cp.addIntegerInfo(value);
        }
    }

    static class LongInitializer extends Initializer {
        long value;

        LongInitializer(long v) { value = v; }

        void check(String desc) throws CannotCompileException {
            if (!desc.equals("J"))
                throw new CannotCompileException("type mismatch");
        }

        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            code.addLdc2w(value);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return 3;   // stack size
        }

        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            code.addLdc2w(value);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return 2;   // stack size
        }

        int getConstantValue(ConstPool cp, CtClass type) {
            if (type == CtClass.longType)
                return cp.addLongInfo(value);
            else
                return 0;
        }
    }

    static class FloatInitializer extends Initializer {
        float value;

        FloatInitializer(float v) { value = v; }

        void check(String desc) throws CannotCompileException {
            if (!desc.equals("F"))
                throw new CannotCompileException("type mismatch");
        }

        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            code.addFconst(value);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return 3;   // stack size
        }

        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            code.addFconst(value);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return 2;   // stack size
        }

        int getConstantValue(ConstPool cp, CtClass type) {
            if (type == CtClass.floatType)
                return cp.addFloatInfo(value);
            else
                return 0;
        }
    }

    static class DoubleInitializer extends Initializer {
        double value;

        DoubleInitializer(double v) { value = v; }

        void check(String desc) throws CannotCompileException {
            if (!desc.equals("D"))
                throw new CannotCompileException("type mismatch");
        }

        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            code.addLdc2w(value);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return 3;   // stack size
        }

        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            code.addLdc2w(value);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return 2;   // stack size
        }

        int getConstantValue(ConstPool cp, CtClass type) {
            if (type == CtClass.doubleType)
                return cp.addDoubleInfo(value);
            else
                return 0;
        }
    }

    static class StringInitializer extends Initializer {
        String value;

        StringInitializer(String v) { value = v; }

        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            code.addLdc(value);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return 2;   // stack size
        }

        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            code.addLdc(value);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return 1;   // stack size
        }

        int getConstantValue(ConstPool cp, CtClass type) {
            if (type.getName().equals(javaLangString))
                return cp.addStringInfo(value);
            else
                return 0;
        }
    }

    static class ArrayInitializer extends Initializer {
        CtClass type;
        int size;

        ArrayInitializer(CtClass t, int s) { type = t; size = s; }

        private void addNewarray(Bytecode code) {
            if (type.isPrimitive())
                code.addNewarray(((CtPrimitiveType)type).getArrayType(),
                                 size);
            else
                code.addAnewarray(type, size);
        }

        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            addNewarray(code);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return 2;   // stack size
        }

        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            addNewarray(code);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return 1;   // stack size
        }
    }

    static class MultiArrayInitializer extends Initializer {
        CtClass type;
        int[] dim;

        MultiArrayInitializer(CtClass t, int[] d) { type = t; dim = d; }

        void check(String desc) throws CannotCompileException {
            if (desc.charAt(0) != '[')
                throw new CannotCompileException("type mismatch");
        }

        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            int s = code.addMultiNewarray(type, dim);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return s + 1;       // stack size
        }

        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            int s = code.addMultiNewarray(type, dim);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return s;   // stack size
        }
    }
}
