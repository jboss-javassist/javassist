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

import javassist.CtClass;
import javassist.CtPrimitiveType;

class ByteVector implements Cloneable {
    private byte[] buffer;
    private int size;

    public ByteVector() {
        buffer = new byte[64];
        size = 0;
    }

    public Object clone() throws CloneNotSupportedException {
        ByteVector bv = (ByteVector)super.clone();
        bv.buffer = (byte[])buffer.clone();
        return bv;
    }

    public final int getSize() { return size; }

    public final byte[] copy() {
        byte[] b = new byte[size];
        System.arraycopy(buffer, 0, b, 0, size);
        return b;
    }

    public int read(int offset) {
        if (offset < 0 || size <= offset)
            throw new ArrayIndexOutOfBoundsException(offset);

        return buffer[offset];
    }

    public void write(int offset, int value) {
        if (offset < 0 || size <= offset)
            throw new ArrayIndexOutOfBoundsException(offset);

        buffer[offset] = (byte)value;
    }

    public void add(int code) {
        addGap(1);
        buffer[size - 1] = (byte)code;
    }

    public void add(int b1, int b2) {
        addGap(2);
        buffer[size - 2] = (byte)b1;
        buffer[size - 1] = (byte)b2;
    }

    public void add(int b1, int b2, int b3, int b4) {
        addGap(4);
        buffer[size - 4] = (byte)b1;
        buffer[size - 3] = (byte)b2;
        buffer[size - 2] = (byte)b3;
        buffer[size - 1] = (byte)b4;
    }

    public void addGap(int length) {
        if (size + length > buffer.length) {
            int newSize = size << 1;
            if (newSize < size + length)
                newSize = size + length;

            byte[] newBuf = new byte[newSize];
            System.arraycopy(buffer, 0, newBuf, 0, size);
            buffer = newBuf;
        }

        size += length;
    }
}

/**
 * A utility class for producing a bytecode sequence.
 *
 * <p>A <code>Bytecode</code> object is an unbounded array
 * containing bytecode.  For example,
 *
 * <ul><pre>ConstPool cp = ...;    // constant pool table
 * Bytecode b = new Bytecode(cp, 1, 0);
 * b.addIconst(3);
 * b.addReturn(CtClass.intType);
 * CodeAttribute ca = b.toCodeAttribute();</ul></pre>
 *
 * <p>This program produces a Code attribute including a bytecode
 * sequence:
 *
 * <ul><pre>iconst_3
 * ireturn</pre></ul>
 *
 * @see ConstPool
 * @see CodeAttribute
 */
public class Bytecode extends ByteVector implements Cloneable, Opcode {
    /**
     * Represents the <code>CtClass</code> file using the
     * constant pool table given to this <code>Bytecode</code> object.
     */
    public static final CtClass THIS = ConstPool.THIS;

    ConstPool constPool;
    int maxStack, maxLocals;
    ExceptionTable tryblocks;
    private int stackDepth;

    /**
     * Constructs a <code>Bytecode</code> object with an empty bytecode
     * sequence.
     *
     * <p>The parameters <code>stacksize</code> and <code>localvars</code>
     * specify initial values
     * of <code>max_stack</code> and <code>max_locals</code>.
     * They can be changed later.
     *
     * @param cp                constant pool table.
     * @param stacksize         <code>max_stack</code>.
     * @param localvars         <code>max_locals</code>.
     */
    public Bytecode(ConstPool cp, int stacksize, int localvars) {
        constPool = cp;
        maxStack = stacksize;
        maxLocals = localvars;
        tryblocks = new ExceptionTable(cp);
        stackDepth = 0;
    }

    /**
     * Constructs a <code>Bytecode</code> object with an empty bytecode
     * sequence.  The initial values of <code>max_stack</code> and
     * <code>max_locals</code> are zero.
     * 
     * @param cp            constant pool table.
     * @see Bytecode#setMaxStack(int)
     * @see Bytecode#setMaxLocals(int)
     */
    public Bytecode(ConstPool cp) {
        this(cp, 0, 0);
    }

    /**
     * Creates and returns a copy of this object.
     * The constant pool object is shared between this object
     * and the cloned object.
     */
    public Object clone() {
        try {
            Bytecode bc = (Bytecode)super.clone();
            bc.tryblocks = (ExceptionTable)tryblocks.clone();
            return bc;
        }
        catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }

    /**
     * Gets a constant pool table.
     */
    public ConstPool getConstPool() { return constPool; }

    /**
     * Returns <code>exception_table</code>.
     */
    public ExceptionTable getExceptionTable() { return tryblocks; }

    /**
     * Converts to a <code>CodeAttribute</code>.
     */
    public CodeAttribute toCodeAttribute() {
        return new CodeAttribute(constPool, maxStack, maxLocals,
                                 get(), tryblocks);
    }

