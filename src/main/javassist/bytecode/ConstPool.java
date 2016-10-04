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
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javassist.CtClass;

/**
 * Constant pool table.
 */
public final class ConstPool {
    LongVector items;
    int numOfItems;
    int thisClassInfo;
    HashMap itemsCache;

    /**
     * <code>CONSTANT_Class</code>
     */
    public static final int CONST_Class = ClassInfo.tag;

    /**
     * <code>CONSTANT_Fieldref</code>
     */
    public static final int CONST_Fieldref = FieldrefInfo.tag;

    /**
     * <code>CONSTANT_Methodref</code>
     */
    public static final int CONST_Methodref = MethodrefInfo.tag;

    /**
     * <code>CONSTANT_InterfaceMethodref</code>
     */
    public static final int CONST_InterfaceMethodref
                                        = InterfaceMethodrefInfo.tag;

    /**
     * <code>CONSTANT_String</code>
     */
    public static final int CONST_String = StringInfo.tag;

    /**
     * <code>CONSTANT_Integer</code>
     */
    public static final int CONST_Integer = IntegerInfo.tag;

    /**
     * <code>CONSTANT_Float</code>
     */
    public static final int CONST_Float = FloatInfo.tag;

    /**
     * <code>CONSTANT_Long</code>
     */
    public static final int CONST_Long = LongInfo.tag;

    /**
     * <code>CONSTANT_Double</code>
     */
    public static final int CONST_Double = DoubleInfo.tag;

    /**
     * <code>CONSTANT_NameAndType</code>
     */
    public static final int CONST_NameAndType = NameAndTypeInfo.tag;

    /**
     * <code>CONSTANT_Utf8</code>
     */
    public static final int CONST_Utf8 = Utf8Info.tag;

    /**
     * <code>CONSTANT_MethodHandle</code>
     */
    public static final int CONST_MethodHandle = MethodHandleInfo.tag;

    /**
     * <code>CONSTANT_MethodHandle</code>
     */
    public static final int CONST_MethodType = MethodTypeInfo.tag;

    /**
     * <code>CONSTANT_MethodHandle</code>
     */
    public static final int CONST_InvokeDynamic = InvokeDynamicInfo.tag;

    /**
     * Represents the class using this constant pool table.
     */
    public static final CtClass THIS = null;

    /**
     * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
     */
    public static final int REF_getField = 1;

    /**
     * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
     */
    public static final int REF_getStatic = 2;

    /**
     * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
     */
    public static final int REF_putField = 3;

    /**
     * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
     */
    public static final int REF_putStatic = 4;

    /**
     * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
     */
    public static final int REF_invokeVirtual = 5;

    /**
     * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
     */
    public static final int REF_invokeStatic = 6;

    /**
     * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
     */
    public static final int REF_invokeSpecial = 7;

    /**
     * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
     */
    public static final int REF_newInvokeSpecial = 8;

    /**
     * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
     */
    public static final int REF_invokeInterface = 9;

    /**
     * Constructs a constant pool table.
     *
     * @param thisclass         the name of the class using this constant
     *                          pool table
     */
    public ConstPool(String thisclass) {
        items = new LongVector();
        itemsCache = null;
        numOfItems = 0;
        addItem0(null);          // index 0 is reserved by the JVM.
        thisClassInfo = addClassInfo(thisclass);
    }

    /**
     * Constructs a constant pool table from the given byte stream.
     *
     * @param in        byte stream.
     */
    public ConstPool(DataInputStream in) throws IOException {
        itemsCache = null;
        thisClassInfo = 0;
        /* read() initializes items and numOfItems, and do addItem(null).
         */
        read(in);
    }

    void prune() {
        itemsCache = null;
    }

    /**
     * Returns the number of entries in this table.
     */
    public int getSize() {
        return numOfItems;
    }

    /**
     * Returns the name of the class using this constant pool table.
     */
    public String getClassName() {
        return getClassInfo(thisClassInfo);
    }

    /**
     * Returns the index of <code>CONSTANT_Class_info</code> structure
     * specifying the class using this constant pool table.
     */
    public int getThisClassInfo() {
        return thisClassInfo;
    }

    void setThisClassInfo(int i) {
        thisClassInfo = i;
    }

    ConstInfo getItem(int n) {
        return items.elementAt(n);
    }

    /**
     * Returns the <code>tag</code> field of the constant pool table
     * entry at the given index.
     *
     * @return either <code>CONST_Class</code>, <code>CONST_Fieldref</code>,
     *         <code>CONST_Methodref</code>, or ...  
     */
    public int getTag(int index) {
        return getItem(index).getTag();
    }

    /**
     * Reads <code>CONSTANT_Class_info</code> structure
     * at the given index.
     *
     * @return  a fully-qualified class or interface name specified
     *          by <code>name_index</code>.  If the type is an array
     *          type, this method returns an encoded name like
     *          <code>[Ljava.lang.Object;</code> (note that the separators
     *          are not slashes but dots).
     * @see javassist.ClassPool#getCtClass(String)
     */
    public String getClassInfo(int index) {
        ClassInfo c = (ClassInfo)getItem(index);
        if (c == null)
            return null;
        else
            return Descriptor.toJavaName(getUtf8Info(c.name));
    }

    /**
     * Reads <code>CONSTANT_Class_info</code> structure
     * at the given index.
     *
     * @return  the descriptor of the type specified
     *          by <code>name_index</code>.
     * @see javassist.ClassPool#getCtClass(String)
     * @since 3.15
     */
    public String getClassInfoByDescriptor(int index) {
        ClassInfo c = (ClassInfo)getItem(index);
        if (c == null)
            return null;
        else {
            String className = getUtf8Info(c.name);
            if (className.charAt(0) == '[')
                return className;
            else
                return Descriptor.of(className);
        }
    }

    /**
     * Reads the <code>name_index</code> field of the
     * <code>CONSTANT_NameAndType_info</code> structure
     * at the given index.
     */
    public int getNameAndTypeName(int index) {
        NameAndTypeInfo ntinfo = (NameAndTypeInfo)getItem(index);
        return ntinfo.memberName;
    }

    /**
     * Reads the <code>descriptor_index</code> field of the
     * <code>CONSTANT_NameAndType_info</code> structure
     * at the given index.
     */
    public int getNameAndTypeDescriptor(int index) {
        NameAndTypeInfo ntinfo = (NameAndTypeInfo)getItem(index);
        return ntinfo.typeDescriptor;
    }

    /**
     * Reads the <code>class_index</code> field of the
     * <code>CONSTANT_Fieldref_info</code>,
     * <code>CONSTANT_Methodref_info</code>,
     * or <code>CONSTANT_Interfaceref_info</code>,
     * structure at the given index.
     *
     * @since 3.6
     */
    public int getMemberClass(int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return minfo.classIndex;
    }

