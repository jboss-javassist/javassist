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

package javassist;

import java.io.*;
import javassist.bytecode.*;
import java.util.Collection;
import javassist.expr.ExprEditor;
import java.net.URL;

// Subclasses of CtClass: CtClassType, CtPrimitiveType, and CtArray

/**
 * An instance of <code>CtClass</code> represents a class.
 * It is obtained from <code>ClassPool</code>.
 *
 * @see ClassPool#get(String)
 */
public abstract class CtClass {
    protected String qualifiedName;

    /**
     * The version number of this release.
     */
    public static final String version = "3.0 RC1";

    /**
     * Prints the version number and the copyright notice.
     *
     * <p>The following command invokes this method:
     *
     * <ul><pre>java -jar javassist.jar</pre></ul>
     */
    public static void main(String[] args) {
        System.out.println("Javassist version " + CtClass.version);
        System.out.println("Copyright (C) 1999-2004 Shigeru Chiba."
                           + " All Rights Reserved.");
    }

    static final String javaLangObject = "java.lang.Object";

    /**
     * The <code>CtClass</code> object representing
     * the <code>boolean</code> type.
     */
    public static CtClass booleanType;

    /**
     * The <code>CtClass</code> object representing
     * the <code>char</code> type.
     */
    public static CtClass charType;

    /**
     * The <code>CtClass</code> object representing
     * the <code>byte</code> type.
     */
    public static CtClass byteType;

    /**
     * The <code>CtClass</code> object representing
     * the <code>short</code> type.
     */
    public static CtClass shortType;

    /**
     * The <code>CtClass</code> object representing
     * the <code>int</code> type.
     */
    public static CtClass intType;

    /**
     * The <code>CtClass</code> object representing
     * the <code>long</code> type.
     */
    public static CtClass longType;

    /**
     * The <code>CtClass</code> object representing
     * the <code>float</code> type.
     */
    public static CtClass floatType;

    /**
     * The <code>CtClass</code> object representing
     * the <code>double</code> type.
     */
    public static CtClass doubleType;

    /**
     * The <code>CtClass</code> object representing
     * the <code>void</code> type.
     */
    public static CtClass voidType;

    static CtClass[] primitiveTypes;

    static {
        primitiveTypes = new CtClass[9];

        booleanType = new CtPrimitiveType("boolean", 'Z', "java.lang.Boolean",
                                "booleanValue", "()Z", Opcode.IRETURN,
                                Opcode.T_BOOLEAN, 1);
        primitiveTypes[0] = booleanType;

        charType = new CtPrimitiveType("char", 'C', "java.lang.Character",
                                "charValue", "()C", Opcode.IRETURN,
                                Opcode.T_CHAR, 1);
        primitiveTypes[1] = charType;

        byteType = new CtPrimitiveType("byte", 'B', "java.lang.Byte",
                                "byteValue", "()B", Opcode.IRETURN,
                                Opcode.T_BYTE, 1);
        primitiveTypes[2] = byteType;

        shortType = new CtPrimitiveType("short", 'S', "java.lang.Short",
                                "shortValue", "()S", Opcode.IRETURN,
                                Opcode.T_SHORT, 1);
        primitiveTypes[3] = shortType;

        intType = new CtPrimitiveType("int", 'I', "java.lang.Integer",
                                "intValue", "()I", Opcode.IRETURN,
                                Opcode.T_INT, 1);
        primitiveTypes[4] = intType;

        longType = new CtPrimitiveType("long", 'J', "java.lang.Long",
                                "longValue", "()J", Opcode.LRETURN,
                                Opcode.T_LONG, 2);
        primitiveTypes[5] = longType;

        floatType = new CtPrimitiveType("float", 'F', "java.lang.Float",
                                "floatValue", "()F", Opcode.FRETURN,
                                Opcode.T_FLOAT, 1);
        primitiveTypes[6] = floatType;

        doubleType = new CtPrimitiveType("double", 'D', "java.lang.Double",
                                "doubleValue", "()D", Opcode.DRETURN,
                                Opcode.T_DOUBLE, 2);
        primitiveTypes[7] = doubleType;

        voidType = new CtPrimitiveType("void", 'V', "java.lang.Void",
                                null, null, Opcode.RETURN, 0, 0);
        primitiveTypes[8] = voidType;
    }

    protected CtClass(String name) {
        qualifiedName = name;
    }