    /**
     * Returns the length of the bytecode sequence.
     */
    public int length() {
        return getSize();
    }

    /**
     * Returns the produced bytecode sequence.
     */
    public byte[] get() {
        return copy();
    }

    /**
     * Gets <code>max_stack</code>.
     */
    public int getMaxStack() { return maxStack; }

    /**
     * Sets <code>max_stack</code>.
     *
     * <p>This value may be automatically updated when an instruction
     * is appended.  A <code>Bytecode</code> object maintains the current
     * stack depth whenever an instruction is added
     * by <code>addOpcode()</code>.  For example, if DUP is appended,
     * the current stack depth is increased by one.  If the new stack
     * depth is more than <code>max_stack</code>, then it is assigned
     * to <code>max_stack</code>.  However, if branch instructions are
     * appended, the current stack depth may not be correctly maintained.
     *
     * @see #addOpcode(int)
     */
    public void setMaxStack(int size) {
        maxStack = size;
    }

    /**
     * Gets <code>max_locals</code>.
     */
    public int getMaxLocals() { return maxLocals; }

    /**
     * Sets <code>max_locals</code>.
     */
    public void setMaxLocals(int size) {
        maxLocals = size;
    }

    /**
     * Sets <code>max_locals</code>.
     *
     * <p>This computes the number of local variables
     * used to pass method parameters and sets <code>max_locals</code>
     * to that number plus <code>locals</code>.
     *
     * @param isStatic          true if <code>params</code> must be
     *                          interpreted as parameters to a static method.
     * @param params            parameter types.
     * @param locals            the number of local variables excluding
     *                          ones used to pass parameters.
     */
    public void setMaxLocals(boolean isStatic, CtClass[] params,
                             int locals) {
        if (!isStatic)
            ++locals;

        if (params != null) {
            CtClass doubleType = CtClass.doubleType;
            CtClass longType = CtClass.longType;
            int n = params.length;
            for (int i = 0; i < n; ++i) {
                CtClass type = params[i];
                if (type == doubleType || type == longType)
                    locals += 2;
                else
                    ++locals;
            }
        }

        maxLocals = locals;
    }

    /**
     * Increments <code>max_locals</code>.
     */
    public void incMaxLocals(int diff) {
        maxLocals += diff;
    }

    /**
     * Adds a new entry of <code>exception_table</code>.
     */
    public void addExceptionHandler(int start, int end,
                                    int handler, CtClass type) {
        addExceptionHandler(start, end, handler,
                            constPool.addClassInfo(type));
    }

    /**
     * Adds a new entry of <code>exception_table</code>.
     *
     * @param type      the fully-qualified name of a throwable class.
     */
    public void addExceptionHandler(int start, int end,
                                    int handler, String type) {
        addExceptionHandler(start, end, handler,
                            constPool.addClassInfo(type));
    }

    /**
     * Adds a new entry of <code>exception_table</code>.
     */
    public void addExceptionHandler(int start, int end,
                                    int handler, int type) {
        tryblocks.add(start, end, handler, type);
    }

    /**
     * Returns the length of bytecode sequence
     * that have been added so far.
     */
    public int currentPc() {
        return getSize();
    }

    /**
     * Reads a signed 8bit value at the offset from the beginning of the
     * bytecode sequence.
     *
     * @throws ArrayIndexOutOfBoundsException   if offset is invalid.
     */
    public int read(int offset) {
        return super.read(offset);
    }

    /**
     * Reads a signed 16bit value at the offset from the beginning of the
     * bytecode sequence.
     */
    public int read16bit(int offset) {
        int v1 = read(offset);
        int v2 = read(offset + 1);
        return (v1 << 8) + (v2 & 0xff);
    }

    /**
     * Reads a signed 32bit value at the offset from the beginning of the
     * bytecode sequence.
     */
    public int read32bit(int offset) {
        int v1 = read16bit(offset);
        int v2 = read16bit(offset + 2);
        return (v1 << 16) + (v2 & 0xffff);
    }

    /**
     * Writes an 8bit value at the offset from the beginning of the
     * bytecode sequence.
     *
     * @throws ArrayIndexOutOfBoundsException   if offset is invalid.
     */
    public void write(int offset, int value) {
        super.write(offset, value);
    }

    /**
     * Writes an 16bit value at the offset from the beginning of the
     * bytecode sequence.
     */
    public void write16bit(int offset, int value) {
        write(offset, value >> 8);
        write(offset + 1, value);
    }

    /**
     * Writes an 32bit value at the offset from the beginning of the
     * bytecode sequence.
     */
    public void write32bit(int offset, int value) {
        write16bit(offset, value >> 16);
        write16bit(offset + 2, value);
    }

    /**
     * Appends an 8bit value to the end of the bytecode sequence.
     */
    public void add(int code) {
        super.add(code);
    }