    /**
     * Reads the <code>name_and_type_index</code> field of the
     * <code>CONSTANT_Fieldref_info</code>,
     * <code>CONSTANT_Methodref_info</code>,
     * or <code>CONSTANT_Interfaceref_info</code>,
     * structure at the given index.
     *
     * @since 3.6
     */
    public int getMemberNameAndType(int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return minfo.nameAndTypeIndex;
    }

    /**
     * Reads the <code>class_index</code> field of the
     * <code>CONSTANT_Fieldref_info</code> structure
     * at the given index.
     */
    public int getFieldrefClass(int index) {
        FieldrefInfo finfo = (FieldrefInfo)getItem(index);
        return finfo.classIndex;
    }

    /**
     * Reads the <code>class_index</code> field of the
     * <code>CONSTANT_Fieldref_info</code> structure
     * at the given index.
     *
     * @return the name of the class at that <code>class_index</code>.
     */
    public String getFieldrefClassName(int index) {
        FieldrefInfo f = (FieldrefInfo)getItem(index);
        if (f == null)
            return null;
        else
            return getClassInfo(f.classIndex);
    }

    /**
     * Reads the <code>name_and_type_index</code> field of the
     * <code>CONSTANT_Fieldref_info</code> structure
     * at the given index.
     */
    public int getFieldrefNameAndType(int index) {
        FieldrefInfo finfo = (FieldrefInfo)getItem(index);
        return finfo.nameAndTypeIndex;
    }

    /**
     * Reads the <code>name_index</code> field of the
     * <code>CONSTANT_NameAndType_info</code> structure
     * indirectly specified by the given index.
     *
     * @param index     an index to a <code>CONSTANT_Fieldref_info</code>.
     * @return  the name of the field.
     */
    public String getFieldrefName(int index) {
        FieldrefInfo f = (FieldrefInfo)getItem(index);
        if (f == null)
            return null;
        else {
            NameAndTypeInfo n = (NameAndTypeInfo)getItem(f.nameAndTypeIndex);
            if(n == null)
                return null;
            else
                return getUtf8Info(n.memberName);
        }
    }

    /**
     * Reads the <code>descriptor_index</code> field of the
     * <code>CONSTANT_NameAndType_info</code> structure
     * indirectly specified by the given index.
     *
     * @param index     an index to a <code>CONSTANT_Fieldref_info</code>.
     * @return  the type descriptor of the field.
     */
    public String getFieldrefType(int index) {
        FieldrefInfo f = (FieldrefInfo)getItem(index);
        if (f == null)
            return null;
        else {
            NameAndTypeInfo n = (NameAndTypeInfo)getItem(f.nameAndTypeIndex);
            if(n == null)
                return null;
            else
                return getUtf8Info(n.typeDescriptor);
        }
    }

    /**
     * Reads the <code>class_index</code> field of the
     * <code>CONSTANT_Methodref_info</code> structure
     * at the given index.
     */
    public int getMethodrefClass(int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return minfo.classIndex;
    }

    /**
     * Reads the <code>class_index</code> field of the
     * <code>CONSTANT_Methodref_info</code> structure
     * at the given index.
     *
     * @return the name of the class at that <code>class_index</code>.
     */
    public String getMethodrefClassName(int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        if (minfo == null)
            return null;
        else
            return getClassInfo(minfo.classIndex);
    }

    /**
     * Reads the <code>name_and_type_index</code> field of the
     * <code>CONSTANT_Methodref_info</code> structure
     * at the given index.
     */
    public int getMethodrefNameAndType(int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return minfo.nameAndTypeIndex;
    }

    /**
     * Reads the <code>name_index</code> field of the
     * <code>CONSTANT_NameAndType_info</code> structure
     * indirectly specified by the given index.
     *
     * @param index     an index to a <code>CONSTANT_Methodref_info</code>.
     * @return  the name of the method.
     */
    public String getMethodrefName(int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        if (minfo == null)
            return null;
        else {
            NameAndTypeInfo n
                = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
            if(n == null)
                return null;
            else
                return getUtf8Info(n.memberName);
        }
    }

    /**
     * Reads the <code>descriptor_index</code> field of the
     * <code>CONSTANT_NameAndType_info</code> structure
     * indirectly specified by the given index.
     *
     * @param index     an index to a <code>CONSTANT_Methodref_info</code>.
     * @return  the descriptor of the method.
     */
    public String getMethodrefType(int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        if (minfo == null)
            return null;
        else {
            NameAndTypeInfo n
                = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
            if(n == null)
                return null;
            else
                return getUtf8Info(n.typeDescriptor);
        }
    }

    /**
     * Reads the <code>class_index</code> field of the
     * <code>CONSTANT_InterfaceMethodref_info</code> structure
     * at the given index.
     */
    public int getInterfaceMethodrefClass(int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return minfo.classIndex;
    }

    /**
     * Reads the <code>class_index</code> field of the
     * <code>CONSTANT_InterfaceMethodref_info</code> structure
     * at the given index.
     *
     * @return the name of the class at that <code>class_index</code>.
     */
    public String getInterfaceMethodrefClassName(int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return getClassInfo(minfo.classIndex);
    }

    /**
     * Reads the <code>name_and_type_index</code> field of the
     * <code>CONSTANT_InterfaceMethodref_info</code> structure
     * at the given index.
     */
    public int getInterfaceMethodrefNameAndType(int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return minfo.nameAndTypeIndex;
    }

    /**
     * Reads the <code>name_index</code> field of the
     * <code>CONSTANT_NameAndType_info</code> structure
     * indirectly specified by the given index.
     *
     * @param index     an index to
     *                  a <code>CONSTANT_InterfaceMethodref_info</code>.
     * @return  the name of the method.
     */
    public String getInterfaceMethodrefName(int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        if (minfo == null)
            return null;
        else {
            NameAndTypeInfo n
                = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
            if(n == null)
                return null;
            else
                return getUtf8Info(n.memberName);
        }
    }

