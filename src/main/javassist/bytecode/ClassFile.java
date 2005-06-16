/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2005 Shigeru Chiba. All Rights Reserved.
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

package javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javassist.CannotCompileException;

/**
 * <code>ClassFile</code> represents a Java <code>.class</code> file, which
 * consists of a constant pool, methods, fields, and attributes.
 * 
 * @see javassist.CtClass#getClassFile()
 */
public final class ClassFile {
    int major, minor; // version number
    ConstPool constPool;
    int thisClass;
    int accessFlags;
    int superClass;
    int[] interfaces;
    ArrayList fields;
    ArrayList methods;
    LinkedList attributes;
    String thisclassname; // not JVM-internal name
    String[] cachedInterfaces;
    String cachedSuperclass;

    /**
     * Constructs a class file from a byte stream.
     */
    public ClassFile(DataInputStream in) throws IOException {
        read(in);
    }

    /**
     * Constructs a class file including no members.
     * 
     * @param isInterface
     *            true if this is an interface. false if this is a class.
     * @param classname
     *            a fully-qualified class name
     * @param superclass
     *            a fully-qualified super class name
     */
    public ClassFile(boolean isInterface, String classname, String superclass) {
        major = 45;
        minor = 3; // JDK 1.1 or later
        constPool = new ConstPool(classname);
        thisClass = constPool.getThisClassInfo();
        if (isInterface)
            accessFlags = AccessFlag.SUPER | AccessFlag.INTERFACE
                    | AccessFlag.ABSTRACT;
        else
            accessFlags = AccessFlag.SUPER;

        initSuperclass(superclass);
        interfaces = null;
        fields = new ArrayList();
        methods = new ArrayList();
        thisclassname = classname;

        attributes = new LinkedList();
        attributes.add(new SourceFileAttribute(constPool,
                getSourcefileName(thisclassname)));
    }

    private void initSuperclass(String superclass) {
        if (superclass != null) {
            this.superClass = constPool.addClassInfo(superclass);
            cachedSuperclass = superclass;
        }
        else {
            this.superClass = constPool.addClassInfo("java.lang.Object");
            cachedSuperclass = "java.lang.Object";
        }
    }

    private static String getSourcefileName(String qname) {
        int index = qname.lastIndexOf('.');
        if (index >= 0)
            qname = qname.substring(index + 1);

        return qname + ".java";
    }

    /**
     * Eliminates dead constant pool items. If a method or a field is removed,
     * the constant pool items used by that method/field become dead items. This
     * method recreates a constant pool.
     */
    public void compact() {
        ConstPool cp = compact0();
        ArrayList list = methods;
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            minfo.compact(cp);
        }

        list = fields;
        n = list.size();
        for (int i = 0; i < n; ++i) {
            FieldInfo finfo = (FieldInfo)list.get(i);
            finfo.compact(cp);
        }

