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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <code>method_info</code> structure.
 * 
 * @see javassist.CtMethod#getMethodInfo()
 * @see javassist.CtConstructor#getMethodInfo()
 */
public final class MethodInfo {
    ConstPool constPool;
    int accessFlags;
    int name;
   String cachedName;
    int descriptor;
    LinkedList attribute; // may be null

    // Bill, do you really need this?
    // public Exception created = new Exception();

    /**
     * The name of constructors: <code>&lt;init&gt</code>.
     */
    public static final String nameInit = "<init>";

    /**
     * The name of class initializer (static initializer):
     * <code>&lt;clinit&gt</code>.
     */
    public static final String nameClinit = "<clinit>";

    private MethodInfo(ConstPool cp) {
        constPool = cp;
        attribute = null;
    }

    /**
     * Constructs a <code>method_info</code> structure. The initial value of
     * <code>access_flags</code> is zero.
     * 
     * @param cp
     *            a constant pool table
     * @param methodname
     *            method name
     * @param desc
     *            method descriptor
     * @see Descriptor
     */
    public MethodInfo(ConstPool cp, String methodname, String desc) {
        this(cp);
        accessFlags = 0;
        name = cp.addUtf8Info(methodname);
        cachedName = methodname;
        descriptor = constPool.addUtf8Info(desc);
    }

    MethodInfo(ConstPool cp, DataInputStream in) throws IOException {
        this(cp);
        read(in);
    }

    /**
     * Constructs a copy of <code>method_info</code> structure. Class names
     * appearing in the source <code>method_info</code> are renamed according
     * to <code>classnameMap</code>.
     * 
     * <p>
     * Note: only <code>Code</code> and <code>Exceptions</code> attributes
     * are copied from the source. The other attributes are ignored.
     * 
     * @param cp
     *            a constant pool table
     * @param methodname
     *            a method name
     * @param src
     *            a source <code>method_info</code>
     * @param classnameMap
     *            specifies pairs of replaced and substituted name.
     * @see Descriptor
     */
    public MethodInfo(ConstPool cp, String methodname, MethodInfo src,
            Map classnameMap) throws BadBytecode {
        this(cp);
        read(src, methodname, classnameMap);
    }

    /**
     * Returns a string representation of the object.
     */
    public String toString() {
        return getName() + " "
                + constPool.getUtf8Info(descriptor);
    }

    /**
     * Copies all constant pool items to a given new constant pool
     * and replaces the original items with the new ones.
     * This is used for garbage collecting the items of removed fields
     * and methods.
     *
     * @param cp    the destination
     */
    void compact(ConstPool cp) {
        name = cp.addUtf8Info(getName());
        descriptor = cp.addUtf8Info(getDescriptor());
        attribute = AttributeInfo.copyAll(attribute, cp);
        constPool = cp;
    }

    void prune(ConstPool cp) {
       AttributeInfo invisibleAnnotations = getAttribute(AnnotationsAttribute.invisibleTag);
       LinkedList newAttributes = new LinkedList();
       if (invisibleAnnotations != null)
       {
          invisibleAnnotations = invisibleAnnotations.copy(cp, null);
          newAttributes.add(invisibleAnnotations);
       }
       AttributeInfo visibleAnnotations = getAttribute(AnnotationsAttribute.visibleTag);
       if (visibleAnnotations != null)
       {
          visibleAnnotations = visibleAnnotations.copy(cp, null);
          newAttributes.add(visibleAnnotations);
       }
       ExceptionsAttribute ea = getExceptionsAttribute();
       if (ea != null) newAttributes.add(ea);
       
        attribute = newAttributes;
        name = cp.addUtf8Info(getName());
        descriptor = cp.addUtf8Info(getDescriptor());
        constPool = cp;
    }

    /**
     * Returns a method name.
     */
    public String getName() {
       if (cachedName == null) cachedName = constPool.getUtf8Info(name);
       return cachedName;
    }

    /**
     * Sets a method name.
     */
    public void setName(String newName) {
        name = constPool.addUtf8Info(newName);
       cachedName = newName;
    }

    /**
     * Returns true if this is not a constructor or a class initializer (static
     * initializer).
     */
    public boolean isMethod() {
        String n = getName();
        return !n.equals(nameInit) && !n.equals(nameClinit);
    }

    /**
     * Returns a constant pool table used by this method.
     */
    public ConstPool getConstPool() {
        return constPool;
    }

    /**
     * Returns true if this is a constructor.
     */
    public boolean isConstructor() {
        return getName().equals(nameInit);
    }

    /**
     * Returns true if this is a class initializer (static initializer).
     */
    public boolean isStaticInitializer() {
        return getName().equals(nameClinit);
    }

    /**
     * Returns access flags.
     * 
     * @see AccessFlag
     */
    public int getAccessFlags() {
        return accessFlags;
    }

    /**
     * Sets access flags.
     * 
     * @see AccessFlag
     */
    public void setAccessFlags(int acc) {
        accessFlags = acc;
    }

    /**
     * Returns a method descriptor.
     * 
     * @see Descriptor
     */
    public String getDescriptor() {
        return constPool.getUtf8Info(descriptor);
    }

    /**
     * Sets a method descriptor.
     * 
     * @see Descriptor
     */
    public void setDescriptor(String desc) {
        if (!desc.equals(getDescriptor()))
            descriptor = constPool.addUtf8Info(desc);
    }

    /**
     * Returns all the attributes. A new element can be added to the returned
     * list and an existing element can be removed from the list.
     * 
     * @return a list of <code>AttributeInfo</code> objects.
     * @see AttributeInfo
     */
    public List getAttributes() {
        if (attribute == null)
            attribute = new LinkedList();

        return attribute;
    }

