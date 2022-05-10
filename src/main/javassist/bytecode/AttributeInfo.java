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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// Note: if you define a new subclass of AttributeInfo, then
//       update AttributeInfo.read(), .copy(), and (maybe) write().

/**
 * <code>attribute_info</code> structure.
 *
 * @see ClassFile#getAttribute(String)
 * @see MethodInfo#getAttribute(String)
 * @see FieldInfo#getAttribute(String)
 */
public class AttributeInfo {
    protected ConstPool constPool;
    int name;
    byte[] info;

    protected AttributeInfo(ConstPool cp, int attrname, byte[] attrinfo) {
        constPool = cp;
        name = attrname;
        info = attrinfo;
    }

    protected AttributeInfo(ConstPool cp, String attrname) {
        this(cp, attrname, (byte[])null);
    }

    /**
     * Constructs an <code>attribute_info</code> structure.
     *
     * @param cp                constant pool table
     * @param attrname          attribute name
     * @param attrinfo          <code>info</code> field
     *                          of <code>attribute_info</code> structure.
     */
    public AttributeInfo(ConstPool cp, String attrname, byte[] attrinfo) {
        this(cp, cp.addUtf8Info(attrname), attrinfo);
    }

    protected AttributeInfo(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        constPool = cp;
        name = n;
        int len = in.readInt();
        info = new byte[len];
        if (len > 0)
            in.readFully(info);
    }

    static AttributeInfo read(ConstPool cp, DataInputStream in)
        throws IOException
    {
        int name = in.readUnsignedShort();
        String nameStr = cp.getUtf8Info(name);
        char first = nameStr.charAt(0);
        if (first < 'E')
            if (nameStr.equals(AnnotationDefaultAttribute.tag))
                return new AnnotationDefaultAttribute(cp, name, in);
            else if (nameStr.equals(BootstrapMethodsAttribute.tag))
                return new BootstrapMethodsAttribute(cp, name, in);
            else if (nameStr.equals(CodeAttribute.tag))
                return new CodeAttribute(cp, name, in);
            else if (nameStr.equals(ConstantAttribute.tag))
                return new ConstantAttribute(cp, name, in);
            else if (nameStr.equals(DeprecatedAttribute.tag))
                return new DeprecatedAttribute(cp, name, in);

        if (first < 'M')
            if (nameStr.equals(EnclosingMethodAttribute.tag))
                return new EnclosingMethodAttribute(cp, name, in);
            else if (nameStr.equals(ExceptionsAttribute.tag))
                return new ExceptionsAttribute(cp, name, in);
            else if (nameStr.equals(InnerClassesAttribute.tag))
                return new InnerClassesAttribute(cp, name, in);
            else if (nameStr.equals(LineNumberAttribute.tag))
                return new LineNumberAttribute(cp, name, in);
            else if (nameStr.equals(LocalVariableAttribute.tag))
                return new LocalVariableAttribute(cp, name, in);
            else if (nameStr.equals(LocalVariableTypeAttribute.tag))
                return new LocalVariableTypeAttribute(cp, name, in);

        if (first < 'S')
            /* Note that the names of Annotations attributes begin with 'R'. */
            if (nameStr.equals(MethodParametersAttribute.tag))
                return new MethodParametersAttribute(cp, name, in);
            else if (nameStr.equals(NestHostAttribute.tag))
                return new NestHostAttribute(cp, name, in);
            else if (nameStr.equals(NestMembersAttribute.tag))
                return new NestMembersAttribute(cp, name, in);
            else if (nameStr.equals(AnnotationsAttribute.visibleTag)
                     || nameStr.equals(AnnotationsAttribute.invisibleTag))
                // RuntimeVisibleAnnotations or RuntimeInvisibleAnnotations
                return new AnnotationsAttribute(cp, name, in);
            else if (nameStr.equals(ParameterAnnotationsAttribute.visibleTag)
                     || nameStr.equals(ParameterAnnotationsAttribute.invisibleTag))
                return new ParameterAnnotationsAttribute(cp, name, in);
            else if (nameStr.equals(TypeAnnotationsAttribute.visibleTag)
                     || nameStr.equals(TypeAnnotationsAttribute.invisibleTag))
                return new TypeAnnotationsAttribute(cp, name, in);

        if (first >= 'S')
            if (nameStr.equals(SignatureAttribute.tag))
                return new SignatureAttribute(cp, name, in);
            else if (nameStr.equals(SourceFileAttribute.tag))
                return new SourceFileAttribute(cp, name, in);
            else if (nameStr.equals(SyntheticAttribute.tag))
                return new SyntheticAttribute(cp, name, in);
            else if (nameStr.equals(StackMap.tag))
                return new StackMap(cp, name, in);
            else if (nameStr.equals(StackMapTable.tag))
                return new StackMapTable(cp, name, in);

        return new AttributeInfo(cp, name, in);
    }