    /**
     * Appends a 32bit value to the end of the bytecode sequence.
     */
    public void add32bit(int value) {
        add(value >> 24, value >> 16, value >> 8, value);
    }

    /**
     * Appends the length-byte gap to the end of the bytecode sequence.
     *
     * @param length    the gap length in byte.
     */
    public void addGap(int length) {
        super.addGap(length);
    }

    /**
     * Appends an 8bit opcode to the end of the bytecode sequence.
     * The current stack depth is updated.
     * <code>max_stack</code> is updated if the current stack depth
     * is the deepest so far.
     *
     * <p>Note: some instructions such as INVOKEVIRTUAL does not
     * update the current stack depth since the increment depends
     * on the method signature.
     * <code>growStack()</code> must be explicitly called.
     */
    public void addOpcode(int code) {
        add(code);
        growStack(STACK_GROW[code]);
    }

    /**
     * Increases the current stack depth.
     * It also updates <code>max_stack</code> if the current stack depth
     * is the deepest so far.
     *
     * @param diff      the number added to the current stack depth.
     */
    public void growStack(int diff) {
        setStackDepth(stackDepth + diff);
    }

    /**
     * Returns the current stack depth.
     */
    public int getStackDepth() { return stackDepth; }

    /**
     * Sets the current stack depth.
     * It also updates <code>max_stack</code> if the current stack depth
     * is the deepest so far.
     *
     * @param depth     new value.
     */
    public void setStackDepth(int depth) {
        stackDepth = depth;
        if (stackDepth > maxStack)
            maxStack = stackDepth;
    }

    /**
     * Appends a 16bit value to the end of the bytecode sequence.
     * It never changes the current stack depth.
     */
    public void addIndex(int index) {
        add(index >> 8, index);
    }

    /**
     * Appends ALOAD or (WIDE) ALOAD_&lt;n&gt;
     *
     * @param n         an index into the local variable array.
     */
    public void addAload(int n) {
        if (n < 4)
            addOpcode(42 + n);          // aload_<n>
        else if (n < 0x100) {
            addOpcode(ALOAD);           // aload
            add(n);
        }
        else {
            addOpcode(WIDE);
            addOpcode(ALOAD);
            addIndex(n);
        }
    }

    /**
     * Appends ASTORE or (WIDE) ASTORE_&lt;n&gt;
     *
     * @param n         an index into the local variable array.
     */
    public void addAstore(int n) {
        if (n < 4)
            addOpcode(75 + n);  // astore_<n>
        else if (n < 0x100) {
            addOpcode(ASTORE);          // astore
            add(n);
        }
        else {
            addOpcode(WIDE);
            addOpcode(ASTORE);
            addIndex(n);
        }
    }

    /**
     * Appends ICONST or ICONST_&lt;n&gt;
     *
     * @param n         the pushed integer constant.
     */
    public void addIconst(int n) {
        if (n < 6 && -2 < n)
            addOpcode(3 + n);           // iconst_<i>   -1..5
        else if (n <= 127 && -128 <= n) {
            addOpcode(16);              // bipush
            add(n);
        }
        else if (n <= 32767 && -32768 <= n) {
            addOpcode(17);              // sipush
            add(n >> 8);
            add(n);
        }
        else
            addLdc(constPool.addIntegerInfo(n));
    }

    /**
     * Appends an instruction for pushing zero or null on the stack.
     * If the type is void, this method does not append any instruction.
     *
     * @param type      the type of the zero value (or null).
     */
    public void addConstZero(CtClass type) {
        if (type.isPrimitive()) {
            if (type == CtClass.longType)
                addOpcode(LCONST_0);
            else if (type == CtClass.floatType)
                addOpcode(FCONST_0);
            else if (type == CtClass.doubleType)
                addOpcode(DCONST_0);
            else if (type == CtClass.voidType)
                throw new RuntimeException("void type?");
            else
                addOpcode(ICONST_0);
        }
        else
            addOpcode(ACONST_NULL);
    }

    /**
     * Appends ILOAD or (WIDE) ILOAD_&lt;n&gt;
     *
     * @param n         an index into the local variable array.
     */
    public void addIload(int n) {
        if (n < 4)
            addOpcode(26 + n);          // iload_<n>
        else if (n < 0x100) {
            addOpcode(ILOAD);           // iload
            add(n);
        }
        else {
            addOpcode(WIDE);
            addOpcode(ILOAD);
            addIndex(n);
        }
    }

    /**
     * Appends ISTORE or (WIDE) ISTORE_&lt;n&gt;
     *
     * @param n         an index into the local variable array.
     */
    public void addIstore(int n) {
        if (n < 4)
            addOpcode(59 + n);          // istore_<n>
        else if (n < 0x100) {
            addOpcode(ISTORE);          // istore
            add(n);
        }
        else {
            addOpcode(WIDE);
            addOpcode(ISTORE);
            addIndex(n);
        }
    }