    /**
     * Reads the <code>descriptor_index</code> field of the
     * <code>CONSTANT_NameAndType_info</code> structure
     * indirectly specified by the given index.
     *
     * @param index     an index to
     *                  a <code>CONSTANT_InterfaceMethodref_info</code>.
     * @return  the descriptor of the method.
     */
    public String getInterfaceMethodrefType(int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        if (minfo == null)
            return null;
        else {
            NameAndTypeInfo n
                = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
            if(n == null)
                return null;
            else
                return getUtf8Info(n.typeDescriptor);
        }
    }
    /**
     * Reads <code>CONSTANT_Integer_info</code>, <code>_Float_info</code>,
     * <code>_Long_info</code>, <code>_Double_info</code>, or
     * <code>_String_info</code> structure.
     * These are used with the LDC instruction.
     *
     * @return a <code>String</code> value or a wrapped primitive-type
     * value.
     */
    public Object getLdcValue(int index) {
        ConstInfo constInfo = this.getItem(index);
        Object value = null;
        if (constInfo instanceof StringInfo)
            value = this.getStringInfo(index);
        else if (constInfo instanceof FloatInfo)
            value = Float.valueOf(getFloatInfo(index));
        else if (constInfo instanceof IntegerInfo)
            value = Integer.valueOf(getIntegerInfo(index));
        else if (constInfo instanceof LongInfo)
            value = Long.valueOf(getLongInfo(index));
        else if (constInfo instanceof DoubleInfo)
            value = Double.valueOf(getDoubleInfo(index));
        else
            value = null;

        return value;
    }

    /**
     * Reads <code>CONSTANT_Integer_info</code> structure
     * at the given index.
     *
     * @return the value specified by this entry.
     */
    public int getIntegerInfo(int index) {
        IntegerInfo i = (IntegerInfo)getItem(index);
        return i.value;
    }

    /**
     * Reads <code>CONSTANT_Float_info</code> structure
     * at the given index.
     *
     * @return the value specified by this entry.
     */
    public float getFloatInfo(int index) {
        FloatInfo i = (FloatInfo)getItem(index);
        return i.value;
    }

    /**
     * Reads <code>CONSTANT_Long_info</code> structure
     * at the given index.
     *
     * @return the value specified by this entry.
     */
    public long getLongInfo(int index) {
        LongInfo i = (LongInfo)getItem(index);
        return i.value;
    }

    /**
     * Reads <code>CONSTANT_Double_info</code> structure
     * at the given index.
     *
     * @return the value specified by this entry.
     */
    public double getDoubleInfo(int index) {
        DoubleInfo i = (DoubleInfo)getItem(index);
        return i.value;
    }

    /**
     * Reads <code>CONSTANT_String_info</code> structure
     * at the given index.
     *
     * @return the string specified by <code>string_index</code>.
     */
    public String getStringInfo(int index) {
        StringInfo si = (StringInfo)getItem(index);
        return getUtf8Info(si.string);
    }

    /**
     * Reads <code>CONSTANT_utf8_info</code> structure
     * at the given index.
     *
     * @return the string specified by this entry.
     */
    public String getUtf8Info(int index) {
        Utf8Info utf = (Utf8Info)getItem(index);
        return utf.string;
    }

    /**
     * Reads the <code>reference_kind</code> field of the
     * <code>CONSTANT_MethodHandle_info</code> structure
     * at the given index.
     *
     * @see #REF_getField
     * @see #REF_getStatic
     * @see #REF_invokeInterface
     * @see #REF_invokeSpecial
     * @see #REF_invokeStatic
     * @see #REF_invokeVirtual
     * @see #REF_newInvokeSpecial
     * @see #REF_putField
     * @see #REF_putStatic
     * @since 3.17
     */
    public int getMethodHandleKind(int index) {
        MethodHandleInfo mhinfo = (MethodHandleInfo)getItem(index);
        return mhinfo.refKind;
    }

    /**
     * Reads the <code>reference_index</code> field of the
     * <code>CONSTANT_MethodHandle_info</code> structure
     * at the given index.
     *
     * @since 3.17
     */
    public int getMethodHandleIndex(int index) {
        MethodHandleInfo mhinfo = (MethodHandleInfo)getItem(index);
        return mhinfo.refIndex;
    }

    /**
     * Reads the <code>descriptor_index</code> field of the
     * <code>CONSTANT_MethodType_info</code> structure
     * at the given index.
     *
     * @since 3.17
     */
    public int getMethodTypeInfo(int index) {
        MethodTypeInfo mtinfo = (MethodTypeInfo)getItem(index);
        return mtinfo.descriptor;
    }

    /**
     * Reads the <code>bootstrap_method_attr_index</code> field of the
     * <code>CONSTANT_InvokeDynamic_info</code> structure
     * at the given index.
     *
     * @since 3.17
     */
    public int getInvokeDynamicBootstrap(int index) {
        InvokeDynamicInfo iv = (InvokeDynamicInfo)getItem(index);
        return iv.bootstrap;
    }

    /**
     * Reads the <code>name_and_type_index</code> field of the
     * <code>CONSTANT_InvokeDynamic_info</code> structure
     * at the given index.
     *
     * @since 3.17
     */
    public int getInvokeDynamicNameAndType(int index) {
        InvokeDynamicInfo iv = (InvokeDynamicInfo)getItem(index);
        return iv.nameAndType;
    }

    /**
     * Reads the <code>descriptor_index</code> field of the
     * <code>CONSTANT_NameAndType_info</code> structure
     * indirectly specified by the given index.
     *
     * @param index     an index to a <code>CONSTANT_InvokeDynamic_info</code>.
     * @return  the descriptor of the method.
     * @since 3.17
     */
    public String getInvokeDynamicType(int index) {
        InvokeDynamicInfo iv = (InvokeDynamicInfo)getItem(index);
        if (iv == null)
            return null;
        else {
            NameAndTypeInfo n = (NameAndTypeInfo)getItem(iv.nameAndType);
            if(n == null)
                return null;
            else
                return getUtf8Info(n.typeDescriptor);
        }
    }

    /**
     * Determines whether <code>CONSTANT_Methodref_info</code>
     * structure at the given index represents the constructor
     * of the given class.
     *
     * @return          the <code>descriptor_index</code> specifying
     *                  the type descriptor of the that constructor.
     *                  If it is not that constructor,
     *                  <code>isConstructor()</code> returns 0.
     */
    public int isConstructor(String classname, int index) {
        return isMember(classname, MethodInfo.nameInit, index);
    }

    /**
     * Determines whether <code>CONSTANT_Methodref_info</code>,
     * <code>CONSTANT_Fieldref_info</code>, or
     * <code>CONSTANT_InterfaceMethodref_info</code> structure
     * at the given index represents the member with the specified
     * name and declaring class.
     *
     * @param classname         the class declaring the member
     * @param membername        the member name
     * @param index             the index into the constant pool table
     *
     * @return          the <code>descriptor_index</code> specifying
     *                  the type descriptor of that member.
     *                  If it is not that member,
     *                  <code>isMember()</code> returns 0.
     */
    public int isMember(String classname, String membername, int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        if (getClassInfo(minfo.classIndex).equals(classname)) {
            NameAndTypeInfo ntinfo
                = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
            if (getUtf8Info(ntinfo.memberName).equals(membername))
                return ntinfo.typeDescriptor;
        }

        return 0;       // false
    }

