/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2010 Shigeru Chiba. All Rights Reserved.
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

import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A quick class-file writer.  This is useful when a generated
 * class file is simple and the code generation should be fast.
 *
 * <p>Example:
 *
 * <blockquote><pre>
 * ClassFileWriter cfw = new ClassFileWriter(ClassFile.JAVA_4, 0);
 * ConstPoolWriter cpw = cfw.getConstPool();
 *
 * FieldWriter fw = cfw.getFieldWriter();
 * fw.add(AccessFlag.PUBLIC, "value", "I", null);
 * fw.add(AccessFlag.PUBLIC, "value2", "J", null);
 *
 * int thisClass = cpw.addClassInfo("sample/Test");
 * int superClass = cpw.addClassInfo("java/lang/Object");
 *
 * MethodWriter mw = cfw.getMethodWriter();
 *
 * mw.begin(AccessFlag.PUBLIC, MethodInfo.nameInit, "()V", null, null);
 * mw.add(Opcode.ALOAD_0);
 * mw.add(Opcode.INVOKESPECIAL);
 * int signature = cpw.addNameAndTypeInfo(MethodInfo.nameInit, "()V");
 * mw.add16(cpw.addMethodrefInfo(superClass, signature));
 * mw.add(Opcode.RETURN);
 * mw.codeEnd(1, 1);
 * mw.end(null, null);
 *
 * mw.begin(AccessFlag.PUBLIC, "one", "()I", null, null);
 * mw.add(Opcode.ICONST_1);
 * mw.add(Opcode.IRETURN);
 * mw.codeEnd(1, 1);
 * mw.end(null, null);
 *
 * byte[] classfile = cfw.end(AccessFlag.PUBLIC, thisClass, superClass,
 *                            null, null);
 * </pre></blockquote>
 *
 * <p>The code above generates the following class:
 *
 * <blockquote><pre>
 * package sample;
 * public class Test {
 *     public int value;
 *     public long value2;
 *     public Test() { super(); }
 *     public one() { return 1; }
 * }
 * </pre></blockquote>
 *
 * @since 3.13
 */
public class ClassFileWriter {
    private ByteStream output;
    private ConstPoolWriter constPool;
    private FieldWriter fields;
    private MethodWriter methods;
    int thisClass, superClass;

    /**
     * Constructs a class file writer.
     *
     * @param major     the major version ({@link ClassFile#JAVA_4}, {@link ClassFile#JAVA_5}, ...).
     * @param minor     the minor version (0 for JDK 1.3 and later).
     */
    public ClassFileWriter(int major, int minor) {
        output = new ByteStream(512);
        output.writeInt(0xCAFEBABE); // magic
        output.writeShort(minor);
        output.writeShort(major);
        constPool = new ConstPoolWriter(output);
        fields = new FieldWriter(constPool);
        methods = new MethodWriter(constPool);

    }

    /**
     * Returns a constant pool.
     */
    public ConstPoolWriter getConstPool() { return constPool; }

    /**
     * Returns a filed writer.
     */
    public FieldWriter getFieldWriter() { return fields; }

    /**
     * Returns a method writer.
     */
    public MethodWriter getMethodWriter() { return methods; }

    /**
     * Ends writing and returns the contents of the class file.
     *
     * @param accessFlags       access flags.
     * @param thisClass         this class.  an index indicating its <code>CONSTANT_Class_info</code>.
     * @param superClass        super class.  an index indicating its <code>CONSTANT_Class_info</code>.
     * @param interfaces        implemented interfaces.
     *                          index numbers indicating their <code>ClassInfo</code>.
     *                          It may be null.
     * @param aw        attributes of the class file.  May be null.
     *
     * @see AccessFlag
     */
    public byte[] end(int accessFlags, int thisClass, int superClass,
                      int[] interfaces, AttributeWriter aw) {
        constPool.end();
        output.writeShort(accessFlags);
        output.writeShort(thisClass);
        output.writeShort(superClass);
        if (interfaces == null)
            output.writeShort(0);
        else {
            int n = interfaces.length;
            output.writeShort(n);
            for (int i = 0; i < n; i++)
                output.writeShort(interfaces[i]);
        }

        output.enlarge(fields.dataSize() + methods.dataSize() + 6);
        try {
            output.writeShort(fields.size());
            fields.write(output);

            output.writeShort(methods.size());
            methods.write(output);
        }
        catch (IOException e) {}

        writeAttribute(output, aw, 0);
        return output.toByteArray();
    }