    /**
     * Appends LCONST or LCONST_&lt;n&gt;
     *
     * @param n         the pushed long integer constant.
     */
    public void addLconst(long n) {
        if (n == 0 || n == 1)
            addOpcode(9 + (int)n);              // lconst_<n>
        else
            addLdc2w(n);
    }

    /**
     * Appends LLOAD or (WIDE) LLOAD_&lt;n&gt;
     *
     * @param n         an index into the local variable array.
     */
    public void addLload(int n) {
        if (n < 4)
            addOpcode(30 + n);          // lload_<n>
        else if (n < 0x100) {
            addOpcode(LLOAD);           // lload
            add(n);
        }
        else {
            addOpcode(WIDE);
            addOpcode(LLOAD);
            addIndex(n);
        }
    }

    /**
     * Appends LSTORE or LSTORE_&lt;n&gt;
     *
     * @param n         an index into the local variable array.
     */
    public void addLstore(int n) {
        if (n < 4)
            addOpcode(63 + n);          // lstore_<n>
        else if (n < 0x100) {
            addOpcode(LSTORE);          // lstore
            add(n);
        }
        else {
            addOpcode(WIDE);
            addOpcode(LSTORE);
            addIndex(n);
        }
    }
   
    /**
     * Appends DCONST or DCONST_&lt;n&gt;
     *
     * @param d         the pushed double constant.
     */
    public void addDconst(double d) {
        if (d == 0.0 || d == 1.0)
            addOpcode(14 + (int)d);             // dconst_<n>
        else
            addLdc2w(d);
    }

    /**
     * Appends DLOAD or (WIDE) DLOAD_&lt;n&gt;
     *
     * @param n         an index into the local variable array.
     */
    public void addDload(int n) {
        if (n < 4)
            addOpcode(38 + n);          // dload_<n>
        else if (n < 0x100) {
            addOpcode(DLOAD);           // dload
            add(n);
        }
        else {
            addOpcode(WIDE);
            addOpcode(DLOAD);
            addIndex(n);
        }
    }

    /**
     * Appends DSTORE or (WIDE) DSTORE_&lt;n&gt;
     *
     * @param n         an index into the local variable array.
     */
    public void addDstore(int n) {
        if (n < 4)
            addOpcode(71 + n);          // dstore_<n>
        else if (n < 0x100) {
            addOpcode(DSTORE);          // dstore
            add(n);
        }
        else {
            addOpcode(WIDE);
            addOpcode(DSTORE);
            addIndex(n);
        }
    }

    /**
     * Appends FCONST or FCONST_&lt;n&gt;
     *
     * @param f         the pushed float constant.
     */
    public void addFconst(float f) {
        if (f == 0.0f || f == 1.0f || f == 2.0f)
            addOpcode(11 + (int)f);             // fconst_<n>
        else
            addLdc(constPool.addFloatInfo(f));
    }

    /**
     * Appends FLOAD or (WIDE) FLOAD_&lt;n&gt;
     *
     * @param n         an index into the local variable array.
     */
    public void addFload(int n) {
        if (n < 4)
            addOpcode(34 + n);          // fload_<n>
        else if (n < 0x100) {
            addOpcode(FLOAD);           // fload
            add(n);
        }
        else {
            addOpcode(WIDE);
            addOpcode(FLOAD);
            addIndex(n);
        }
    }

    /**
     * Appends FSTORE or FSTORE_&lt;n&gt;
     *
     * @param n         an index into the local variable array.
     */
    public void addFstore(int n) {
        if (n < 4)
            addOpcode(67 + n);          // fstore_<n>
        else if (n < 0x100) {
            addOpcode(FSTORE);          // fstore
            add(n);
        }
        else {
            addOpcode(WIDE);
            addOpcode(FSTORE);
            addIndex(n);
        }
    }

    /**
     * Appends an instruction for loading a value from the
     * local variable at the index <code>n</code>.
     *
     * @param n         the index.
     * @param type      the type of the loaded value.
     * @return          the size of the value (1 or 2 word).
     */
    public int addLoad(int n, CtClass type) {
        if (type.isPrimitive()) {
            if (type == CtClass.booleanType || type == CtClass.charType
                || type == CtClass.byteType || type == CtClass.shortType
                || type == CtClass.intType)
                addIload(n);
            else if (type == CtClass.longType) {
                addLload(n);
                return 2;
            }
            else if(type == CtClass.floatType)
                addFload(n);
            else if(type == CtClass.doubleType) {
                addDload(n);
                return 2;
            }
            else
                throw new RuntimeException("void type?");
        }
        else
            addAload(n);

        return 1;
    }