    /**
     * Converts the object to a string.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(getClass().getName());
        buf.append("@");
        buf.append(Integer.toHexString(hashCode()));
        buf.append("[");
        buf.append(Modifier.toString(getModifiers()));
        buf.append(' ');
        buf.append(getName());
        extendToString(buf);
        buf.append("]");
        return buf.toString();
    }    

    /**
     * Implemented in subclasses to add to the {@link #toString()} result.
     * Subclasses should put a space before each token added to the buffer.
     */
    abstract protected void extendToString(StringBuffer buffer);

    /**
     * Returns a <code>ClassPool</code> for this class.
     */
    public ClassPool getClassPool() { return null; }

    /**
     * Returns a class file for this class.
     *
     * <p>This method is not available if <code>isFrozen()</code>
     * is true.
     */
    public ClassFile getClassFile() {
        checkModify();
        return getClassFile2();
    }

    /**
     * Undocumented method.  Do not use; internal-use only.
     */
    public ClassFile getClassFile2() { return null; }

    /**
     * Undocumented method.  Do not use; internal-use only.
     */
    public javassist.compiler.AccessorMaker getAccessorMaker() {
        return null;
    }

    /**
     * Returns the uniform resource locator (URL) of the class file.
     */
    public URL getURL() throws NotFoundException {
        throw new NotFoundException(getName());
    }

    /**
     * Returns true if the definition of the class has been modified.
     */
    public boolean isModified() { return false; }

    /**
     * Returns true if the class has been loaded or written out
     * and thus it cannot be modified any more.
     *
     * @see #defrost()
     */
    public boolean isFrozen() { return true; }

    void freeze() {}

    void checkModify() throws RuntimeException {
        if (isFrozen())
            throw new RuntimeException(getName() + " class is frozen");

        // isModified() must return true after this method is invoked.
    }

    /**
     * Defrosts the class so that the class can be modified again.
     *
     * To avoid changes that will be never reflected,
     * the class is frozen to be unmodifiable if it is loaded or
     * written out.  This method should be called only in a case
     * that the class will be reloaded or written out later again.
     *
     * @see #isFrozen()
     */
    public void defrost() {
        throw new RuntimeException("cannot defrost " + getName());
    }

    /**
     * Returns <code>true</code> if this object represents a primitive
     * Java type: boolean, byte, char, short, int, long, float, double,
     * or void.
     */
    public boolean isPrimitive() { return false; }

    /**
     * Returns <code>true</code> if this object represents an array type.
     */
    public boolean isArray() {
        return false;
    }

    /**
     * If this object represents an array, this method returns the component
     * type of the array.  Otherwise, it returns <code>null</code>.
     */
    public CtClass getComponentType() throws NotFoundException {
        return null;
    }

    /**
     * Returns <code>true</code> if this class extends or implements
     * <code>clazz</code>.  It also returns <code>true</code> if
     * this class is the same as <code>clazz</code>.
     */
    public boolean subtypeOf(CtClass clazz) throws NotFoundException {
        return this == clazz || getName().equals(clazz.getName());
    }

    /**
     * Obtains the fully-qualified name of the class.
     */
    public String getName() { return qualifiedName; }

    /**
     * Obtains the not-qualified class name.
     */
    public final String getSimpleName() {
        String qname = qualifiedName;
        int index = qname.lastIndexOf('.');
        if (index < 0)
            return qname;
        else
            return qname.substring(index + 1);
    }

    /**
     * Obtains the package name.  It may be <code>null</code>.
     */
    public final String getPackageName() {
        String qname = qualifiedName;
        int index = qname.lastIndexOf('.');
        if (index < 0)
            return null;
        else
            return qname.substring(0, index);
    }

    /**
     * Sets the class name
     *
     * @param name      fully-qualified name
     */
    public void setName(String name) {
        checkModify();
        if (name != null)
            qualifiedName = name;
    }

    /**
     * Substitutes <code>newName</code> for all occurrences of a class
     * name <code>oldName</code> in the class file.
     *
     * @param oldName           replaced class name
     * @param newName           substituted class name
     */
    public void replaceClassName(String oldName, String newName) {
        checkModify();
    }