    /**
     * Determines whether <code>CONSTANT_Methodref_info</code>,
     * <code>CONSTANT_Fieldref_info</code>, or
     * <code>CONSTANT_InterfaceMethodref_info</code> structure
     * at the given index has the name and the descriptor
     * given as the arguments.
     *
     * @param membername        the member name
     * @param desc              the descriptor of the member.
     * @param index             the index into the constant pool table
     *
     * @return          the name of the target class specified by
     *                  the <code>..._info</code> structure
     *                  at <code>index</code>.
     *                  Otherwise, null if that structure does not 
     *                  match the given member name and descriptor.
     */
    public String eqMember(String membername, String desc, int index) {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        NameAndTypeInfo ntinfo
                = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
        if (getUtf8Info(ntinfo.memberName).equals(membername)
            && getUtf8Info(ntinfo.typeDescriptor).equals(desc))
            return getClassInfo(minfo.classIndex);
        else
            return null;       // false
    }

    private int addItem0(ConstInfo info) {
        items.addElement(info);
        return numOfItems++;
    }

    private int addItem(ConstInfo info) {
        if (itemsCache == null)
            itemsCache = makeItemsCache(items);

        ConstInfo found = (ConstInfo)itemsCache.get(info);
        if (found != null)
            return found.index;
        else {
            items.addElement(info);
            itemsCache.put(info, info);
            return numOfItems++;
        }
    }

    /**
     * Copies the n-th item in this ConstPool object into the destination
     * ConstPool object.
     * The class names that the item refers to are renamed according
     * to the given map.
     *
     * @param n                 the <i>n</i>-th item
     * @param dest              destination constant pool table
     * @param classnames        the map or null.
     * @return the index of the copied item into the destination ClassPool.
     */
    public int copy(int n, ConstPool dest, Map classnames) {
        if (n == 0)
            return 0;

        ConstInfo info = getItem(n);
        return info.copy(this, dest, classnames);
    }

    int addConstInfoPadding() {
        return addItem0(new ConstInfoPadding(numOfItems));
    }

    /**
     * Adds a new <code>CONSTANT_Class_info</code> structure.
     *
     * <p>This also adds a <code>CONSTANT_Utf8_info</code> structure
     * for storing the class name.
     *
     * @return          the index of the added entry.
     */
    public int addClassInfo(CtClass c) {
        if (c == THIS)
            return thisClassInfo;
        else if (!c.isArray())
            return addClassInfo(c.getName());
        else {
            // an array type is recorded in the hashtable with
            // the key "[L<classname>;" instead of "<classname>".
            //
            // note: toJvmName(toJvmName(c)) is equal to toJvmName(c).

            return addClassInfo(Descriptor.toJvmName(c));
        }
    }

    /**
     * Adds a new <code>CONSTANT_Class_info</code> structure.
     *
     * <p>This also adds a <code>CONSTANT_Utf8_info</code> structure
     * for storing the class name.
     *
     * @param qname     a fully-qualified class name
     *                  (or the JVM-internal representation of that name).
     * @return          the index of the added entry.
     */
    public int addClassInfo(String qname) {
        int utf8 = addUtf8Info(Descriptor.toJvmName(qname));
        return addItem(new ClassInfo(utf8, numOfItems));
    }

    /**
     * Adds a new <code>CONSTANT_NameAndType_info</code> structure.
     *
     * <p>This also adds <code>CONSTANT_Utf8_info</code> structures.
     *
     * @param name      <code>name_index</code>
     * @param type      <code>descriptor_index</code>
     * @return          the index of the added entry.
     */
    public int addNameAndTypeInfo(String name, String type) {
        return addNameAndTypeInfo(addUtf8Info(name), addUtf8Info(type));
    }

    /**
     * Adds a new <code>CONSTANT_NameAndType_info</code> structure.
     *
     * @param name      <code>name_index</code>
     * @param type      <code>descriptor_index</code>
     * @return          the index of the added entry.
     */
    public int addNameAndTypeInfo(int name, int type) {
        return addItem(new NameAndTypeInfo(name, type, numOfItems));
    }

    /**
     * Adds a new <code>CONSTANT_Fieldref_info</code> structure.
     *
     * <p>This also adds a new <code>CONSTANT_NameAndType_info</code>
     * structure.
     *
     * @param classInfo         <code>class_index</code>
     * @param name              <code>name_index</code>
     *                          of <code>CONSTANT_NameAndType_info</code>.
     * @param type              <code>descriptor_index</code>
     *                          of <code>CONSTANT_NameAndType_info</code>.
     * @return          the index of the added entry.
     */
    public int addFieldrefInfo(int classInfo, String name, String type) {
        int nt = addNameAndTypeInfo(name, type);
        return addFieldrefInfo(classInfo, nt);
    }

    /**
     * Adds a new <code>CONSTANT_Fieldref_info</code> structure.
     *
     * @param classInfo         <code>class_index</code>
     * @param nameAndTypeInfo   <code>name_and_type_index</code>.
     * @return          the index of the added entry.
     */
    public int addFieldrefInfo(int classInfo, int nameAndTypeInfo) {
        return addItem(new FieldrefInfo(classInfo, nameAndTypeInfo, numOfItems));
    }

    /**
     * Adds a new <code>CONSTANT_Methodref_info</code> structure.
     *
     * <p>This also adds a new <code>CONSTANT_NameAndType_info</code>
     * structure.
     *
     * @param classInfo         <code>class_index</code>
     * @param name              <code>name_index</code>
     *                          of <code>CONSTANT_NameAndType_info</code>.
     * @param type              <code>descriptor_index</code>
     *                          of <code>CONSTANT_NameAndType_info</code>.
     * @return          the index of the added entry.
     */
    public int addMethodrefInfo(int classInfo, String name, String type) {
        int nt = addNameAndTypeInfo(name, type);
        return addMethodrefInfo(classInfo, nt);
    }

    /**
     * Adds a new <code>CONSTANT_Methodref_info</code> structure.
     *
     * @param classInfo         <code>class_index</code>
     * @param nameAndTypeInfo   <code>name_and_type_index</code>.
     * @return          the index of the added entry.
     */
    public int addMethodrefInfo(int classInfo, int nameAndTypeInfo) {
         return addItem(new MethodrefInfo(classInfo, nameAndTypeInfo, numOfItems));
    }