    /**
     * Ends writing and writes the contents of the class file into the
     * given output stream.
     *
     * @param accessFlags       access flags.
     * @param thisClass         this class.  an index indicating its <code>CONSTANT_Class_info</code>.
     * @param superClass        super class.  an index indicating its <code>CONSTANT_Class_info</code>.
     * @param interfaces        implemented interfaces.
     *                          index numbers indicating their <code>CONSTATNT_Class_info</code>.
     *                          It may be null.
     * @param aw        attributes of the class file.  May be null.
     *
     * @see AccessFlag
     */
    public void end(DataOutputStream out,
                    int accessFlags, int thisClass, int superClass,
                    int[] interfaces, AttributeWriter aw)
        throws IOException
    {
        constPool.end();
        output.writeTo(out);
        out.writeShort(accessFlags);
        out.writeShort(thisClass);
        out.writeShort(superClass);
        if (interfaces == null)
            out.writeShort(0);
        else {
            int n = interfaces.length;
            out.writeShort(n);
            for (int i = 0; i < n; i++)
                out.writeShort(interfaces[i]);
        }

        out.writeShort(fields.size());
        fields.write(out);

        out.writeShort(methods.size());
        methods.write(out);
        if (aw == null)
            out.writeShort(0);
        else {
            out.writeShort(aw.size());
            aw.write(out);
        }
    }

    /**
     * This writes attributes.
     *
     * <p>For example, the following object writes a synthetic attribute:
     *
     * <pre>
     * ConstPoolWriter cpw = ...;
     * final int tag = cpw.addUtf8Info("Synthetic");
     * AttributeWriter aw = new AttributeWriter() {
     *     public int size() {
     *         return 1;
     *     }
     *     public void write(DataOutputStream out) throws java.io.IOException {
     *         out.writeShort(tag);
     *         out.writeInt(0);
     *     }
     * };
     * </pre>
     */
    public static interface AttributeWriter {
        /**
         * Returns the number of attributes that this writer will
         * write.
         */
        public int size();

        /**
         * Writes all the contents of the attributes.  The binary representation
         * of the contents is an array of <code>attribute_info</code>.
         */
        public void write(DataOutputStream out) throws IOException;
    }

    static void writeAttribute(ByteStream bs, AttributeWriter aw, int attrCount) {
        if (aw == null) {
            bs.writeShort(attrCount);
            return;
        }

        bs.writeShort(aw.size() + attrCount);
        DataOutputStream dos = new DataOutputStream(bs);
        try {
            aw.write(dos);
            dos.flush();
        }
        catch (IOException e) {}
    }

    /**
     * Field.
     */
    public static final class FieldWriter {
        protected ByteStream output;
        protected ConstPoolWriter constPool;
        private int fieldCount;

        FieldWriter(ConstPoolWriter cp) {
            output = new ByteStream(128);
            constPool = cp;
            fieldCount = 0;
        }

        /**
         * Adds a new field.
         *
         * @param accessFlags       access flags.
         * @param name              the field name.
         * @param descriptor        the field type.
         * @param aw                the attributes of the field.  may be null.
         * @see AccessFlag
         */
        public void add(int accessFlags, String name, String descriptor, AttributeWriter aw) {
            int nameIndex = constPool.addUtf8Info(name);
            int descIndex = constPool.addUtf8Info(descriptor);
            add(accessFlags, nameIndex, descIndex, aw);
        }