    /**
     * Changes class names appearing in the class file according to the
     * given <code>map</code>.
     *
     * <p>All the class names appearing in the class file are tested
     * with <code>map</code> to determine whether each class name is
     * replaced or not.  Thus this method can be used for collecting
     * all the class names in the class file.  To do that, first define
     * a subclass of <code>ClassMap</code> so that <code>get()</code>
     * records all the given parameters.  Then, make an instance of
     * that subclass as an empty hash-table.  Finally, pass that instance
     * to this method.  After this method finishes, that instance would
     * contain all the class names appearing in the class file.
     *
     * @param map       the hashtable associating replaced class names
     *                  with substituted names.
     */
    public void replaceClassName(ClassMap map) {
        checkModify();
    }

    /**
     * Returns a collection of the names of all the classes
     * referenced in this class.
     * That collection includes the name of this class.
     *
     * <p>This method may return <code>null</code>.
     */
    public Collection getRefClasses() {
        ClassFile cf = getClassFile2();
        if (cf != null) {
            ClassMap cm = new ClassMap() {
                    public void put(String oldname, String newname) {
                        put0(oldname, newname);
                    }

                    public Object get(Object jvmClassName) {
                        String n = toJavaName((String)jvmClassName);
                        put0(n, n);
                        return null;
                    }

                    public void fix(String name) {}
                };
            cf.renameClass(cm);
            return cm.values();
        }
        else
            return null;
    }

    /**
     * Determines whether this object represents a class or an interface.
     * It returns <code>true</code> if this object represents an interface.
     */
    public boolean isInterface() {
        return false;
    }

    /**
     * Returns the modifiers for this class, encoded in an integer.
     * For decoding, use <code>javassist.Modifier</code>.
     *
     * @see Modifier
     */
    public int getModifiers() {
        return 0;
    }

    /**
     * Sets the modifiers.
     *
     * @param mod       modifiers encoded by
     *                  <code>javassist.Modifier</code>
     * @see Modifier
     */
    public void setModifiers(int mod) {
        checkModify();
    }

    /**
     * Determines whether the class directly or indirectly extends
     * the given class.  If this class extends a class A and
     * the class A extends a class B, then subclassof(B) returns true.
     *
     * <p>This method returns true if the given class is identical to
     * the class represented by this object.
     */
    public boolean subclassOf(CtClass superclass) {
        return false;
    }

    /**
     * Obtains the class object representing the superclass of the
     * class.
     * It returns null if this object represents the
     * <code>java.lang.Object</code> class and thus it does not have
     * the super class.
     *
     * <p>If this object represents an interface, this method
     * always returns the <code>java.lang.Object</code> class.
     * To obtain the super interfaces
     * extended by that interface, call <code>getInterfaces()</code>.
     */
    public CtClass getSuperclass() throws NotFoundException {
        return null;
    }

    /**
     * Changes a super class unless this object represents an interface.
     * The new super class must be compatible with the old one.
     *
     * <p>If this object represents an interface, this method is equivalent
     * to <code>addInterface()</code>; it appends <code>clazz</code> to
     * the list of the super interfaces extended by that interface.
     * Note that an interface can extend multiple super interfaces.
     */
    public void setSuperclass(CtClass clazz) throws CannotCompileException {
        checkModify();
    }

    /**
     * Obtains the class objects representing the interfaces implemented
     * by the class or, if this object represents an interface, the interfaces
     * extended by that interface.
     */
    public CtClass[] getInterfaces() throws NotFoundException {
        return new CtClass[0];
    }

    /**
     * Sets implemented interfaces.  If this object represents an interface,
     * this method sets the interfaces extended by that interface.
     *
     * @param list              a list of the <code>CtClass</code> objects
     *                          representing interfaces, or
     *                          <code>null</code> if the class implements
     *                          no interfaces.
     */
    public void setInterfaces(CtClass[] list) {
        checkModify();
    }

    /**
     * Adds an interface.
     *
     * @param anInterface       the added interface.
     */
    public void addInterface(CtClass anInterface) {
        checkModify();
    }

    /**
     * If this class is a member class or interface of another class,
     * then the class enclosing this class is returned.
     *
     * @return null if this class is a top-level class.
     */
    public CtClass getDeclaringClass() throws NotFoundException {
        return null;
    }

    /**
     * Returns an array containing <code>CtField</code> objects
     * representing all the public fields of the class.
     * That array includes public fields inherited from the
     * superclasses.
     */
    public CtField[] getFields() { return new CtField[0]; }

    /**
     * Returns the field with the specified name.  The returned field
     * may be a private field declared in a super class or interface.
     */
    public CtField getField(String name) throws NotFoundException {
        throw new NotFoundException(name);
    }