    /**
     * Adds a new <code>CONSTANT_InterfaceMethodref_info</code>
     * structure.
     *
     * <p>This also adds a new <code>CONSTANT_NameAndType_info</code>
     * structure.
     *
     * @param classInfo         <code>class_index</code>
     * @param name              <code>name_index</code>
     *                          of <code>CONSTANT_NameAndType_info</code>.
     * @param type              <code>descriptor_index</code>
     *                          of <code>CONSTANT_NameAndType_info</code>.
     * @return          the index of the added entry.
     */
    public int addInterfaceMethodrefInfo(int classInfo, String name,
                                         String type) {
        int nt = addNameAndTypeInfo(name, type);
        return addInterfaceMethodrefInfo(classInfo, nt);
    }

    /**
     * Adds a new <code>CONSTANT_InterfaceMethodref_info</code>
     * structure.
     *
     * @param classInfo         <code>class_index</code>
     * @param nameAndTypeInfo   <code>name_and_type_index</code>.
     * @return          the index of the added entry.
     */
    public int addInterfaceMethodrefInfo(int classInfo,
                                         int nameAndTypeInfo) {
        return addItem(new InterfaceMethodrefInfo(classInfo, nameAndTypeInfo,
                                                  numOfItems));
    }

    /**
     * Adds a new <code>CONSTANT_String_info</code>
     * structure.
     *
     * <p>This also adds a new <code>CONSTANT_Utf8_info</code>
     * structure.
     *
     * @return          the index of the added entry.
     */
    public int addStringInfo(String str) {
        int utf = addUtf8Info(str);
        return addItem(new StringInfo(utf, numOfItems));
    }

    /**
     * Adds a new <code>CONSTANT_Integer_info</code>
     * structure.
     *
     * @return          the index of the added entry.
     */
    public int addIntegerInfo(int i) {
        return addItem(new IntegerInfo(i, numOfItems));
    }

    /**
     * Adds a new <code>CONSTANT_Float_info</code>
     * structure.
     *
     * @return          the index of the added entry.
     */
    public int addFloatInfo(float f) {
        return addItem(new FloatInfo(f, numOfItems));
    }

    /**
     * Adds a new <code>CONSTANT_Long_info</code>
     * structure.
     *
     * @return          the index of the added entry.
     */
    public int addLongInfo(long l) {
        int i = addItem(new LongInfo(l, numOfItems));
        if (i == numOfItems - 1)    // if not existing
            addConstInfoPadding();

        return i;
    }

    /**
     * Adds a new <code>CONSTANT_Double_info</code>
     * structure.
     *
     * @return          the index of the added entry.
     */
    public int addDoubleInfo(double d) {
        int i = addItem(new DoubleInfo(d, numOfItems));
        if (i == numOfItems - 1)    // if not existing
            addConstInfoPadding();

        return i;
    }

    /**
     * Adds a new <code>CONSTANT_Utf8_info</code>
     * structure.
     *
     * @return          the index of the added entry.
     */
    public int addUtf8Info(String utf8) {
        return addItem(new Utf8Info(utf8, numOfItems));
    }

    /**
     * Adds a new <code>CONSTANT_MethodHandle_info</code>
     * structure.
     *
     * @param kind      <code>reference_kind</code>
     *                  such as {@link #REF_invokeStatic <code>REF_invokeStatic</code>}.
     * @param index     <code>reference_index</code>.
     * @return          the index of the added entry.
     *
     * @since 3.17
     */
    public int addMethodHandleInfo(int kind, int index) {
        return addItem(new MethodHandleInfo(kind, index, numOfItems));
    }

    /**
     * Adds a new <code>CONSTANT_MethodType_info</code>
     * structure.
     *
     * @param desc      <code>descriptor_index</code>.
     * @return          the index of the added entry.
     *
     * @since 3.17
     */
    public int addMethodTypeInfo(int desc) {
        return addItem(new MethodTypeInfo(desc, numOfItems));
    }

    /**
     * Adds a new <code>CONSTANT_InvokeDynamic_info</code>
     * structure.
     *
     * @param bootstrap     <code>bootstrap_method_attr_index</code>.
     * @param nameAndType   <code>name_and_type_index</code>.
     * @return          the index of the added entry.
     *
     * @since 3.17
     */
    public int addInvokeDynamicInfo(int bootstrap, int nameAndType) {
        return addItem(new InvokeDynamicInfo(bootstrap, nameAndType, numOfItems));
    }

    /**
     * Get all the class names.
     *
     * @return a set of class names (<code>String</code> objects).
     */
    public Set getClassNames() {
        HashSet result = new HashSet();
        LongVector v = items;
        int size = numOfItems;
        for (int i = 1; i < size; ++i) {
            String className = v.elementAt(i).getClassName(this);
            if (className != null)
               result.add(className);
        }
        return result;
    }

    /**
     * Replaces all occurrences of a class name.
     *
     * @param oldName           the replaced name (JVM-internal representation).
     * @param newName           the substituted name (JVM-internal representation).
     */
    public void renameClass(String oldName, String newName) {
        LongVector v = items;
        int size = numOfItems;
        for (int i = 1; i < size; ++i) {
            ConstInfo ci = v.elementAt(i);
            ci.renameClass(this, oldName, newName, itemsCache);
        }
    }

    /**
     * Replaces all occurrences of class names.
     *
     * @param classnames        specifies pairs of replaced and substituted
     *                          name.
     */
    public void renameClass(Map classnames) {
        LongVector v = items;
        int size = numOfItems;
        for (int i = 1; i < size; ++i) {
            ConstInfo ci = v.elementAt(i);
            ci.renameClass(this, classnames, itemsCache);
        }
    }

    private void read(DataInputStream in) throws IOException {
        int n = in.readUnsignedShort();

        items = new LongVector(n);
        numOfItems = 0;
        addItem0(null);          // index 0 is reserved by the JVM.

        while (--n > 0) {       // index 0 is reserved by JVM
            int tag = readOne(in);
            if ((tag == LongInfo.tag) || (tag == DoubleInfo.tag)) {
                addConstInfoPadding();
                --n;
            }
        }
    }

    private static HashMap makeItemsCache(LongVector items) {
        HashMap cache = new HashMap();
        int i = 1;
        while (true) {
            ConstInfo info = items.elementAt(i++);
            if (info == null)
                break;
            else
                cache.put(info, info);
        }

        return cache;
    }