    /**
     * Appends an instruction for storing a value into the
     * local variable at the index <code>n</code>.
     *
     * @param n         the index.
     * @param type      the type of the stored value.
     * @return          2 if the type is long or double.  Otherwise 1.
     */
    public int addStore(int n, CtClass type) {
        if (type.isPrimitive()) {
            if (type == CtClass.booleanType || type == CtClass.charType
                || type == CtClass.byteType || type == CtClass.shortType
                || type == CtClass.intType)
                addIstore(n);
            else if (type == CtClass.longType) {
                addLstore(n);
                return 2;
            }
            else if (type == CtClass.floatType)
                addFstore(n);
            else if (type == CtClass.doubleType) {
                addDstore(n);
                return 2;
            }
            else
                throw new RuntimeException("void type?");
        }
        else
            addAstore(n);

        return 1;
    }

    /**
     * Appends instructions for loading all the parameters onto the
     * operand stack.
     *
     * @param offset	the index of the first parameter.  It is 0
     *			if the method is static.  Otherwise, it is 1.
     */
    public int addLoadParameters(CtClass[] params, int offset) {
        int stacksize = 0;
        if (params != null) {
            int n = params.length;
            for (int i = 0; i < n; ++i)
                stacksize += addLoad(stacksize + offset, params[i]);
        }

        return stacksize;
    }

    /**
     * Appends CHECKCAST.
     *
     * @param c         the type.
     */
    public void addCheckcast(CtClass c) {
        addOpcode(CHECKCAST);
        addIndex(constPool.addClassInfo(c));
    }

    /**
     * Appends CHECKCAST.
     *
     * @param classname         a fully-qualified class name.
     */
    public void addCheckcast(String classname) {
        addOpcode(CHECKCAST);
        addIndex(constPool.addClassInfo(classname));
    }

    /**
     * Appends INSTANCEOF.
     *
     * @param classname         the class name.
     */
    public void addInstanceof(String classname) {
        addOpcode(INSTANCEOF);
        addIndex(constPool.addClassInfo(classname));
    }

    /**
     * Appends GETFIELD.
     *
     * @param c         the class.
     * @param name      the field name.
     * @param type      the descriptor of the field type.
     *
     * @see Descriptor#of(CtClass)
     */
    public void addGetfield(CtClass c, String name, String type) {
        add(GETFIELD);
        int ci = constPool.addClassInfo(c);
        addIndex(constPool.addFieldrefInfo(ci, name, type));
        growStack(Descriptor.dataSize(type) - 1);
    }

    /**
     * Appends GETFIELD.
     *
     * @param c         the fully-qualified class name.
     * @param name      the field name.
     * @param type      the descriptor of the field type.
     *
     * @see Descriptor#of(CtClass)
     */
    public void addGetfield(String c, String name, String type) {
        add(GETFIELD);
        int ci = constPool.addClassInfo(c);
        addIndex(constPool.addFieldrefInfo(ci, name, type));
        growStack(Descriptor.dataSize(type) - 1);
    }

    /**
     * Appends GETSTATIC.
     *
     * @param c         the class
     * @param name      the field name
     * @param type      the descriptor of the field type.
     *
     * @see Descriptor#of(CtClass)
     */
    public void addGetstatic(CtClass c, String name, String type) {
        add(GETSTATIC);
        int ci = constPool.addClassInfo(c);
        addIndex(constPool.addFieldrefInfo(ci, name, type));
        growStack(Descriptor.dataSize(type));
    }

    /**
     * Appends GETSTATIC.
     *
     * @param c         the fully-qualified class name
     * @param name      the field name
     * @param type      the descriptor of the field type.
     *
     * @see Descriptor#of(CtClass)
     */
    public void addGetstatic(String c, String name, String type) {
        add(GETSTATIC);
        int ci = constPool.addClassInfo(c);
        addIndex(constPool.addFieldrefInfo(ci, name, type));
        growStack(Descriptor.dataSize(type));
    }

    /**
     * Appends INVOKESPECIAL.
     *
     * @param clazz     the target class.
     * @param name      the method name.
     * @param returnType        the return type.
     * @param paramTypes        the parameter types.
     */
    public void addInvokespecial(CtClass clazz, String name,
                                 CtClass returnType, CtClass[] paramTypes) {
        String desc = Descriptor.ofMethod(returnType, paramTypes);
        addInvokespecial(clazz, name, desc);
    }

    /**
     * Appends INVOKESPECIAL.
     *
     * @param clazz     the target class.
     * @param name      the method name
     * @param desc      the descriptor of the method signature.
     *
     * @see Descriptor#ofMethod(CtClass,CtClass[])
     * @see Descriptor#ofConstructor(CtClass[])
     */
    public void addInvokespecial(CtClass clazz, String name, String desc) {
        addInvokespecial(constPool.addClassInfo(clazz), name, desc);
    }