        /**
         * Adds a new field.
         *
         * @param accessFlags       access flags.
         * @param name              the field name.  an index indicating its <code>CONSTANT_Utf8_info</code>.
         * @param descriptor        the field type.  an index indicating its <code>CONSTANT_Utf8_info</code>.
         * @param aw                the attributes of the field.  may be null.
         * @see AccessFlag
         */
        public void add(int accessFlags, int name, int descriptor, AttributeWriter aw) {
            ++fieldCount;
            output.writeShort(accessFlags);
            output.writeShort(name);
            output.writeShort(descriptor);
            writeAttribute(output, aw, 0);
        }

        int size() { return fieldCount; }

        int dataSize() { return output.size(); }

        /**
         * Writes the added fields.
         */
        void write(OutputStream out) throws IOException {
            output.writeTo(out);
        }
    }

    /**
     * Method.
     */
    public static final class MethodWriter {
        protected ByteStream output;
        protected ConstPoolWriter constPool;
        private int methodCount;
        protected int codeIndex;
        protected int throwsIndex;
        protected int stackIndex;

        private int startPos;
        private boolean isAbstract;
        private int catchPos;
        private int catchCount;

        MethodWriter(ConstPoolWriter cp) {
            output = new ByteStream(256);
            constPool = cp;
            methodCount = 0;
            codeIndex = 0;
            throwsIndex = 0;
            stackIndex = 0;
        }

        /**
         * Starts Adding a new method.
         *
         * @param accessFlags       access flags.
         * @param name              the method name.
         * @param descriptor        the method signature.
         * @param exceptions        throws clause.  It may be null.
         *                          The class names must be the JVM-internal
         *                          representations like <code>java/lang/Exception</code>.
         * @param aw                attributes to the <code>Method_info</code>.                         
         */
        public void begin(int accessFlags, String name, String descriptor,
                        String[] exceptions, AttributeWriter aw) {
            int nameIndex = constPool.addUtf8Info(name);
            int descIndex = constPool.addUtf8Info(descriptor);
            int[] intfs;
            if (exceptions == null)
                intfs = null;
            else
                intfs = constPool.addClassInfo(exceptions);

            begin(accessFlags, nameIndex, descIndex, intfs, aw);
        }

        /**
         * Starts adding a new method.
         *
         * @param accessFlags       access flags.
         * @param name              the method name.  an index indicating its <code>CONSTANT_Utf8_info</code>.
         * @param descriptor        the field type.  an index indicating its <code>CONSTANT_Utf8_info</code>.
         * @param exceptions        throws clause.  indexes indicating <code>CONSTANT_Class_info</code>s.
         *                          It may be null.
         * @param aw                attributes to the <code>Method_info</code>.                         
         */
        public void begin(int accessFlags, int name, int descriptor, int[] exceptions, AttributeWriter aw) {
            ++methodCount;
            output.writeShort(accessFlags);
            output.writeShort(name);
            output.writeShort(descriptor);
            isAbstract = (accessFlags & AccessFlag.ABSTRACT) != 0;

            int attrCount = isAbstract ? 0 : 1;
            if (exceptions != null)
                ++attrCount;

            writeAttribute(output, aw, attrCount);

            if (exceptions != null)
                writeThrows(exceptions);

            if (!isAbstract) {
                if (codeIndex == 0)
                    codeIndex = constPool.addUtf8Info(CodeAttribute.tag);

                startPos = output.getPos();
                output.writeShort(codeIndex);
                output.writeBlank(12);   // attribute_length, maxStack, maxLocals, code_lenth
            }

            catchPos = -1;
            catchCount = 0;
        }

        private void writeThrows(int[] exceptions) {
            if (throwsIndex == 0)
                throwsIndex = constPool.addUtf8Info(ExceptionsAttribute.tag);

            output.writeShort(throwsIndex);
            output.writeInt(exceptions.length * 2 + 2);
            output.writeShort(exceptions.length);
            for (int i = 0; i < exceptions.length; i++)
                output.writeShort(exceptions[i]);
        }

