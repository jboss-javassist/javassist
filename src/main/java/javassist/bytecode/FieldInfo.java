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

package javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>field_info</code> structure.
 *
 * <p>The following code adds a public field <code>width</code>
 * of <code>int</code> type:
 * <blockquote><pre>
 * ClassFile cf = ...
 * FieldInfo f = new FieldInfo(cf.getConstPool(), "width", "I");
 * f.setAccessFlags(AccessFlag.PUBLIC);
 * cf.addField(f);
 * </pre></blockquote>
 *
 * @see javassist.CtField#getFieldInfo()
 */
public final class FieldInfo {
    ConstPool constPool;
    int accessFlags;
    int name;
    String cachedName;
    String cachedType;
    int descriptor;
    List<AttributeInfo> attribute;       // may be null.

    private FieldInfo(ConstPool cp) {
        constPool = cp;
        accessFlags = 0;
        attribute = null;
    }

    /**
     * Constructs a <code>field_info</code> structure.
     *
     * @param cp                a constant pool table
     * @param fieldName         field name
     * @param desc              field descriptor
     *
     * @see Descriptor
     */
    public FieldInfo(ConstPool cp, String fieldName, String desc) {
        this(cp);
        name = cp.addUtf8Info(fieldName);
        cachedName = fieldName;
        descriptor = cp.addUtf8Info(desc);
    }

    FieldInfo(ConstPool cp, DataInputStream in) throws IOException {
        this(cp);
        read(in);
    }

    /**
     * Returns a string representation of the object.
     */
    @Override
    public String toString() {
        return getName() + " " + getDescriptor();
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
        List<AttributeInfo> newAttributes = new ArrayList<AttributeInfo>();
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

        AttributeInfo signature
            = getAttribute(SignatureAttribute.tag);
        if (signature != null) {
            signature = signature.copy(cp, null);
            newAttributes.add(signature);
        }

        int index = getConstantValue();
        if (index != 0) {
            index = constPool.copy(index, cp, null);
            newAttributes.add(new ConstantAttribute(cp, index));
        }

        attribute = newAttributes;
        name = cp.addUtf8Info(getName());
        descriptor = cp.addUtf8Info(getDescriptor());
        constPool = cp;
    }

    /**
     * Returns the constant pool table used
     * by this <code>field_info</code>.
     */
    public ConstPool getConstPool() {
        return constPool;
    }

    /**
     * Returns the field name.
     */
    public String getName() {
       if (cachedName == null)
           cachedName = constPool.getUtf8Info(name);

       return cachedName;
    }

    /**
     * Sets the field name.
     */
    public void setName(String newName) {
        name = constPool.addUtf8Info(newName);
        cachedName = newName;
    }

    /**
     * Returns the access flags.
     *
     * @see AccessFlag
     */
    public int getAccessFlags() {
        return accessFlags;
    }

    /**
     * Sets the access flags.
     *
     * @see AccessFlag
     */
    public void setAccessFlags(int acc) {
        accessFlags = acc;
    }

    /**
     * Returns the field descriptor.
     *
     * @see Descriptor
     */
    public String getDescriptor() {
        return constPool.getUtf8Info(descriptor);
    }

    /**
     * Sets the field descriptor.
     *
     * @see Descriptor
     */
    public void setDescriptor(String desc) {
        if (!desc.equals(getDescriptor()))
            descriptor = constPool.addUtf8Info(desc);
    }

    /**
     * Finds a ConstantValue attribute and returns the index into
     * the <code>constant_pool</code> table.
     *
     * @return 0    if a ConstantValue attribute is not found.
     */
    public int getConstantValue() {
        if ((accessFlags & AccessFlag.STATIC) == 0)
            return 0;

        ConstantAttribute attr
            = (ConstantAttribute)getAttribute(ConstantAttribute.tag);
        if (attr == null)
            return 0;
        return attr.getConstantValue();
    }

    /**
     * Returns all the attributes.    The returned <code>List</code> object
     * is shared with this object.  If you add a new attribute to the list,
     * the attribute is also added to the field represented by this
     * object.  If you remove an attribute from the list, it is also removed
     * from the field.
     *
     * @return a list of <code>AttributeInfo</code> objects.
     * @see AttributeInfo
     */
    public List<AttributeInfo> getAttributes() {
        if (attribute == null)
            attribute = new ArrayList<AttributeInfo>();

        return attribute;
    }

    /**
     * Returns the attribute with the specified name.
     * It returns null if the specified attribute is not found.
     *
     * <p>An attribute name can be obtained by, for example,
     * {@link AnnotationsAttribute#visibleTag} or
     * {@link AnnotationsAttribute#invisibleTag}. 
     * </p>
     * 
     * @param name      attribute name
     * @see #getAttributes()
     */
    public AttributeInfo getAttribute(String name) {
        return AttributeInfo.lookup(attribute, name);
    }

    /**
     * Removes an attribute with the specified name.
     *
     * @param name      attribute name.
     * @return          the removed attribute or null.
     * @since 3.21
     */
    public AttributeInfo removeAttribute(String name) {
        return AttributeInfo.remove(attribute, name);
    }

    /**
     * Appends an attribute.  If there is already an attribute with
     * the same name, the new one substitutes for it.
     *
     * @see #getAttributes()
     */
    public void addAttribute(AttributeInfo info) {
        if (attribute == null)
            attribute = new ArrayList<AttributeInfo>();

        AttributeInfo.remove(attribute, info.getName());
        attribute.add(info);
    }

    private void read(DataInputStream in) throws IOException {
        accessFlags = in.readUnsignedShort();
        name = in.readUnsignedShort();
        descriptor = in.readUnsignedShort();
        int n = in.readUnsignedShort();
        attribute = new ArrayList<AttributeInfo>();
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