    /**
     * @return null     if the specified field is not found.
     */
    CtField getField2(String name) { return null; }

    /**
     * Gets all the fields declared in the class.  The inherited fields
     * are not included.
     *
     * <p>Note: the result does not include inherited fields.
     */
    public CtField[] getDeclaredFields() { return new CtField[0]; }

    /**
     * Retrieves the field with the specified name among the fields
     * declared in the class.
     *
     * <p>Note: this method does not search the superclasses.
     */
    public CtField getDeclaredField(String name) throws NotFoundException {
        throw new NotFoundException(name);
    }

    /**
     * Gets all the constructors and methods declared in the class.
     */
    public CtBehavior[] getDeclaredBehaviors() {
        return new CtBehavior[0];
    }

    /**
     * Returns an array containing <code>CtConstructor</code> objects
     * representing all the public constructors of the class.
     */
    public CtConstructor[] getConstructors() {
        return new CtConstructor[0];
    }

    /**
     * Returns the constructor with the given signature,
     * which is represented by a character string
     * called method descriptor.
     * For details of the method descriptor, see the JVM specification
     * or <code>javassist.bytecode.Descriptor</code>.
     *
     * @param desc      method descriptor
     * @see javassist.bytecode.Descriptor
     */
    public CtConstructor getConstructor(String desc)
        throws NotFoundException
    {
        throw new NotFoundException("no such a constructor");
    }

    /**
     * Gets all the constructors declared in the class.
     *
     * @see javassist.CtConstructor
     */
    public CtConstructor[] getDeclaredConstructors() {
        return new CtConstructor[0];
    }

    /**
     * Returns a constructor receiving the specified parameters.
     *
     * @param params    parameter types.
     */
    public CtConstructor getDeclaredConstructor(CtClass[] params)
        throws NotFoundException
    {
        String desc = Descriptor.ofConstructor(params);
        return getConstructor(desc);
    }

    /**
     * Gets the class initializer (static constructor)
     * declared in the class.
     * This method returns <code>null</code> if
     * no class initializer is not declared.
     *
     * @see #makeClassInitializer()
     * @see javassist.CtConstructor
     */
    public CtConstructor getClassInitializer() {
        return null;
    }

    /**
     * Returns an array containing <code>CtMethod</code> objects
     * representing all the public methods of the class.
     * That array includes public methods inherited from the
     * superclasses.
     */
    public CtMethod[] getMethods() {
        return new CtMethod[0];
    }

    /**
     * Returns the method with the given name and signature.
     * The returned method may be declared in a super class.
     * The method signature is represented by a character string
     * called method descriptor,
     * which is defined in the JVM specification.
     *
     * @param name      method name
     * @param desc      method descriptor
     * @see javassist.bytecode.Descriptor
     */
    public CtMethod getMethod(String name, String desc)
        throws NotFoundException
    {
        throw new NotFoundException(name);
    }

    /**
     * Gets all methods declared in the class.  The inherited methods
     * are not included.
     *
     * @see javassist.CtMethod
     */
    public CtMethod[] getDeclaredMethods() {
        return new CtMethod[0];
    }

    /**
     * Retrieves the method with the specified name and parameter types
     * among the methods declared in the class.
     *
     * <p>Note: this method does not search the superclasses.
     *
     * @param name              method name
     * @param params            parameter types
     * @see javassist.CtMethod
     */
    public CtMethod getDeclaredMethod(String name, CtClass[] params)
        throws NotFoundException
    {
        throw new NotFoundException(name);
    }

    /**
     * Retrieves the method with the specified name among the methods
     * declared in the class.  If there are multiple methods with
     * the specified name, then this method returns one of them.
     *
     * <p>Note: this method does not search the superclasses.
     *
     * @see javassist.CtMethod
     */
    public CtMethod getDeclaredMethod(String name) throws NotFoundException {
        throw new NotFoundException(name);
    }

    /**
     * Makes an empty class initializer (static constructor).
     * If the class already includes a class initializer,
     * this method returns it.
     *
     * @see #getClassInitializer()
     */
    public CtConstructor makeClassInitializer()
        throws CannotCompileException
    {
        throw new CannotCompileException("not a class");
    }

    /**
     * Adds a constructor.  To add a class initializer (static constructor),
     * call <code>makeClassInitializer()</code>.
     *
     * @see #makeClassInitializer()
     */
    public void addConstructor(CtConstructor c)
        throws CannotCompileException
    {
        checkModify();
    }