    private int readOne(DataInputStream in) throws IOException {
        ConstInfo info;
        int tag = in.readUnsignedByte();
        switch (tag) {
        case Utf8Info.tag :                     // 1
            info = new Utf8Info(in, numOfItems);
            break;
        case IntegerInfo.tag :                  // 3
            info = new IntegerInfo(in, numOfItems);
            break;
        case FloatInfo.tag :                    // 4
            info = new FloatInfo(in, numOfItems);
            break;
        case LongInfo.tag :                     // 5
            info = new LongInfo(in, numOfItems);
            break;
        case DoubleInfo.tag :                   // 6
            info = new DoubleInfo(in, numOfItems);
            break;
        case ClassInfo.tag :                    // 7
            info = new ClassInfo(in, numOfItems);
            break;
        case StringInfo.tag :                   // 8
            info = new StringInfo(in, numOfItems);
            break;
        case FieldrefInfo.tag :                 // 9
            info = new FieldrefInfo(in, numOfItems);
            break;
        case MethodrefInfo.tag :                // 10
            info = new MethodrefInfo(in, numOfItems);
            break;
        case InterfaceMethodrefInfo.tag :       // 11
            info = new InterfaceMethodrefInfo(in, numOfItems);
            break;
        case NameAndTypeInfo.tag :              // 12
            info = new NameAndTypeInfo(in, numOfItems);
            break;
        case MethodHandleInfo.tag :             // 15
            info = new MethodHandleInfo(in, numOfItems);
            break;
        case MethodTypeInfo.tag :               // 16
            info = new MethodTypeInfo(in, numOfItems);
            break;
        case InvokeDynamicInfo.tag :            // 18
            info = new InvokeDynamicInfo(in, numOfItems);
            break;
        default :
            throw new IOException("invalid constant type: " + tag + " at " + numOfItems);
        }

        addItem0(info);
        return tag;
    }

    /**
     * Writes the contents of the constant pool table.
     */
    public void write(DataOutputStream out) throws IOException {
        out.writeShort(numOfItems);
        LongVector v = items;
        int size = numOfItems;
        for (int i = 1; i < size; ++i)
            v.elementAt(i).write(out);
    }

    /**
     * Prints the contents of the constant pool table.
     */
    public void print() {
        print(new PrintWriter(System.out, true));
    }

    /**
     * Prints the contents of the constant pool table.
     */
    public void print(PrintWriter out) {
        int size = numOfItems;
        for (int i = 1; i < size; ++i) {
            out.print(i);
            out.print(" ");
            items.elementAt(i).print(out);
        }
    }
}

abstract class ConstInfo {
    int index;

    public ConstInfo(int i) { index = i; }

    public abstract int getTag();

    public String getClassName(ConstPool cp) { return null; }
    public void renameClass(ConstPool cp, String oldName, String newName, HashMap cache) {}
    public void renameClass(ConstPool cp, Map classnames, HashMap cache) {}
    public abstract int copy(ConstPool src, ConstPool dest, Map classnames);
                        // ** classnames is a mapping between JVM names.

    public abstract void write(DataOutputStream out) throws IOException;
    public abstract void print(PrintWriter out);

    public String toString() {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(bout);
        print(out);
        return bout.toString();
    }
}

/* padding following DoubleInfo or LongInfo.
 */
class ConstInfoPadding extends ConstInfo {
    public ConstInfoPadding(int i) { super(i); }

    public int getTag() { return 0; }

    public int copy(ConstPool src, ConstPool dest, Map map) {
        return dest.addConstInfoPadding();
    }

    public void write(DataOutputStream out) throws IOException {}

    public void print(PrintWriter out) {
        out.println("padding");
    }
}

class ClassInfo extends ConstInfo {
    static final int tag = 7;
    int name;

    public ClassInfo(int className, int index) {
        super(index);
        name = className;
    }

    public ClassInfo(DataInputStream in, int index) throws IOException {
        super(index);
        name = in.readUnsignedShort();
    }

    public int hashCode() { return name; }

    public boolean equals(Object obj) {
        return obj instanceof ClassInfo && ((ClassInfo)obj).name == name;
    }

    public int getTag() { return tag; }

    public String getClassName(ConstPool cp) {
        return cp.getUtf8Info(name);
    }

    public void renameClass(ConstPool cp, String oldName, String newName, HashMap cache) {
        String nameStr = cp.getUtf8Info(name);
        String newNameStr = null;
        if (nameStr.equals(oldName))
            newNameStr = newName;
        else if (nameStr.charAt(0) == '[') {
            String s = Descriptor.rename(nameStr, oldName, newName);
            if (nameStr != s)
                newNameStr = s;
        }

        if (newNameStr != null)
            if (cache == null)
                name = cp.addUtf8Info(newNameStr);
            else {
                cache.remove(this);
                name = cp.addUtf8Info(newNameStr);
                cache.put(this, this);
            }
    }

    public void renameClass(ConstPool cp, Map map, HashMap cache) {
        String oldName = cp.getUtf8Info(name);
        String newName = null;
        if (oldName.charAt(0) == '[') {
            String s = Descriptor.rename(oldName, map);
            if (oldName != s)
                newName = s;
        }
        else {
            String s = (String)map.get(oldName);
            if (s != null && !s.equals(oldName))
                newName = s;
        }

        if (newName != null) {
            if (cache == null)
                name = cp.addUtf8Info(newName);
            else {
                cache.remove(this);
                name = cp.addUtf8Info(newName);
                cache.put(this, this);
            }
        }
    }

    public int copy(ConstPool src, ConstPool dest, Map map) {
        String classname = src.getUtf8Info(name);
        if (map != null) {
            String newname = (String)map.get(classname);
            if (newname != null)
                classname = newname;
        }

        return dest.addClassInfo(classname);
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
        out.writeShort(name);
    }

    public void print(PrintWriter out) {
        out.print("Class #");
        out.println(name);
    }
}

class NameAndTypeInfo extends ConstInfo {
    static final int tag = 12;
    int memberName;
    int typeDescriptor;

    public NameAndTypeInfo(int name, int type, int index) {
        super(index);
        memberName = name;
        typeDescriptor = type;
    }

    public NameAndTypeInfo(DataInputStream in, int index) throws IOException {
        super(index);
        memberName = in.readUnsignedShort();
        typeDescriptor = in.readUnsignedShort();
    }

    public int hashCode() { return (memberName << 16) ^ typeDescriptor; }

    public boolean equals(Object obj) {
        if (obj instanceof NameAndTypeInfo) {
            NameAndTypeInfo nti = (NameAndTypeInfo)obj;
            return nti.memberName == memberName && nti.typeDescriptor == typeDescriptor;
        }
        else
            return false;
    }

    public int getTag() { return tag; }

    public void renameClass(ConstPool cp, String oldName, String newName, HashMap cache) {
        String type = cp.getUtf8Info(typeDescriptor);
        String type2 = Descriptor.rename(type, oldName, newName);
        if (type != type2)
            if (cache == null)
                typeDescriptor = cp.addUtf8Info(type2);
            else {
                cache.remove(this);
                typeDescriptor = cp.addUtf8Info(type2);
                cache.put(this, this);
            }
    }