    /**
     * Appends INVOKESPECIAL.
     *
     * @param clazz     the fully-qualified class name.
     * @param name      the method name
     * @param desc      the descriptor of the method signature.
     *
     * @see Descriptor#ofMethod(CtClass,CtClass[])
     * @see Descriptor#ofConstructor(CtClass[])
     */
    public void addInvokespecial(String clazz, String name, String desc) {
        addInvokespecial(constPool.addClassInfo(clazz), name, desc);
    }

    /**
     * Appends INVOKESPECIAL.
     *
     * @param clazz     the index of <code>CONSTANT_Class_info</code>
     *                  structure.
     * @param name      the method name
     * @param desc      the descriptor of the method signature.
     *
     * @see Descriptor#ofMethod(CtClass,CtClass[])
     * @see Descriptor#ofConstructor(CtClass[])
     */
    public void addInvokespecial(int clazz, String name, String desc) {
        add(INVOKESPECIAL);
        addIndex(constPool.addMethodrefInfo(clazz, name, desc));
        growStack(Descriptor.dataSize(desc) - 1);
    }

    /**
     * Appends INVOKESTATIC.
     *
     * @param clazz     the target class.
     * @param name      the method name
     * @param returnType        the return type.
     * @param paramTypes        the parameter types.
     */
    public void addInvokestatic(CtClass clazz, String name,
                                CtClass returnType, CtClass[] paramTypes) {
        String desc = Descriptor.ofMethod(returnType, paramTypes);
        addInvokestatic(clazz, name, desc);
    }

    /**
     * Appends INVOKESTATIC.
     *
     * @param clazz     the target class.
     * @param name      the method name
     * @param desc      the descriptor of the method signature.
     *
     * @see Descriptor#ofMethod(CtClass,CtClass[])
     */
    public void addInvokestatic(CtClass clazz, String name, String desc) {
        addInvokestatic(constPool.addClassInfo(clazz), name, desc);
    }

    /**
     * Appends INVOKESTATIC.
     *
     * @param classname the fully-qualified class name.
     * @param name      the method name
     * @param desc      the descriptor of the method signature.
     *
     * @see Descriptor#ofMethod(CtClass,CtClass[])
     */
    public void addInvokestatic(String classname, String name, String desc) {
        addInvokestatic(constPool.addClassInfo(classname), name, desc);
    }

    /**
     * Appends INVOKESTATIC.
     *
     * @param clazz     the index of <code>CONSTANT_Class_info</code>
     *                  structure.
     * @param name      the method name
     * @param desc      the descriptor of the method signature.
     *
     * @see Descriptor#ofMethod(CtClass,CtClass[])
     */
    public void addInvokestatic(int clazz, String name, String desc) {
        add(INVOKESTATIC);
        addIndex(constPool.addMethodrefInfo(clazz, name, desc));
        growStack(Descriptor.dataSize(desc));
    }

    /**
     * Appends INVOKEVIRTUAL.
     *
     * <p>The specified method must not be an inherited method.
     * It must be directly declared in the class specified
     * in <code>clazz</code>.
     *
     * @param clazz     the target class.
     * @param name      the method name
     * @param returnType        the return type.
     * @param paramTypes        the parameter types.
     */
    public void addInvokevirtual(CtClass clazz, String name,
                                 CtClass returnType, CtClass[] paramTypes) {
        String desc = Descriptor.ofMethod(returnType, paramTypes);
        addInvokevirtual(clazz, name, desc);
    }

    /**
     * Appends INVOKEVIRTUAL.
     *
     * <p>The specified method must not be an inherited method.
     * It must be directly declared in the class specified
     * in <code>clazz</code>.
     *
     * @param clazz     the target class.
     * @param name      the method name
     * @param desc      the descriptor of the method signature.
     *
     * @see Descriptor#ofMethod(CtClass,CtClass[])
     */
    public void addInvokevirtual(CtClass clazz, String name, String desc) {
        addInvokevirtual(constPool.addClassInfo(clazz), name, desc);
    }

    /**
     * Appends INVOKEVIRTUAL.
     *
     * <p>The specified method must not be an inherited method.
     * It must be directly declared in the class specified
     * in <code>classname</code>.
     *
     * @param classname the fully-qualified class name.
     * @param name      the method name
     * @param desc      the descriptor of the method signature.
     *
     * @see Descriptor#ofMethod(CtClass,CtClass[])
     */
    public void addInvokevirtual(String classname, String name, String desc) {
        addInvokevirtual(constPool.addClassInfo(classname), name, desc);
    }