        attributes = AttributeInfo.copyAll(attributes, cp);
        constPool = cp;
    }

    private ConstPool compact0() {
        ConstPool cp = new ConstPool(thisclassname);
        thisClass = cp.getThisClassInfo();
        String sc = getSuperclass();
        if (sc != null)
            superClass = cp.addClassInfo(getSuperclass());

        if (interfaces != null) {
            int n = interfaces.length;
            for (int i = 0; i < n; ++i)
                interfaces[i]
                    = cp.addClassInfo(constPool.getClassInfo(interfaces[i]));
        }

        return cp;
    }

    /**
     * Discards all attributes, associated with both the class file and the
     * members such as a code attribute and exceptions attribute. The unused
     * constant pool entries are also discarded (a new packed constant pool is
     * constructed).
     */
    public void prune() {
        ConstPool cp = compact0();
        LinkedList newAttributes = new LinkedList();
        AttributeInfo invisibleAnnotations
            = getAttribute(AnnotationsAttribute.invisibleTag);
        if (invisibleAnnotations != null) {
            invisibleAnnotations = invisibleAnnotations.copy(cp, null);
            newAttributes.add(invisibleAnnotations);
        }

        AttributeInfo visibleAnnotations
            = getAttribute(AnnotationsAttribute.visibleTag);
        if (visibleAnnotations != null) {
            visibleAnnotations = visibleAnnotations.copy(cp, null);
            newAttributes.add(visibleAnnotations);
        }

        ArrayList list = methods;
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            minfo.prune(cp);
        }

        list = fields;
        n = list.size();
        for (int i = 0; i < n; ++i) {
            FieldInfo finfo = (FieldInfo)list.get(i);
            finfo.prune(cp);
        }

        attributes = newAttributes;
        cp.prune();
        constPool = cp;
    }

    /**
     * Returns a constant pool table.
     */
    public ConstPool getConstPool() {
        return constPool;
    }

    /**
     * Returns true if this is an interface.
     */
    public boolean isInterface() {
        return (accessFlags & AccessFlag.INTERFACE) != 0;
    }

    /**
     * Returns true if this is a final class or interface.
     */
    public boolean isFinal() {
        return (accessFlags & AccessFlag.FINAL) != 0;
    }

    /**
     * Returns true if this is an abstract class or an interface.
     */
    public boolean isAbstract() {
        return (accessFlags & AccessFlag.ABSTRACT) != 0;
    }

    /**
     * Returns access flags.
     * 
     * @see javassist.bytecode.AccessFlag
     */
    public int getAccessFlags() {
        return accessFlags;
    }

    /**
     * Changes access flags.
     * 
     * @see javassist.bytecode.AccessFlag
     */
    public void setAccessFlags(int acc) {
        accessFlags = acc | AccessFlag.SUPER;
    }

    /**
     * Returns the class name.
     */
    public String getName() {
        return thisclassname;
    }

    /**
     * Sets the class name. This method substitutes the new name for all
     * occurrences of the old class name in the class file.
     */
    public void setName(String name) {
        renameClass(thisclassname, name);
    }

    /**
     * Returns the super class name.
     */
    public String getSuperclass() {
        if (cachedSuperclass == null)
            cachedSuperclass = constPool.getClassInfo(superClass);

        return cachedSuperclass;
    }

    /**
     * Returns the index of the constant pool entry representing the super
     * class.
     */
    public int getSuperclassId() {
        return superClass;
    }

    /**
     * Sets the super class.
     * 
     * <p>
     * This method modifies constructors so that they call constructors declared
     * in the new super class.
     */
    public void setSuperclass(String superclass) throws CannotCompileException {
        if (superclass == null)
            superclass = "java.lang.Object";

        try {
            this.superClass = constPool.addClassInfo(superclass);
            ArrayList list = methods;
            int n = list.size();
            for (int i = 0; i < n; ++i) {
                MethodInfo minfo = (MethodInfo)list.get(i);
                minfo.setSuperclass(superclass);
            }
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
        cachedSuperclass = superclass;
    }

    /**
     * Replaces all occurrences of a class name in the class file.
     * 
     * <p>
     * If class X is substituted for class Y in the class file, X and Y must
     * have the same signature. If Y provides a method m(), X must provide it
     * even if X inherits m() from the super class. If this fact is not
     * guaranteed, the bytecode verifier may cause an error.
     * 
     * @param oldname
     *            the replaced class name
     * @param newname
     *            the substituted class name
     */
    public final void renameClass(String oldname, String newname) {
        ArrayList list;
        int n;

        if (oldname.equals(newname))
            return;

        if (oldname.equals(thisclassname))
            thisclassname = newname;

        oldname = Descriptor.toJvmName(oldname);
        newname = Descriptor.toJvmName(newname);
        constPool.renameClass(oldname, newname);

        list = methods;
        n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            String desc = minfo.getDescriptor();
            minfo.setDescriptor(Descriptor.rename(desc, oldname, newname));
        }

        list = fields;
        n = list.size();
        for (int i = 0; i < n; ++i) {
            FieldInfo finfo = (FieldInfo)list.get(i);
            String desc = finfo.getDescriptor();
            finfo.setDescriptor(Descriptor.rename(desc, oldname, newname));
        }
    }

    /**
     * Replaces all occurrences of several class names in the class file.
     * 
     * @param classnames
     *            specifies which class name is replaced with which new name.
     *            Class names must be described with the JVM-internal
     *            representation like <code>java/lang/Object</code>.
     * @see #renameClass(String,String)
     */
    public final void renameClass(Map classnames) {
        String jvmNewThisName = (String)classnames.get(Descriptor
                .toJvmName(thisclassname));
        if (jvmNewThisName != null)
            thisclassname = Descriptor.toJavaName(jvmNewThisName);

        constPool.renameClass(classnames);

        ArrayList list = methods;
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            String desc = minfo.getDescriptor();
            minfo.setDescriptor(Descriptor.rename(desc, classnames));
        }

        list = fields;
        n = list.size();
        for (int i = 0; i < n; ++i) {
            FieldInfo finfo = (FieldInfo)list.get(i);
            String desc = finfo.getDescriptor();
            finfo.setDescriptor(Descriptor.rename(desc, classnames));
        }
    }

    /**
     * Returns the names of the interfaces implemented by the class.
     * The returned array is read only.
     */
    public String[] getInterfaces() {
        if (cachedInterfaces != null)
            return cachedInterfaces;

        String[] rtn = null;
        if (interfaces == null)
            rtn = new String[0];
        else {
            int n = interfaces.length;
            String[] list = new String[n];
            for (int i = 0; i < n; ++i)
                list[i] = constPool.getClassInfo(interfaces[i]);

            rtn = list;
        }

        cachedInterfaces = rtn;
        return rtn;
    }

    /**
     * Sets the interfaces.
     * 
     * @param nameList
     *            the names of the interfaces.
     */
    public void setInterfaces(String[] nameList) {
        cachedInterfaces = null;
        if (nameList != null) {
            int n = nameList.length;
            interfaces = new int[n];
            for (int i = 0; i < n; ++i)
                interfaces[i] = constPool.addClassInfo(nameList[i]);
        }
    }

    /**
     * Appends an interface to the interfaces implemented by the class.
     */
    public void addInterface(String name) {
        cachedInterfaces = null;
        int info = constPool.addClassInfo(name);
        if (interfaces == null) {
            interfaces = new int[1];
            interfaces[0] = info;
        }
        else {
            int n = interfaces.length;
            int[] newarray = new int[n + 1];
            System.arraycopy(interfaces, 0, newarray, 0, n);
            newarray[n] = info;
            interfaces = newarray;
        }
    }

    /**
     * Returns all the fields declared in the class.
     * 
     * @return a list of <code>FieldInfo</code>.
     * @see FieldInfo
     */
    public List getFields() {
        return fields;
    }

    /**
     * Appends a field to the class.
     */
    public void addField(FieldInfo finfo) throws CannotCompileException {
        testExistingField(finfo.getName(), finfo.getDescriptor());
        fields.add(finfo);
    }

    private void addField0(FieldInfo finfo) {
        fields.add(finfo);
    }

    private void testExistingField(String name, String descriptor)
            throws CannotCompileException {
        ListIterator it = fields.listIterator(0);
        while (it.hasNext()) {
            FieldInfo minfo = (FieldInfo)it.next();
            if (minfo.getName().equals(name))
                throw new CannotCompileException("duplicate field: " + name);
        }
    }

    /**
     * Returns all the methods declared in the class.
     * 
     * @return a list of <code>MethodInfo</code>.
     * @see MethodInfo
     */
    public List getMethods() {
        return methods;
    }

    /**
     * Returns the method with the specified name. If there are multiple methods
     * with that name, this method returns one of them.
     * 
     * @return null if no such a method is found.
     */
    public MethodInfo getMethod(String name) {
        ArrayList list = methods;
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            if (minfo.getName().equals(name))
                return minfo;
        }

        return null;
    }

    /**
     * Returns a static initializer (class initializer), or null if it does not
     * exist.
     */
    public MethodInfo getStaticInitializer() {
        return getMethod(MethodInfo.nameClinit);
    }

    /**
     * Appends a method to the class.
     */
    public void addMethod(MethodInfo minfo) throws CannotCompileException {
        testExistingMethod(minfo.getName(), minfo.getDescriptor());
        methods.add(minfo);
    }

    private void addMethod0(MethodInfo minfo) {
        methods.add(minfo);
    }

    private void testExistingMethod(String name, String descriptor)
            throws CannotCompileException {
        ListIterator it = methods.listIterator(0);
        while (it.hasNext()) {
            MethodInfo minfo = (MethodInfo)it.next();
            if (minfo.getName().equals(name)
                    && Descriptor.eqParamTypes(minfo.getDescriptor(),
                            descriptor))
                throw new CannotCompileException("duplicate method: " + name);
        }
    }

    /**
     * Returns all the attributes.
     * 
     * @return a list of <code>AttributeInfo</code> objects.
     * @see AttributeInfo
     */
    public List getAttributes() {
        return attributes;
    }

    /**
     * Returns the attribute with the specified name.
     * 
     * @param name
     *            attribute name
     */
    public AttributeInfo getAttribute(String name) {
        LinkedList list = attributes;
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            AttributeInfo ai = (AttributeInfo)list.get(i);
            if (ai.getName().equals(name))
                return ai;
        }

        return null;
    }

    /**
     * Appends an attribute. If there is already an attribute with the same
     * name, the new one substitutes for it.
     */
    public void addAttribute(AttributeInfo info) {
        AttributeInfo.remove(attributes, info.getName());
        attributes.add(info);
    }

    /**
     * Returns the source file containing this class.
     * 
     * @return null if this information is not available.
     */
    public String getSourceFile() {
        SourceFileAttribute sf
            = (SourceFileAttribute)getAttribute(SourceFileAttribute.tag);
        if (sf == null)
            return null;
        else
            return sf.getFileName();
    }

    private void read(DataInputStream in) throws IOException {
        int i, n;
        int magic = in.readInt();
        if (magic != 0xCAFEBABE)
            throw new IOException("non class file");

        minor = in.readUnsignedShort();
        major = in.readUnsignedShort();
        constPool = new ConstPool(in);
        accessFlags = in.readUnsignedShort();
        thisClass = in.readUnsignedShort();
        constPool.setThisClassInfo(thisClass);
        superClass = in.readUnsignedShort();
        n = in.readUnsignedShort();
        if (n == 0)
            interfaces = null;
        else {
            interfaces = new int[n];
            for (i = 0; i < n; ++i)
                interfaces[i] = in.readUnsignedShort();
        }

        ConstPool cp = constPool;
        n = in.readUnsignedShort();
        fields = new ArrayList();
        for (i = 0; i < n; ++i)
            addField0(new FieldInfo(cp, in));

        n = in.readUnsignedShort();
        methods = new ArrayList();
        for (i = 0; i < n; ++i)
            addMethod0(new MethodInfo(cp, in));

        attributes = new LinkedList();
        n = in.readUnsignedShort();
        for (i = 0; i < n; ++i)
            addAttribute(AttributeInfo.read(cp, in));

        thisclassname = constPool.getClassInfo(thisClass);
    }

    /**
     * Writes a class file represened by this object into an output stream.
     */
    public void write(DataOutputStream out) throws IOException {
        int i, n;

        out.writeInt(0xCAFEBABE); // magic
        out.writeShort(minor); // minor version
        out.writeShort(major); // major version
        constPool.write(out); // constant pool
        out.writeShort(accessFlags);
        out.writeShort(thisClass);
        out.writeShort(superClass);

        if (interfaces == null)
            n = 0;
        else
            n = interfaces.length;

        out.writeShort(n);
        for (i = 0; i < n; ++i)
            out.writeShort(interfaces[i]);

        ArrayList list = fields;
        n = list.size();
        out.writeShort(n);
        for (i = 0; i < n; ++i) {
            FieldInfo finfo = (FieldInfo)list.get(i);
            finfo.write(out);
        }

        list = methods;
        n = list.size();
        out.writeShort(n);
        for (i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            minfo.write(out);
        }

        out.writeShort(attributes.size());
        AttributeInfo.writeAll(attributes, out);
    }

    /**
     * Get the Major version.
     * 
     * @return the major version
     */
    public int getMajorVersion() {
        return major;
    }

    /**
     * Set the Major version.
     * 
     * @param major
     *            the major version
     */
    public void setMajorVersion(int major) {
        this.major = major;
    }

    /**
     * Get the Minor version.
     * 
     * @return the minor version
     */
    public int getMinorVersion() {
        return minor;
    }

    /**
     * Set the Minor version.
     * 
     * @param minor
     *            the minor version
     */
    public void setMinorVersion(int minor) {
        this.minor = minor;
    }
}