    /**
     * Returns the attribute with the specified name. If it is not found, this
     * method returns null.
     * 
     * @param name
     *            attribute name
     * @return an <code>AttributeInfo</code> object or null.
     */
    public AttributeInfo getAttribute(String name) {
        return AttributeInfo.lookup(attribute, name);
    }

    /**
     * Appends an attribute. If there is already an attribute with the same
     * name, the new one substitutes for it.
     */
    public void addAttribute(AttributeInfo info) {
        if (attribute == null)
            attribute = new LinkedList();

        AttributeInfo.remove(attribute, info.getName());
        attribute.add(info);
    }

    /**
     * Returns an Exceptions attribute.
     * 
     * @return an Exceptions attribute or null if it is not specified.
     */
    public ExceptionsAttribute getExceptionsAttribute() {
        AttributeInfo info = AttributeInfo.lookup(attribute,
                ExceptionsAttribute.tag);
        return (ExceptionsAttribute)info;
    }

    /**
     * Returns a Code attribute.
     * 
     * @return a Code attribute or null if it is not specified.
     */
    public CodeAttribute getCodeAttribute() {
        AttributeInfo info = AttributeInfo.lookup(attribute, CodeAttribute.tag);
        return (CodeAttribute)info;
    }

    /**
     * Removes an Exception attribute.
     */
    public void removeExceptionsAttribute() {
        AttributeInfo.remove(attribute, ExceptionsAttribute.tag);
    }

    /**
     * Adds an Exception attribute.
     * 
     * <p>
     * The added attribute must share the same constant pool table as this
     * <code>method_info</code> structure.
     */
    public void setExceptionsAttribute(ExceptionsAttribute cattr) {
        removeExceptionsAttribute();
        if (attribute == null)
            attribute = new LinkedList();

        attribute.add(cattr);
    }

    /**
     * Removes a Code attribute.
     */
    public void removeCodeAttribute() {
        AttributeInfo.remove(attribute, CodeAttribute.tag);
    }

    /**
     * Adds a Code attribute.
     * 
     * <p>
     * The added attribute must share the same constant pool table as this
     * <code>method_info</code> structure.
     */
    public void setCodeAttribute(CodeAttribute cattr) {
        removeCodeAttribute();
        if (attribute == null)
            attribute = new LinkedList();

        attribute.add(cattr);
    }

    /**
     * Returns the line number of the source line corresponding to the specified
     * bytecode contained in this method.
     * 
     * @param pos
     *            the position of the bytecode (&gt;= 0). an index into the code
     *            array.
     * @return -1 if this information is not available.
     */
    public int getLineNumber(int pos) {
        CodeAttribute ca = getCodeAttribute();
        if (ca == null)
            return -1;

        LineNumberAttribute ainfo = (LineNumberAttribute)ca
                .getAttribute(LineNumberAttribute.tag);
        if (ainfo == null)
            return -1;

        return ainfo.toLineNumber(pos);
    }

    /**
     * Changes a super constructor called by this constructor.
     * 
     * <p>
     * This method modifies a call to <code>super()</code>, which should be
     * at the head of a constructor body, so that a constructor in a different
     * super class is called. This method does not change actural parameters.
     * Hence the new super class must have a constructor with the same signature
     * as the original one.
     * 
     * <p>
     * This method should be called when the super class of the class declaring
     * this method is changed.
     * 
     * <p>
     * This method does not perform anything unless this <code>MethodInfo</code>
     * represents a constructor.
     * 
     * @param superclass
     *            the new super class
     */
    public void setSuperclass(String superclass) throws BadBytecode {
        if (!isConstructor())
            return;

        CodeAttribute ca = getCodeAttribute();
        byte[] code = ca.getCode();
        CodeIterator iterator = ca.iterator();
        int pos = iterator.skipSuperConstructor();
        if (pos >= 0) { // not this()
            ConstPool cp = constPool;
            int mref = ByteArray.readU16bit(code, pos + 1);
            int nt = cp.getMethodrefNameAndType(mref);
            int sc = cp.addClassInfo(superclass);
            int mref2 = cp.addMethodrefInfo(sc, nt);
            ByteArray.write16bit(mref2, code, pos + 1);
        }
    }

    private void read(MethodInfo src, String methodname, Map classnames)
            throws BadBytecode {
        ConstPool destCp = constPool;
        accessFlags = src.accessFlags;
        name = destCp.addUtf8Info(methodname);
        cachedName = methodname;
        ConstPool srcCp = src.constPool;
        String desc = srcCp.getUtf8Info(src.descriptor);
        String desc2 = Descriptor.rename(desc, classnames);
        descriptor = destCp.addUtf8Info(desc2);

        attribute = new LinkedList();
        ExceptionsAttribute eattr = src.getExceptionsAttribute();
        if (eattr != null)
            attribute.add(eattr.copy(destCp, classnames));

        CodeAttribute cattr = src.getCodeAttribute();
        if (cattr != null)
            attribute.add(cattr.copy(destCp, classnames));
    }

    private void read(DataInputStream in) throws IOException {
        accessFlags = in.readUnsignedShort();
        name = in.readUnsignedShort();
        descriptor = in.readUnsignedShort();
        int n = in.readUnsignedShort();
        attribute = new LinkedList();
        for (int i = 0; i < n; ++i)
            attribute.add(AttributeInfo.read(constPool, in));
    }

    void write(DataOutputStream out) throws IOException {
        out.writeShort(accessFlags);
        out.writeShort(name);
        out.writeShort(descriptor);

        if (attribute == null)
            out.writeShort(0);
        else {
            out.writeShort(attribute.size());
            AttributeInfo.writeAll(attribute, out);
        }
    }
}