    /**
     * Returns an attribute name.
     */
    public String getName() {
        return constPool.getUtf8Info(name);
    }

    /**
     * Returns a constant pool table.
     */
    public ConstPool getConstPool() { return constPool; }

    /**
     * Returns the length of this <code>attribute_info</code>
     * structure.
     * The returned value is <code>attribute_length + 6</code>.
     */
    public int length() {
        return info.length + 6;
    }

    /**
     * Returns the <code>info</code> field
     * of this <code>attribute_info</code> structure.
     *
     * <p>This method is not available if the object is an instance
     * of <code>CodeAttribute</code>.
     */
    public byte[] get() { return info; }

    /**
     * Sets the <code>info</code> field
     * of this <code>attribute_info</code> structure.
     *
     * <p>This method is not available if the object is an instance
     * of <code>CodeAttribute</code>.
     */
    public void set(byte[] newinfo) { info = newinfo; }

    /**
     * Makes a copy.  Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        pairs of replaced and substituted
     *                          class names.
     */
    public AttributeInfo copy(ConstPool newCp, Map<String,String> classnames)
    {
        return new AttributeInfo(newCp, getName(), Arrays.copyOf(info, info.length));
    }

    void write(DataOutputStream out) throws IOException
    {
        out.writeShort(name);
        out.writeInt(info.length);
        if (info.length > 0)
            out.write(info);
    }

    static int getLength(List<AttributeInfo> attributes) {
        int size = 0;

        for (AttributeInfo attr:attributes)
            size += attr.length();

        return size;
    }

    static AttributeInfo lookup(List<AttributeInfo> attributes, String name) {
        if (attributes == null)
            return null;

        for (AttributeInfo ai:attributes)
            if (ai.getName().equals(name))
                return ai;

        return null;            // no such attribute
    }

    static synchronized AttributeInfo remove(List<AttributeInfo> attributes, String name) {
        if (attributes == null)
            return null;

        for (AttributeInfo ai:attributes)
            if (ai.getName().equals(name))
                if (attributes.remove(ai))
                    return ai;

        return null;
    }

    static void writeAll(List<AttributeInfo> attributes, DataOutputStream out)
        throws IOException
    {
        if (attributes == null)
            return;

        for (AttributeInfo attr:attributes)
            attr.write(out);
    }

    static List<AttributeInfo> copyAll(List<AttributeInfo> attributes, ConstPool cp) {
        if (attributes == null)
            return null;

        List<AttributeInfo> newList = new ArrayList<AttributeInfo>();
        for (AttributeInfo attr:attributes)
            newList.add(attr.copy(cp, null));

        return newList;
    }

    /* The following two methods are used to implement
     * ClassFile.renameClass().
     * Only CodeAttribute, LocalVariableAttribute,
     * AnnotationDefaultAttribute, AnnotationsAttribute, and SignatureAttribute
     * override these methods.
     */
    void renameClass(String oldname, String newname) {}
    void renameClass(Map<String,String> classnames) {}

    static void renameClass(List<AttributeInfo> attributes, String oldname, String newname) {
        if (attributes == null)
            return;

        for (AttributeInfo ai:attributes)
            ai.renameClass(oldname, newname);
    }

    static void renameClass(List<AttributeInfo> attributes, Map<String,String> classnames) {
        if (attributes == null)
            return;

        for (AttributeInfo ai:attributes)
            ai.renameClass(classnames);
    }

    void getRefClasses(Map<String,String> classnames) {}

    static void getRefClasses(List<AttributeInfo> attributes, Map<String,String> classnames) {
        if (attributes == null)
            return;

        for (AttributeInfo ai:attributes)
            ai.getRefClasses(classnames);
    }
}