        /**
         * Appends an 8bit value of bytecode.
         *
         * @see Opcode
         */
        public void add(int b) {
            output.write(b);
        }

        /**
         * Appends a 16bit value of bytecode.
         */
        public void add16(int b) {
            output.writeShort(b);
        }

        /**
         * Appends a 32bit value of bytecode.
         */
        public void add32(int b) {
            output.writeInt(b);
        }

        /**
         * Appends a invokevirtual, inovkespecial, or invokestatic bytecode.
         *
         * @see Opcode
         */
        public void addInvoke(int opcode, String targetClass, String methodName,
                              String descriptor) {
            int target = constPool.addClassInfo(targetClass);
            int nt = constPool.addNameAndTypeInfo(methodName, descriptor);
            int method = constPool.addMethodrefInfo(target, nt);
            add(opcode);
            add16(method);
        }

        /**
         * Ends appending bytecode.
         */
        public void codeEnd(int maxStack, int maxLocals) {
            if (!isAbstract) {
                output.writeShort(startPos + 6, maxStack);
                output.writeShort(startPos + 8, maxLocals);
                output.writeInt(startPos + 10, output.getPos() - startPos - 14);  // code_length
                catchPos = output.getPos();
                catchCount = 0;
                output.writeShort(0);   // number of catch clauses
            }
        }

        /**
         * Appends an <code>exception_table</code> entry to the
         * <code>Code_attribute</code>.  This method is available
         * only after the <code>codeEnd</code> method is called.
         *
         * @param catchType     an index indicating a <code>CONSTANT_Class_info</code>.
         */
        public void addCatch(int startPc, int endPc, int handlerPc, int catchType) {
            ++catchCount;
            output.writeShort(startPc);
            output.writeShort(endPc);
            output.writeShort(handlerPc);
            output.writeShort(catchType);
        }

        /**
         * Ends adding a new method.  The <code>add</code> method must be
         * called before the <code>end</code> method is called.
         *
         * @param smap              a stack map table.  may be null.
         * @param aw                attributes to the <code>Code_attribute</code>.
         *                          may be null.
         */
        public void end(StackMapTable.Writer smap, AttributeWriter aw) {
            if (isAbstract)
                return;

            // exception_table_length
            output.writeShort(catchPos, catchCount);

            int attrCount = smap == null ? 0 : 1;
            writeAttribute(output, aw, attrCount);

            if (smap != null) {
                if (stackIndex == 0)
                    stackIndex = constPool.addUtf8Info(StackMapTable.tag);

                output.writeShort(stackIndex);
                byte[] data = smap.toByteArray();
                output.writeInt(data.length);
                output.write(data);
            }

            // Code attribute_length
            output.writeInt(startPos + 2, output.getPos() - startPos - 6);
        }

        int size() { return methodCount; }

        int dataSize() { return output.size(); }

        /**
         * Writes the added methods.
         */
        void write(OutputStream out) throws IOException {
            output.writeTo(out);
        }
    }

    /**
     * Constant Pool.
     */
    public static final class ConstPoolWriter {
        ByteStream output;
        protected int startPos;
        protected int num;

        ConstPoolWriter(ByteStream out) {
            output = out;
            startPos = out.getPos();
            num = 1;
            output.writeShort(1);   // number of entries
        }

        /**
         * Makes <code>CONSTANT_Class_info</code> objects for each class name.
         *
         * @return an array of indexes indicating <code>CONSTANT_Class_info</code>s.
         */
        public int[] addClassInfo(String[] classNames) {
            int n = classNames.length;
            int[] result = new int[n];
            for (int i = 0; i < n; i++)
                result[i] = addClassInfo(classNames[i]);

            return result;
        }