    /**
     * Appends INVOKEVIRTUAL.
     *
     * <p>The specified method must not be an inherited method.
     * It must be directly declared in the class specified
     * by <code>clazz</code>.
     *
     * @param clazz     the index of <code>CONSTANT_Class_info</code>
     *                  structure.
     * @param name      the method name
     * @param desc      the descriptor of the method signature.
     *
     * @see Descriptor#ofMethod(CtClass,CtClass[])
     */
    public void addInvokevirtual(int clazz, String name, String desc) {
        add(INVOKEVIRTUAL);
        addIndex(constPool.addMethodrefInfo(clazz, name, desc));
        growStack(Descriptor.dataSize(desc) - 1);
    }

    /**
     * Appends INVOKEINTERFACE.
     *
     * @param clazz     the target class.
     * @param name      the method name
     * @param returnType        the return type.
     * @param paramTypes        the parameter types.
     * @param count     the count operand of the instruction.
     */
    public void addInvokeinterface(CtClass clazz, String name,
                                   CtClass returnType, CtClass[] paramTypes,
                                   int count) {
        String desc = Descriptor.ofMethod(returnType, paramTypes);
        addInvokeinterface(clazz, name, desc, count);
    }

    /**
     * Appends INVOKEINTERFACE.
     *
     * @param clazz     the target class.
     * @param name      the method name
     * @param desc      the descriptor of the method signature.
     * @param count     the count operand of the instruction.
     *
     * @see Descriptor#ofMethod(CtClass,CtClass[])
     */
    public void addInvokeinterface(CtClass clazz, String name,
                                   String desc, int count) {
        addInvokeinterface(constPool.addClassInfo(clazz), name, desc,
                           count);
    }

    /**
     * Appends INVOKEINTERFACE.
     *
     * @param classname the fully-qualified class name.
     * @param name      the method name
     * @param desc      the descriptor of the method signature.
     * @param count     the count operand of the instruction.
     *
     * @see Descriptor#ofMethod(CtClass,CtClass[])
     */
    public void addInvokeinterface(String classname, String name,
                                   String desc, int count) {
        addInvokeinterface(constPool.addClassInfo(classname), name, desc,
                           count);
    }

    /**
     * Appends INVOKEINTERFACE.
     *
     * @param clazz     the index of <code>CONSTANT_Class_info</code>
     *                  structure.
     * @param name      the method name
     * @param desc      the descriptor of the method signature.
     * @param count     the count operand of the instruction.
     *
     * @see Descriptor#ofMethod(CtClass,CtClass[])
     */
    public void addInvokeinterface(int clazz, String name,
                                   String desc, int count) {
        add(INVOKEINTERFACE);
        addIndex(constPool.addInterfaceMethodrefInfo(clazz, name, desc));
        add(count);
        add(0);
        growStack(Descriptor.dataSize(desc) - 1);
    }

    /**
     * Appends LDC or LDC_W.  The pushed item is a <code>String</code>
     * object.
     *
     * @param s         the character string pushed by LDC or LDC_W.
     */
    public void addLdc(String s) {
        addLdc(constPool.addStringInfo(s));
    }

    /**
     * Appends LDC or LDC_W.
     *
     * @param i         index into the constant pool.
     */
    public void addLdc(int i) {
        if (i > 0xFF) {
            addOpcode(LDC_W);
            addIndex(i);
        }
        else {
            addOpcode(LDC);
            add(i);
        }
    }

    /**
     * Appends LDC2_W.  The pushed item is a long value.
     */
    public void addLdc2w(long l) {
        addOpcode(LDC2_W);
        addIndex(constPool.addLongInfo(l));
    }

    /**
     * Appends LDC2_W.  The pushed item is a double value.
     */
    public void addLdc2w(double d) {
        addOpcode(LDC2_W);
        addIndex(constPool.addDoubleInfo(d));
    }

    /**
     * Appends NEW.
     *
     * @param clazz     the class of the created instance.
     */
    public void addNew(CtClass clazz) {
        addOpcode(NEW);
        addIndex(constPool.addClassInfo(clazz));
    }

    /**
     * Appends NEW.
     *
     * @param classname         the fully-qualified class name.
     */
    public void addNew(String classname) {
        addOpcode(NEW);
        addIndex(constPool.addClassInfo(classname));
    }

    /**
     * Appends ANEWARRAY.
     *
     * @param classname         the qualified class name of the element type.
     */
    public void addAnewarray(String classname) {
        addOpcode(ANEWARRAY);
        addIndex(constPool.addClassInfo(classname));
    }

    /**
     * Appends ICONST and ANEWARRAY.
     *
     * @param clazz     the elememnt type.
     * @param length    the array length.
     */
    public void addAnewarray(CtClass clazz, int length) {
        addIconst(length);
        addOpcode(ANEWARRAY);
        addIndex(constPool.addClassInfo(clazz));
    }