    /**
     * Adds a method.
     */
    public void addMethod(CtMethod m) throws CannotCompileException {
        checkModify();
    }

    /**
     * Adds a field.
     *
     * <p>The <code>CtField</code> belonging to another
     * <code>CtClass</code> cannot be directly added to this class.
     * Only a field created for this class can be added.
     *
     * @see javassist.CtField#CtField(CtField,CtClass)
     */
    public void addField(CtField f) throws CannotCompileException {
        addField(f, (CtField.Initializer)null);
    }

    /**
     * Adds a field with an initial value.
     *
     * <p>The <code>CtField</code> belonging to another
     * <code>CtClass</code> cannot be directly added to this class.
     * Only a field created for this class can be added.
     *
     * <p>The initial value is given as an expression written in Java.
     * Any regular Java expression can be used for specifying the initial
     * value.  The followings are examples.
     *
     * <ul><pre>
     * cc.addField(f, "0")               // the initial value is 0.
     * cc.addField(f, "i + 1")           // i + 1.
     * cc.addField(f, "new Point()");    // a Point object.
     * </pre></ul>
     *
     * <p>Here, the type of variable <code>cc</code> is <code>CtClass</code>.
     * The type of <code>f</code> is <code>CtField</code>.
     *
     * @param init      an expression for the initial value.
     *
     * @see javassist.CtField.Initializer#byExpr(String)
     * @see javassist.CtField#CtField(CtField,CtClass)
     */
    public void addField(CtField f, String init)
        throws CannotCompileException
    {
        checkModify();
    }

    /**
     * Adds a field with an initial value.
     *
     * <p>The <code>CtField</code> belonging to another
     * <code>CtClass</code> cannot be directly added to this class.
     * Only a field created for this class can be added.
     *
     * <p>For example,
     *
     * <ul><pre>
     * CtClass cc = ...;
     * addField(new CtField(CtClass.intType, "i", cc),
     *          CtField.Initializer.constant(1));
     * </pre></ul>
     *
     * <p>This code adds an <code>int</code> field named "i".  The
     * initial value of this field is 1.
     *
     * @param init      specifies the initial value of the field.
     *
     * @see javassist.CtField#CtField(CtField,CtClass)
     */
    public void addField(CtField f, CtField.Initializer init)
        throws CannotCompileException
    {
        checkModify();
    }

    /**
     * Obtains an attribute with the given name.
     * If that attribute is not found in the class file, this
     * method returns null.
     *
     * @param name              attribute name
     */
    public byte[] getAttribute(String name) {
        return null;
    }

    /**
     * Adds a named attribute.
     * An arbitrary data (smaller than 64Kb) can be saved in the class
     * file.  Some attribute name are reserved by the JVM.
     * The attributes with the non-reserved names are ignored when a
     * class file is loaded into the JVM.
     * If there is already an attribute with
     * the same name, this method substitutes the new one for it.
     *
     * @param name      attribute name
     * @param data      attribute value
     */
    public void setAttribute(String name, byte[] data) {
        checkModify();
    }

    /**
     * Applies the given converter to all methods and constructors
     * declared in the class.  This method calls <code>instrument()</code>
     * on every <code>CtMethod</code> and <code>CtConstructor</code> object
     * in the class.
     *
     * @param converter         specifies how to modify.
     */
    public void instrument(CodeConverter converter)
        throws CannotCompileException
    {
        checkModify();
    }

    /**
     * Modifies the bodies of all methods and constructors
     * declared in the class.  This method calls <code>instrument()</code>
     * on every <code>CtMethod</code> and <code>CtConstructor</code> object
     * in the class.
     *
     * @param editor            specifies how to modify.
     */
    public void instrument(ExprEditor editor)
        throws CannotCompileException
    {
        checkModify();
    }