        /**
         * Adds a new <code>CONSTANT_Class_info</code> structure.
         *
         * <p>This also adds a <code>CONSTANT_Utf8_info</code> structure
         * for storing the class name.
         *
         * @param jvmname   the JVM-internal representation of a class name.
         *                  e.g. <code>java/lang/Object</code>.
         * @return          the index of the added entry.
         */
        public int addClassInfo(String jvmname) {
            int utf8 = addUtf8Info(jvmname);
            output.write(ClassInfo.tag);
            output.writeShort(utf8);
            return num++;
        }

        /**
         * Adds a new <code>CONSTANT_Class_info</code> structure.
         *
         * @param name      <code>name_index</code>
         * @return          the index of the added entry.
         */
        public int addClassInfo(int name) {
            output.write(ClassInfo.tag);
            output.writeShort(name);
            return num++;
        }

        /**
         * Adds a new <code>CONSTANT_NameAndType_info</code> structure.
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
            output.write(NameAndTypeInfo.tag);
            output.writeShort(name);
            output.writeShort(type);
            return num++;
        }

        /**
         * Adds a new <code>CONSTANT_Fieldref_info</code> structure.
         *
         * @param classInfo         <code>class_index</code>
         * @param nameAndTypeInfo   <code>name_and_type_index</code>.
         * @return          the index of the added entry.
         */
        public int addFieldrefInfo(int classInfo, int nameAndTypeInfo) {
            output.write(FieldrefInfo.tag);
            output.writeShort(classInfo);
            output.writeShort(nameAndTypeInfo);
            return num++;
        }

        /**
         * Adds a new <code>CONSTANT_Methodref_info</code> structure.
         *
         * @param classInfo         <code>class_index</code>
         * @param nameAndTypeInfo   <code>name_and_type_index</code>.
         * @return          the index of the added entry.
         */
        public int addMethodrefInfo(int classInfo, int nameAndTypeInfo) {
            output.write(MethodrefInfo.tag);
            output.writeShort(classInfo);
            output.writeShort(nameAndTypeInfo);
            return num++;
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
            output.write(InterfaceMethodrefInfo.tag);
            output.writeShort(classInfo);
            output.writeShort(nameAndTypeInfo);
            return num++;
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
            int utf8 = addUtf8Info(str);
            output.write(StringInfo.tag);
            output.writeShort(utf8);
            return num++;
        }

        /**
         * Adds a new <code>CONSTANT_Integer_info</code>
         * structure.
         *
         * @return          the index of the added entry.
         */
        public int addIntegerInfo(int i) {
            output.write(IntegerInfo.tag);
            output.writeInt(i);
            return num++;
        }

        /**
         * Adds a new <code>CONSTANT_Float_info</code>
         * structure.
         *
         * @return          the index of the added entry.
         */
        public int addFloatInfo(float f) {
            output.write(FloatInfo.tag);
            output.writeFloat(f);
            return num++;
        }

        /**
         * Adds a new <code>CONSTANT_Long_info</code>
         * structure.
         *
         * @return          the index of the added entry.
         */
        public int addLongInfo(long l) {
            output.write(LongInfo.tag);
            output.writeLong(l);
            int n = num;
            num += 2;
            return n;
        }

        /**
         * Adds a new <code>CONSTANT_Double_info</code>
         * structure.
         *
         * @return          the index of the added entry.
         */
        public int addDoubleInfo(double d) {
            output.write(DoubleInfo.tag);
            output.writeDouble(d);
            int n = num;
            num += 2;
            return n;
        }

        /**
         * Adds a new <code>CONSTANT_Utf8_info</code>
         * structure.
         *
         * @return          the index of the added entry.
         */
        public int addUtf8Info(String utf8) {
            output.write(Utf8Info.tag);
            output.writeUTF(utf8);
            return num++;
        }

        /**
         * Writes the contents of this class pool.
         */
        void end() {
            output.writeShort(startPos, num);
        }
    }
}
