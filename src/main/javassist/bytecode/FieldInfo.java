/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.LinkedList;

/**
 * <code>field_info</code> structure.
 *
 * @see javassist.CtField#getFieldInfo()
 */
public final class FieldInfo {
    ConstPool constPool;
    int accessFlags;
    int name;
    int descriptor;
    LinkedList attribute;       // may be null.

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
        descriptor = cp.addUtf8Info(desc);
    }

    FieldInfo(ConstPool cp, DataInputStream in) throws IOException {
        this(cp);
        read(in);
    }

    /**
     * Returns the constant pool table used
     * by this <code>field_info</code>.
     */
    public ConstPool getConstPool() { return constPool; }

    /**
     * Returns the field name.
     */
    public String getName() {
        return constPool.getUtf8Info(name);
    }

    /**
     * Sets the field name.
     */
    public void setName(String newName) {
        name = constPool.addUtf8Info(newName);
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
     * Returns all the attributes.
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
     * Returns the attribute with the specified name.
     *
     * @param name      attribute name
     */
    public AttributeInfo getAttribute(String name) {
        return AttributeInfo.lookup(attribute, name);
    }

    /**
     * Appends an attribute.  If there is already an attribute with
     * the same name, the new one substitutes for it.
     */
    public void addAttribute(AttributeInfo info) {
        if (attribute == null)
            attribute = new LinkedList();

        AttributeInfo.remove(attribute, info.getName());
        attribute.add(info);
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