    /**
     * Converts this class to a <code>java.lang.Class</code> object.
     * Once this method is called, further modifications are not
     * possible any more.
     *
     * <p>This method is provided for convenience.  If you need more
     * complex functionality, you should write your own class loader.
     *
     * <p>To load a class file, this method uses an internal class loader,
     * which is an instance of <code>ClassPool.SimpleLoader</code>.
     * Thus, that class file is not loaded by the system class loader,
     * which should have loaded this <code>CtClass</code> class.
     * The internal class loader
     * loads only the classes explicitly specified by this method
     * <code>toClass()</code>.  The other classes are loaded
     * by the parent class loader (usually the sytem class loader)
     * by delegation.
     *
     * <p>For example,
     *
     * <ul><pre>class Line { Point p1, p2; }</pre></ul>
     *
     * <p>If the class <code>Line</code> is loaded by the internal class
     * loader and the class <code>Point</code> has not been loaded yet,
     * then the class <code>Point</code> that the class <code>Line</code>
     * refers to is loaded by the parent class loader.  There is no
     * chance of modifying the definition of <code>Point</code> with
     * Javassist.
     *
     * <p>The internal class loader is shared among all the instances
     * of <code>ClassPool</code>.
     *
     * @return the <code>Class</code> object representing the loaded class.  
     * @see CtClass#forName(String)
     * @see ClassPool.SimpleLoader
     * @see Loader
     */
    public Class toClass()
        throws NotFoundException, IOException, CannotCompileException
    {
        return getClassPool().toClass(this);
    }

    /**
     * Returns a <code>java.lang.Class</code> object that has been loaded
     * by <code>toClass()</code>.  Such an object cannot be
     * obtained by <code>java.lang.Class.forName()</code> because it has
     * been loaded by an internal class loader of Javassist.
     *
     * @see CtClass#toClass()
     * @see ClassPool.SimpleLoader
     */
    public Class forName(String name) throws ClassNotFoundException {
        return getClassPool().forName(name);
    }

    /**
     * Removes this <code>CtClass</code> from the <code>ClassPool</code>.
     *
     * <p>If needed,
     * the <code>ClassPool</code> will read the class file again
     * and constructs another <code>CtClass</code> object representing
     * the same class.
     */
    public void detach() {
        ClassPool cp = getClassPool();
        CtClass obj = cp.removeCached(getName());
        if (obj != this)
            cp.cacheCtClass(getName(), obj);
    }

    /**
     * Converts this class to a class file.
     * Once this method is called, further modifications are not
     * possible any more.
     *
     * @return the contents of the class file.
     */
    public byte[] toBytecode()
        throws NotFoundException, IOException, CannotCompileException
    {
        ByteArrayOutputStream barray = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(barray);
        try {
            toBytecode(out);
        }
        finally {
            out.close();
        }

        return barray.toByteArray();
    }

    /**
     * Writes a class file represented by this <code>CtClass</code>
     * object in the current directory.
     * Once this method is called, further modifications are not
     * possible any more.
     */
    public void writeFile()
        throws NotFoundException, IOException, CannotCompileException
    {
        writeFile(".");
    }

    /**
     * Writes a class file represented by this <code>CtClass</code>
     * object on a local disk.
     * Once this method is called, further modifications are not
     * possible any more.
     *
     * @param directoryName     it must end without a directory separator.
     */
    public void writeFile(String directoryName)
        throws NotFoundException, CannotCompileException, IOException
    {
        String classname = getName();
        String filename = directoryName + File.separatorChar
            + classname.replace('.', File.separatorChar) + ".class";
        int pos = filename.lastIndexOf(File.separatorChar);
        if (pos > 0) {
            String dir = filename.substring(0, pos);
            if (!dir.equals("."))
                new File(dir).mkdirs();
        }

        DataOutputStream out
            = new DataOutputStream(new BufferedOutputStream(
                                new DelayedFileOutputStream(filename)));
        try {
            toBytecode(out);
        }
        finally {
            out.close();
        }
    }

    static class DelayedFileOutputStream extends OutputStream {
        private FileOutputStream file;
        private String filename;

        DelayedFileOutputStream(String name) {
            file = null;
            filename = name;
        }

        private void init() throws IOException {
            if (file == null)
                file = new FileOutputStream(filename);
        }

        public void write(int b) throws IOException {
            init();
            file.write(b);
        }

        public void write(byte[] b) throws IOException {
            init();
            file.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            init();
            file.write(b, off, len);

        }

        public void flush() throws IOException {
            init();
            file.flush();
        }

        public void close() throws IOException {
            init();
            file.close();
        }
    }

    /**
     * Converts this class to a class file.
     * Once this method is called, further modifications are not
     * possible any more.
     *
     * <p>This method dose not close the output stream in the end.
     *
     * @param out       the output stream that a class file is written to.
     */
    public void toBytecode(DataOutputStream out)
        throws CannotCompileException, IOException
    {
        throw new CannotCompileException("not a class");
    }
}
