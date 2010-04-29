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
    HashMap classes;
    HashMap strings;
    ConstInfo[] constInfoCache;
    int[] constInfoIndexCache;
    int thisClassInfo;

    private static final int CACHE_SIZE = 32;

    /**
     * A hash function for CACHE_SIZE
     */
    private static int hashFunc(int a, int b) {
        int h = -2128831035;
        final int prime = 16777619;
        h = (h ^ (a & 0xff)) * prime;
        h = (h ^ (b & 0xff)) * prime;

        // changing the hash key size from 32bit to 5bit
        h = (h >> 5) ^ (h & 0x1f);
        return h & 0x1f;    // 0..31
    }

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
     * Represents the class using this constant pool table.
     */
    public static final CtClass THIS = null;

    /**
     * Constructs a constant pool table.
     *
     * @param thisclass         the name of the class using this constant
     *                          pool table
     */
    public ConstPool(String thisclass) {
        items = new LongVector();
        numOfItems = 0;
        addItem(null);          // index 0 is reserved by the JVM.
        classes = new HashMap();
        strings = new HashMap();
        constInfoCache = new ConstInfo[CACHE_SIZE];
        constInfoIndexCache = new int[CACHE_SIZE];
        thisClassInfo = addClassInfo(thisclass);
    }

    /**
     * Constructs a constant pool table from the given byte stream.
     *
     * @param in        byte stream.
     */
    public ConstPool(DataInputStream in) throws IOException {
        classes = new HashMap();
        strings = new HashMap();
        constInfoCache = new ConstInfo[CACHE_SIZE];
        constInfoIndexCache = new int[CACHE_SIZE];
        thisClassInfo = 0;
        /* read() initializes items and numOfItems, and do addItem(null).
         */
        read(in);
    }

    void prune() {
        classes = new HashMap();
        strings = new HashMap();
        constInfoCache = new ConstInfo[CACHE_SIZE];
        constInfoIndexCache = new int[CACHE_SIZE];
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
     *          <code>[java.lang.Object;</code> (note that the separators
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
        MethodrefInfo minfo = (MethodrefInfo)getItem(index);
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
        MethodrefInfo minfo = (MethodrefInfo)getItem(index);
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
        MethodrefInfo minfo = (MethodrefInfo)getItem(index);
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
        MethodrefInfo minfo = (MethodrefInfo)getItem(index);
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
        MethodrefInfo minfo = (MethodrefInfo)getItem(index);
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
        InterfaceMethodrefInfo minfo
            = (InterfaceMethodrefInfo)getItem(index);
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
        InterfaceMethodrefInfo minfo
            = (InterfaceMethodrefInfo)getItem(index);
        return getClassInfo(minfo.classIndex);
    }

    /**
     * Reads the <code>name_and_type_index</code> field of the
     * <code>CONSTANT_InterfaceMethodref_info</code> structure
     * at the given index.
     */
    public int getInterfaceMethodrefNameAndType(int index) {
        InterfaceMethodrefInfo minfo
            = (InterfaceMethodrefInfo)getItem(index);
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
        InterfaceMethodrefInfo minfo
            = (InterfaceMethodrefInfo)getItem(index);
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
        InterfaceMethodrefInfo minfo
            = (InterfaceMethodrefInfo)getItem(index);
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
            value = new Float(getFloatInfo(index));
        else if (constInfo instanceof IntegerInfo)
            value = new Integer(getIntegerInfo(index));
        else if (constInfo instanceof LongInfo)
            value = new Long(getLongInfo(index));
        else if (constInfo instanceof DoubleInfo)
            value = new Double(getDoubleInfo(index));
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

    private int addItem(ConstInfo info) {
        items.addElement(info);
        return numOfItems++;
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
        return addItem(new ConstInfoPadding());
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
        ClassInfo info = (ClassInfo)classes.get(qname);
        if (info != null)
            return info.index;
        else {
            int utf8 = addUtf8Info(Descriptor.toJvmName(qname));
            info = new ClassInfo(utf8, numOfItems);
            classes.put(qname, info);
            return addItem(info);
        }
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
        int h = hashFunc(name, type);
        ConstInfo ci = constInfoCache[h];
        if (ci != null && ci instanceof NameAndTypeInfo && ci.hashCheck(name, type))
            return constInfoIndexCache[h];
        else {
            NameAndTypeInfo item = new NameAndTypeInfo(name, type);
            constInfoCache[h] = item;
            int i = addItem(item);
            constInfoIndexCache[h] = i;
            return i;
        }
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
        int h = hashFunc(classInfo, nameAndTypeInfo);
        ConstInfo ci = constInfoCache[h];
        if (ci != null && ci instanceof FieldrefInfo && ci.hashCheck(classInfo, nameAndTypeInfo))
            return constInfoIndexCache[h];
        else {
            FieldrefInfo item = new FieldrefInfo(classInfo, nameAndTypeInfo);
            constInfoCache[h] = item;
            int i = addItem(item);
            constInfoIndexCache[h] = i;
            return i;
        }
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
        int h = hashFunc(classInfo, nameAndTypeInfo);
        ConstInfo ci = constInfoCache[h];
        if (ci != null && ci instanceof MethodrefInfo && ci.hashCheck(classInfo, nameAndTypeInfo))
            return constInfoIndexCache[h];
        else {
            MethodrefInfo item = new MethodrefInfo(classInfo, nameAndTypeInfo);
            constInfoCache[h] = item;
            int i = addItem(item);
            constInfoIndexCache[h] = i;
            return i;
        }
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
        int h = hashFunc(classInfo, nameAndTypeInfo);
        ConstInfo ci = constInfoCache[h];
        if (ci != null && ci instanceof InterfaceMethodrefInfo && ci.hashCheck(classInfo, nameAndTypeInfo))
            return constInfoIndexCache[h];
        else {
            InterfaceMethodrefInfo item =new InterfaceMethodrefInfo(classInfo, nameAndTypeInfo); 
            constInfoCache[h] = item;
            int i = addItem(item);
            constInfoIndexCache[h] = i;
            return i;
        }
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
        return addItem(new StringInfo(addUtf8Info(str)));
    }

    /**
     * Adds a new <code>CONSTANT_Integer_info</code>
     * structure.
     *
     * @return          the index of the added entry.
     */
    public int addIntegerInfo(int i) {
        return addItem(new IntegerInfo(i));
    }

    /**
     * Adds a new <code>CONSTANT_Float_info</code>
     * structure.
     *
     * @return          the index of the added entry.
     */
    public int addFloatInfo(float f) {
        return addItem(new FloatInfo(f));
    }

    /**
     * Adds a new <code>CONSTANT_Long_info</code>
     * structure.
     *
     * @return          the index of the added entry.
     */
    public int addLongInfo(long l) {
        int i = addItem(new LongInfo(l));
        addItem(new ConstInfoPadding());
        return i;
    }

    /**
     * Adds a new <code>CONSTANT_Double_info</code>
     * structure.
     *
     * @return          the index of the added entry.
     */
    public int addDoubleInfo(double d) {
        int i = addItem(new DoubleInfo(d));
        addItem(new ConstInfoPadding());
        return i;
    }

    /**
     * Adds a new <code>CONSTANT_Utf8_info</code>
     * structure.
     *
     * <p>If the given utf8 string has been already recorded in the
     * table, then this method does not add a new entry to avoid adding
     * a duplicated entry.
     * Instead, it returns the index of the entry already recorded.
     *
     * @return          the index of the added entry.
     */
    public int addUtf8Info(String utf8) {
        Utf8Info info = (Utf8Info)strings.get(utf8);
        if (info != null)
            return info.index;
        else {
            info = new Utf8Info(utf8, numOfItems);
            strings.put(utf8, info);
            return addItem(info);
        }
    }

    /**
     * Get all the class names.
     *
     * @return a set of class names
     */
    public Set getClassNames()
    {
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
        classes = new HashMap(classes.size() * 2);
        for (int i = 1; i < size; ++i) {
            ConstInfo ci = v.elementAt(i);
            ci.renameClass(this, oldName, newName);
            ci.makeHashtable(this);
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
        classes = new HashMap(classes.size() * 2);
        for (int i = 1; i < size; ++i) {
            ConstInfo ci = v.elementAt(i);
            ci.renameClass(this, classnames);
            ci.makeHashtable(this);
        }
    }

    private void read(DataInputStream in) throws IOException {
        int n = in.readUnsignedShort();

        items = new LongVector(n);
        numOfItems = 0;
        addItem(null);          // index 0 is reserved by the JVM.

        while (--n > 0) {       // index 0 is reserved by JVM
            int tag = readOne(in);
            if ((tag == LongInfo.tag) || (tag == DoubleInfo.tag)) {
                addItem(new ConstInfoPadding());
                --n;
            }
        }

        int i = 1;
        while (true) {
            ConstInfo info = items.elementAt(i++);
            if (info == null)
                break;
            else
                info.makeHashtable(this);
        }
    }

    private int readOne(DataInputStream in) throws IOException {
        ConstInfo info;
        int tag = in.readUnsignedByte();
        switch (tag) {
        case Utf8Info.tag :                     // 1
            info = new Utf8Info(in, numOfItems);
            strings.put(((Utf8Info)info).string, info);
            break;
        case IntegerInfo.tag :                  // 3
            info = new IntegerInfo(in);
            break;
        case FloatInfo.tag :                    // 4
            info = new FloatInfo(in);
            break;
        case LongInfo.tag :                     // 5
            info = new LongInfo(in);
            break;
        case DoubleInfo.tag :                   // 6
            info = new DoubleInfo(in);
            break;
        case ClassInfo.tag :                    // 7
            info = new ClassInfo(in, numOfItems);
            // classes.put(<classname>, info);
            break;
        case StringInfo.tag :                   // 8
            info = new StringInfo(in);
            break;
        case FieldrefInfo.tag :                 // 9
            info = new FieldrefInfo(in);
            break;
        case MethodrefInfo.tag :                // 10
            info = new MethodrefInfo(in);
            break;
        case InterfaceMethodrefInfo.tag :       // 11
            info = new InterfaceMethodrefInfo(in);
            break;
        case NameAndTypeInfo.tag :              // 12
            info = new NameAndTypeInfo(in);
            break;
        default :
            throw new IOException("invalid constant type: " + tag);
        }

        addItem(info);
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
    public abstract int getTag();

    public String getClassName(ConstPool cp) { return null; }
    public void renameClass(ConstPool cp, String oldName, String newName) {}
    public void renameClass(ConstPool cp, Map classnames) {}
    public abstract int copy(ConstPool src, ConstPool dest, Map classnames);
                        // ** classnames is a mapping between JVM names.

    public abstract void write(DataOutputStream out) throws IOException;
    public abstract void print(PrintWriter out);

    void makeHashtable(ConstPool cp) {}     // called after read() finishes in ConstPool.

    boolean hashCheck(int a, int b) { return false; }

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
    int index;

    public ClassInfo(int className, int i) {
        name = className;
        index = i;
    }

    public ClassInfo(DataInputStream in, int i) throws IOException {
        name = in.readUnsignedShort();
        index = i;
    }

    public int getTag() { return tag; }

    public String getClassName(ConstPool cp) {
        return cp.getUtf8Info(name);
    };

    public void renameClass(ConstPool cp, String oldName, String newName) {
        String nameStr = cp.getUtf8Info(name);
        if (nameStr.equals(oldName))
            name = cp.addUtf8Info(newName);
        else if (nameStr.charAt(0) == '[') {
            String nameStr2 = Descriptor.rename(nameStr, oldName, newName);
            if (nameStr != nameStr2)
                name = cp.addUtf8Info(nameStr2);
        }
    }

    public void renameClass(ConstPool cp, Map map) {
        String oldName = cp.getUtf8Info(name);
        if (oldName.charAt(0) == '[') {
            String newName = Descriptor.rename(oldName, map);
            if (oldName != newName)
                name = cp.addUtf8Info(newName);
        }
        else {
            String newName = (String)map.get(oldName);
            if (newName != null && !newName.equals(oldName))
                name = cp.addUtf8Info(newName);
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

    void makeHashtable(ConstPool cp) {
        String name = Descriptor.toJavaName(getClassName(cp));
        cp.classes.put(name, this);
    }
}

class NameAndTypeInfo extends ConstInfo {
    static final int tag = 12;
    int memberName;
    int typeDescriptor;

    public NameAndTypeInfo(int name, int type) {
        memberName = name;
        typeDescriptor = type;
    }

    public NameAndTypeInfo(DataInputStream in) throws IOException {
        memberName = in.readUnsignedShort();
        typeDescriptor = in.readUnsignedShort();
    }

    boolean hashCheck(int a, int b) { return a == memberName && b == typeDescriptor; }

    public int getTag() { return tag; }

    public void renameClass(ConstPool cp, String oldName, String newName) {
        String type = cp.getUtf8Info(typeDescriptor);
        String type2 = Descriptor.rename(type, oldName, newName);
        if (type != type2)
            typeDescriptor = cp.addUtf8Info(type2);
    }

    public void renameClass(ConstPool cp, Map map) {
        String type = cp.getUtf8Info(typeDescriptor);
        String type2 = Descriptor.rename(type, map);
        if (type != type2)
            typeDescriptor = cp.addUtf8Info(type2);
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

    public MemberrefInfo(int cindex, int ntindex) {
        classIndex = cindex;
        nameAndTypeIndex = ntindex;
    }

    public MemberrefInfo(DataInputStream in) throws IOException {
        classIndex = in.readUnsignedShort();
        nameAndTypeIndex = in.readUnsignedShort();
    }

    public int copy(ConstPool src, ConstPool dest, Map map) {
        int classIndex2 = src.getItem(classIndex).copy(src, dest, map);
        int ntIndex2 = src.getItem(nameAndTypeIndex).copy(src, dest, map);
        return copy2(dest, classIndex2, ntIndex2);
    }

    boolean hashCheck(int a, int b) { return a == classIndex && b == nameAndTypeIndex; }

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

    public FieldrefInfo(int cindex, int ntindex) {
        super(cindex, ntindex);
    }

    public FieldrefInfo(DataInputStream in) throws IOException {
        super(in);
    }

    public int getTag() { return tag; }

    public String getTagName() { return "Field"; }

    protected int copy2(ConstPool dest, int cindex, int ntindex) {
        return dest.addFieldrefInfo(cindex, ntindex);
    }
}

class MethodrefInfo extends MemberrefInfo {
    static final int tag = 10;

    public MethodrefInfo(int cindex, int ntindex) {
        super(cindex, ntindex);
    }

    public MethodrefInfo(DataInputStream in) throws IOException {
        super(in);
    }

    public int getTag() { return tag; }

    public String getTagName() { return "Method"; }

    protected int copy2(ConstPool dest, int cindex, int ntindex) {
        return dest.addMethodrefInfo(cindex, ntindex);
    }
}

class InterfaceMethodrefInfo extends MemberrefInfo {
    static final int tag = 11;

    public InterfaceMethodrefInfo(int cindex, int ntindex) {
        super(cindex, ntindex);
    }

    public InterfaceMethodrefInfo(DataInputStream in) throws IOException {
        super(in);
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

    public StringInfo(int str) {
        string = str;
    }

    public StringInfo(DataInputStream in) throws IOException {
        string = in.readUnsignedShort();
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

    public IntegerInfo(int i) {
        value = i;
    }

    public IntegerInfo(DataInputStream in) throws IOException {
        value = in.readInt();
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

    public FloatInfo(float f) {
        value = f;
    }

    public FloatInfo(DataInputStream in) throws IOException {
        value = in.readFloat();
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

    public LongInfo(long l) {
        value = l;
    }

    public LongInfo(DataInputStream in) throws IOException {
        value = in.readLong();
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

    public DoubleInfo(double d) {
        value = d;
    }

    public DoubleInfo(DataInputStream in) throws IOException {
        value = in.readDouble();
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
    int index;

    public Utf8Info(String utf8, int i) {
        string = utf8;
        index = i;
    }

    public Utf8Info(DataInputStream in, int i) throws IOException {
        string = in.readUTF();
        index = i;
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