    /**
     * Appends NEWARRAY for primitive types.
     *
     * @param atype     <code>T_BOOLEAN</code>, <code>T_CHAR</code>, ...
     * @see Opcode
     */
    public void addNewarray(int atype, int length) {
        addIconst(length);
        addOpcode(NEWARRAY);
        add(atype);
    }

    /**
     * Appends MULTINEWARRAY.
     *
     * @param clazz             the array type.
     * @param dimensions        the sizes of all dimensions.
     * @return          the length of <code>dimensions</code>.
     */
    public int addMultiNewarray(CtClass clazz, int[] dimensions) {
        int len = dimensions.length;
        for (int i = 0; i < len; ++i)
            addIconst(dimensions[i]);

        growStack(len);
        return addMultiNewarray(clazz, len);
    }

    /**
     * Appends MULTINEWARRAY.  The size of every dimension must have been
     * already pushed on the stack.
     *
     * @param clazz             the array type.
     * @param dim               the number of the dimensions.
     * @return                  the value of <code>dim</code>.
     */
    public int addMultiNewarray(CtClass clazz, int dim) {
        add(MULTIANEWARRAY);
        addIndex(constPool.addClassInfo(clazz));
        add(dim);
        growStack(1 - dim);
        return dim;
    }

    /**
     * Appends MULTINEWARRAY.
     *
     * @param desc      the type descriptor of the created array.
     * @param dim       dimensions.
     * @return          the value of <code>dim</code>.
     */
    public int addMultiNewarray(String desc, int dim) {
        add(MULTIANEWARRAY);
        addIndex(constPool.addClassInfo(desc));
        add(dim);
        growStack(1 - dim);
        return dim;
    }

    /**
     * Appends PUTFIELD.
     *
     * @param c         the target class.
     * @param name      the field name.
     * @param desc      the descriptor of the field type.
     */
    public void addPutfield(CtClass c, String name, String desc) {
        addPutfield0(c, null, name, desc);
    }

    /**
     * Appends PUTFIELD.
     *
     * @param classname         the fully-qualified name of the target class.
     * @param name      the field name.
     * @param desc      the descriptor of the field type.
     */
    public void addPutfield(String classname, String name, String desc) {
        // if classnaem is null, the target class is THIS.
        addPutfield0(null, classname, name, desc);
    }

    private void addPutfield0(CtClass target, String classname,
                              String name, String desc) {
        add(PUTFIELD);
        // target is null if it represents THIS.
        int ci = classname == null ? constPool.addClassInfo(target)
                                   : constPool.addClassInfo(classname);
        addIndex(constPool.addFieldrefInfo(ci, name, desc));
        growStack(-1 - Descriptor.dataSize(desc));
    }

    /**
     * Appends PUTSTATIC.
     *
     * @param c         the target class.
     * @param name      the field name.
     * @param desc      the descriptor of the field type.
     */
    public void addPutstatic(CtClass c, String name, String desc) {
        addPutstatic0(c, null, name, desc);
    }

    /**
     * Appends PUTSTATIC.
     *
     * @param classname         the fully-qualified name of the target class.
     * @param fieldName         the field name.
     * @param desc              the descriptor of the field type.
     */
    public void addPutstatic(String classname, String fieldName, String desc) {
        // if classname is null, the target class is THIS.
        addPutstatic0(null, classname, fieldName, desc);
    }

    private void addPutstatic0(CtClass target, String classname,
                               String fieldName, String desc) {
        add(PUTSTATIC);
        // target is null if it represents THIS.
        int ci = classname == null ? constPool.addClassInfo(target)
                                : constPool.addClassInfo(classname);
        addIndex(constPool.addFieldrefInfo(ci, fieldName, desc));
        growStack(-Descriptor.dataSize(desc));
    }

    /**
     * Appends ARETURN, IRETURN, .., or RETURN.
     *
     * @param type      the return type.
     */
    public void addReturn(CtClass type) {
        if (type == null)
            addOpcode(RETURN);
        else if (type.isPrimitive()) {
            CtPrimitiveType ptype = (CtPrimitiveType)type;
            addOpcode(ptype.getReturnOp());
        }
        else
            addOpcode(ARETURN);
    }

    /**
     * Appends RET.
     *
     * @param var       local variable
     */
    public void addRet(int var) {
        if (var < 0x100) {
            addOpcode(RET);
            add(var);
        }
        else {
            addOpcode(WIDE);
            addOpcode(RET);
            addIndex(var);
        }
    }

    /**
     * Appends instructions for executing
     * <code>java.lang.System.println(<i>message</i>)</code>.
     *
     * @param message           printed message.
     */
    public void addPrintln(String message) {
        addGetstatic("java.lang.System", "err", "Ljava/io/PrintStream;");
        addLdc(message);
        addInvokevirtual("java.io.PrintStream",
                         "println", "(Ljava/lang/String;)V");
    }
}