    public void renameClass(ConstPool cp, Map map, HashMap cache) {
        String type = cp.getUtf8Info(typeDescriptor);
        String type2 = Descriptor.rename(type, map);
        if (type != type2)
            if (cache == null)
                typeDescriptor = cp.addUtf8Info(type2);
            else {
                cache.remove(this);
                typeDescriptor = cp.addUtf8Info(type2);
                cache.put(this, this);
            }
    }

    public int copy(ConstPool src, ConstPool dest, Map map) {
        String mname = src.getUtf8Info(memberName);
        String tdesc = src.getUtf8Info(typeDescriptor);
        tdesc = Descriptor.rename(tdesc, map);
        return dest.addNameAndTypeInfo(dest.addUtf8Info(mname),
                                       dest.addUtf8Info(tdesc));
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
        out.writeShort(memberName);
        out.writeShort(typeDescriptor);
    }

    public void print(PrintWriter out) {
        out.print("NameAndType #");
        out.print(memberName);
        out.print(", type #");
        out.println(typeDescriptor);
    }
}

abstract class MemberrefInfo extends ConstInfo {
    int classIndex;
    int nameAndTypeIndex;

    public MemberrefInfo(int cindex, int ntindex, int thisIndex) {
        super(thisIndex);
        classIndex = cindex;
        nameAndTypeIndex = ntindex;
    }

    public MemberrefInfo(DataInputStream in, int thisIndex) throws IOException {
        super(thisIndex);
        classIndex = in.readUnsignedShort();
        nameAndTypeIndex = in.readUnsignedShort();
    }

    public int hashCode() { return (classIndex << 16) ^ nameAndTypeIndex; }

    public boolean equals(Object obj) {
        if (obj instanceof MemberrefInfo) {
            MemberrefInfo mri = (MemberrefInfo)obj;
            return mri.classIndex == classIndex && mri.nameAndTypeIndex == nameAndTypeIndex
                   && mri.getClass() == this.getClass();
        }
        else
            return false;
    }

    public int copy(ConstPool src, ConstPool dest, Map map) {
        int classIndex2 = src.getItem(classIndex).copy(src, dest, map);
        int ntIndex2 = src.getItem(nameAndTypeIndex).copy(src, dest, map);
        return copy2(dest, classIndex2, ntIndex2);
    }

    abstract protected int copy2(ConstPool dest, int cindex, int ntindex);

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(getTag());
        out.writeShort(classIndex);
        out.writeShort(nameAndTypeIndex);
    }

    public void print(PrintWriter out) {
        out.print(getTagName() + " #");
        out.print(classIndex);
        out.print(", name&type #");
        out.println(nameAndTypeIndex);
    }

    public abstract String getTagName();
}

class FieldrefInfo extends MemberrefInfo {
    static final int tag = 9;

    public FieldrefInfo(int cindex, int ntindex, int thisIndex) {
        super(cindex, ntindex, thisIndex);
    }

    public FieldrefInfo(DataInputStream in, int thisIndex) throws IOException {
        super(in, thisIndex);
    }

    public int getTag() { return tag; }

    public String getTagName() { return "Field"; }

    protected int copy2(ConstPool dest, int cindex, int ntindex) {
        return dest.addFieldrefInfo(cindex, ntindex);
    }
}

class MethodrefInfo extends MemberrefInfo {
    static final int tag = 10;

    public MethodrefInfo(int cindex, int ntindex, int thisIndex) {
        super(cindex, ntindex, thisIndex);
    }

    public MethodrefInfo(DataInputStream in, int thisIndex) throws IOException {
        super(in, thisIndex);
    }

    public int getTag() { return tag; }

    public String getTagName() { return "Method"; }

    protected int copy2(ConstPool dest, int cindex, int ntindex) {
        return dest.addMethodrefInfo(cindex, ntindex);
    }
}

class InterfaceMethodrefInfo extends MemberrefInfo {
    static final int tag = 11;

    public InterfaceMethodrefInfo(int cindex, int ntindex, int thisIndex) {
        super(cindex, ntindex, thisIndex);
    }

    public InterfaceMethodrefInfo(DataInputStream in, int thisIndex) throws IOException {
        super(in, thisIndex);
    }

    public int getTag() { return tag; }

    public String getTagName() { return "Interface"; }

    protected int copy2(ConstPool dest, int cindex, int ntindex) {
        return dest.addInterfaceMethodrefInfo(cindex, ntindex);
    }
}

class StringInfo extends ConstInfo {
    static final int tag = 8;
    int string;

    public StringInfo(int str, int index) {
        super(index);
        string = str;
    }

    public StringInfo(DataInputStream in, int index) throws IOException {
        super(index);
        string = in.readUnsignedShort();
    }

    public int hashCode() { return string; }

    public boolean equals(Object obj) {
        return obj instanceof StringInfo && ((StringInfo)obj).string == string;
    }

    public int getTag() { return tag; }

    public int copy(ConstPool src, ConstPool dest, Map map) {
        return dest.addStringInfo(src.getUtf8Info(string));
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
        out.writeShort(string);
    }

    public void print(PrintWriter out) {
        out.print("String #");
        out.println(string);
    }
}

class IntegerInfo extends ConstInfo {
    static final int tag = 3;
    int value;

    public IntegerInfo(int v, int index) {
        super(index);
        value = v;
    }

    public IntegerInfo(DataInputStream in, int index) throws IOException {
        super(index);
        value = in.readInt();
    }

    public int hashCode() { return value; }

    public boolean equals(Object obj) {
        return obj instanceof IntegerInfo && ((IntegerInfo)obj).value == value;
    }

    public int getTag() { return tag; }

    public int copy(ConstPool src, ConstPool dest, Map map) {
        return dest.addIntegerInfo(value);
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
        out.writeInt(value);
    }

    public void print(PrintWriter out) {
        out.print("Integer ");
        out.println(value);
    }
}

class FloatInfo extends ConstInfo {
    static final int tag = 4;
    float value;

    public FloatInfo(float f, int index) {
        super(index);
        value = f;
    }

    public FloatInfo(DataInputStream in, int index) throws IOException {
        super(index);
        value = in.readFloat();
    }

    public int hashCode() { return Float.floatToIntBits(value); }

    public boolean equals(Object obj) {
        return obj instanceof FloatInfo && ((FloatInfo)obj).value == value;
    }

    public int getTag() { return tag; }

    public int copy(ConstPool src, ConstPool dest, Map map) {
        return dest.addFloatInfo(value);
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
        out.writeFloat(value);
    }

    public void print(PrintWriter out) {
        out.print("Float ");
        out.println(value);
    }
}

class LongInfo extends ConstInfo {
    static final int tag = 5;
    long value;

    public LongInfo(long l, int index) {
        super(index);
        value = l;
    }

    public LongInfo(DataInputStream in, int index) throws IOException {
        super(index);
        value = in.readLong();
    }

    public int hashCode() { return (int)(value ^ (value >>> 32)); }

    public boolean equals(Object obj) {
        return obj instanceof LongInfo && ((LongInfo)obj).value == value;
    }

    public int getTag() { return tag; }

    public int copy(ConstPool src, ConstPool dest, Map map) {
        return dest.addLongInfo(value);
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
        out.writeLong(value);
    }

    public void print(PrintWriter out) {
        out.print("Long ");
        out.println(value);
    }
}

class DoubleInfo extends ConstInfo {
    static final int tag = 6;
    double value;

    public DoubleInfo(double d, int index) {
        super(index);
        value = d;
    }

    public DoubleInfo(DataInputStream in, int index) throws IOException {
        super(index);
        value = in.readDouble();
    }

    public int hashCode() {
        long v = Double.doubleToLongBits(value);
        return (int)(v ^ (v >>> 32));
    }

    public boolean equals(Object obj) {
        return obj instanceof DoubleInfo && ((DoubleInfo)obj).value == value;
    }

    public int getTag() { return tag; }

    public int copy(ConstPool src, ConstPool dest, Map map) {
        return dest.addDoubleInfo(value);
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
        out.writeDouble(value);
    }

    public void print(PrintWriter out) {
        out.print("Double ");
        out.println(value);
    }
}

class Utf8Info extends ConstInfo {
    static final int tag = 1;
    String string;

    public Utf8Info(String utf8, int index) {
        super(index);
        string = utf8;
    }

    public Utf8Info(DataInputStream in, int index) throws IOException {
        super(index);
        string = in.readUTF();
    }

    public int hashCode() {
        return string.hashCode();
    }

    public boolean equals(Object obj) {
        return obj instanceof Utf8Info && ((Utf8Info)obj).string.equals(string);
    }

    public int getTag() { return tag; }

    public int copy(ConstPool src, ConstPool dest, Map map) {
        return dest.addUtf8Info(string);
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
        out.writeUTF(string);
    }

    public void print(PrintWriter out) {
        out.print("UTF8 \"");
        out.print(string);
        out.println("\"");
    }
}

class MethodHandleInfo extends ConstInfo {
    static final int tag = 15;
    int refKind, refIndex;

    public MethodHandleInfo(int kind, int referenceIndex, int index) {
        super(index);
        refKind = kind;
        refIndex = referenceIndex;
    }

    public MethodHandleInfo(DataInputStream in, int index) throws IOException {
        super(index);
        refKind = in.readUnsignedByte();
        refIndex = in.readUnsignedShort();
    }

    public int hashCode() { return (refKind << 16) ^ refIndex; }

    public boolean equals(Object obj) {
        if (obj instanceof MethodHandleInfo) {
            MethodHandleInfo mh = (MethodHandleInfo)obj;
            return mh.refKind == refKind && mh.refIndex == refIndex; 
        }
        else
            return false;
    }

    public int getTag() { return tag; }

    public int copy(ConstPool src, ConstPool dest, Map map) {
       return dest.addMethodHandleInfo(refKind,
                           src.getItem(refIndex).copy(src, dest, map));
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
        out.writeByte(refKind);
        out.writeShort(refIndex);
    }

    public void print(PrintWriter out) {
        out.print("MethodHandle #");
        out.print(refKind);
        out.print(", index #");
        out.println(refIndex);
    }
}

class MethodTypeInfo extends ConstInfo {
    static final int tag = 16;
    int descriptor;

    public MethodTypeInfo(int desc, int index) {
        super(index);
        descriptor = desc;
    }

    public MethodTypeInfo(DataInputStream in, int index) throws IOException {
        super(index);
        descriptor = in.readUnsignedShort();
    }

    public int hashCode() { return descriptor; }

    public boolean equals(Object obj) {
        if (obj instanceof MethodTypeInfo)
            return ((MethodTypeInfo)obj).descriptor == descriptor;
        else
            return false;
    }

    public int getTag() { return tag; }

    public void renameClass(ConstPool cp, String oldName, String newName, HashMap cache) {
        String desc = cp.getUtf8Info(descriptor);
        String desc2 = Descriptor.rename(desc, oldName, newName);
        if (desc != desc2)
            if (cache == null)
                descriptor = cp.addUtf8Info(desc2);
            else {
                cache.remove(this);
                descriptor = cp.addUtf8Info(desc2);
                cache.put(this, this);
            }
    }

    public void renameClass(ConstPool cp, Map map, HashMap cache) {
        String desc = cp.getUtf8Info(descriptor);
        String desc2 = Descriptor.rename(desc, map);
        if (desc != desc2)
            if (cache == null)
                descriptor = cp.addUtf8Info(desc2);
            else {
                cache.remove(this);
                descriptor = cp.addUtf8Info(desc2);
                cache.put(this, this);
            }
    }

    public int copy(ConstPool src, ConstPool dest, Map map) {
        String desc = src.getUtf8Info(descriptor);
        desc = Descriptor.rename(desc, map);
        return dest.addMethodTypeInfo(dest.addUtf8Info(desc));
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
        out.writeShort(descriptor);
    }

    public void print(PrintWriter out) {
        out.print("MethodType #");
        out.println(descriptor);
    }
}

class InvokeDynamicInfo extends ConstInfo {
    static final int tag = 18;
    int bootstrap, nameAndType;

    public InvokeDynamicInfo(int bootstrapMethod, int ntIndex, int index) {
        super(index);
        bootstrap = bootstrapMethod;
        nameAndType = ntIndex;
    }

    public InvokeDynamicInfo(DataInputStream in, int index) throws IOException {
        super(index);
        bootstrap = in.readUnsignedShort();
        nameAndType = in.readUnsignedShort();
    }

    public int hashCode() { return (bootstrap << 16) ^ nameAndType; }

    public boolean equals(Object obj) {
        if (obj instanceof InvokeDynamicInfo) {
            InvokeDynamicInfo iv = (InvokeDynamicInfo)obj;
            return iv.bootstrap == bootstrap && iv.nameAndType == nameAndType;
        }
        else
            return false;
    }

    public int getTag() { return tag; }

    public int copy(ConstPool src, ConstPool dest, Map map) {
       return dest.addInvokeDynamicInfo(bootstrap,
                           src.getItem(nameAndType).copy(src, dest, map));
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
        out.writeShort(bootstrap);
        out.writeShort(nameAndType);
    }

    public void print(PrintWriter out) {
        out.print("InvokeDynamic #");
        out.print(bootstrap);
        out.print(", name&type #");
        out.println(nameAndType);
    }
}
