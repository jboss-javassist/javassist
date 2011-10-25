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

import java.io.PrintStream;

import javassist.CtMethod;

/**
 * Simple utility class for printing the bytecode instructions of a method.
 *
 * @author Jason T. Greene
 */
public class InstructionPrinter implements Opcode {

    private final static String opcodes[] = Mnemonic.OPCODE;
    private final PrintStream stream;

    /**
     * Constructs a <code>InstructionPrinter</code> object.
     */
    public InstructionPrinter(PrintStream stream) {
        this.stream = stream;
    }

    /**
     * Prints the bytecode instructions of a given method.
     */
    public static void print(CtMethod method, PrintStream stream) {
        (new InstructionPrinter(stream)).print(method);
    }

    /**
     * Prints the bytecode instructions of a given method.
     */
    public void print(CtMethod method) {
        MethodInfo info = method.getMethodInfo2();
        ConstPool pool = info.getConstPool();
        CodeAttribute code = info.getCodeAttribute();
        if (code == null)
            return;

        CodeIterator iterator = code.iterator();
        while (iterator.hasNext()) {
            int pos;
            try {
                pos = iterator.next();
            } catch (BadBytecode e) {
                throw new RuntimeException(e);
            }

            stream.println(pos + ": " + instructionString(iterator, pos, pool));
        }
    }

    /**
     * Gets a string representation of the bytecode instruction at the specified
     * position. 
     */
    public static String instructionString(CodeIterator iter, int pos, ConstPool pool) {
        int opcode = iter.byteAt(pos);

        if (opcode > opcodes.length || opcode < 0)
            throw new IllegalArgumentException("Invalid opcode, opcode: " + opcode + " pos: "+ pos);

        String opstring = opcodes[opcode];
        switch (opcode) {
            case BIPUSH:
                return opstring + " " + iter.byteAt(pos + 1);
            case SIPUSH:
                return opstring + " " + iter.s16bitAt(pos + 1);
            case LDC:
                return opstring + " " + ldc(pool, iter.byteAt(pos + 1));
            case LDC_W :
            case LDC2_W :
                return opstring + " " + ldc(pool, iter.u16bitAt(pos + 1));
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                return opstring + " " + iter.byteAt(pos + 1);
            case IFEQ:
            case IFGE:
            case IFGT:
            case IFLE:
            case IFLT:
            case IFNE:
            case IFNONNULL:
            case IFNULL:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case IF_ICMPEQ:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ICMPLT:
            case IF_ICMPNE:
                return opstring + " " + (iter.s16bitAt(pos + 1) + pos);
            case IINC:
                return opstring + " " + iter.byteAt(pos + 1);
            case GOTO:
            case JSR:
                return opstring + " " + (iter.s16bitAt(pos + 1) + pos);
            case RET:
                return opstring + " " + iter.byteAt(pos + 1);
            case TABLESWITCH:
                return tableSwitch(iter, pos);
            case LOOKUPSWITCH:
                return lookupSwitch(iter, pos);
            case GETSTATIC:
            case PUTSTATIC:
            case GETFIELD:
            case PUTFIELD:
                return opstring + " " + fieldInfo(pool, iter.u16bitAt(pos + 1));
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
            case INVOKESTATIC:
                return opstring + " " + methodInfo(pool, iter.u16bitAt(pos + 1));
            case INVOKEINTERFACE:
                return opstring + " " + interfaceMethodInfo(pool, iter.u16bitAt(pos + 1));
            case 186:
                throw new RuntimeException("Bad opcode 186");
            case NEW:
                return opstring + " " + classInfo(pool, iter.u16bitAt(pos + 1));
            case NEWARRAY:
                return opstring + " " + arrayInfo(iter.byteAt(pos + 1));
            case ANEWARRAY:
            case CHECKCAST:
                return opstring + " " + classInfo(pool, iter.u16bitAt(pos + 1));
            case WIDE:
                return wide(iter, pos);
            case MULTIANEWARRAY:
                return opstring + " " + classInfo(pool, iter.u16bitAt(pos + 1));
            case GOTO_W:
            case JSR_W:
                return opstring + " " + (iter.s32bitAt(pos + 1)+ pos);
            default:
                return opstring;
        }
    }


    private static String wide(CodeIterator iter, int pos) {
        int opcode = iter.byteAt(pos + 1);
        int index = iter.u16bitAt(pos + 2);
        switch (opcode) {
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
            case IINC:
            case RET:
                return opcodes[opcode] + " " + index;
            default:
                throw new RuntimeException("Invalid WIDE operand");
        }
    }


    private static String arrayInfo(int type) {
        switch (type) {
            case T_BOOLEAN:
                return "boolean";
            case T_CHAR:
                return "char";
            case T_BYTE:
                return "byte";
            case T_SHORT:
                return "short";
            case T_INT:
                return "int";
            case T_LONG:
                return "long";
            case T_FLOAT:
                return "float";
            case T_DOUBLE:
                return "double";
            default:
                throw new RuntimeException("Invalid array type");
        }
    }


    private static String classInfo(ConstPool pool, int index) {
        return "#" + index + " = Class " + pool.getClassInfo(index);
    }


    private static String interfaceMethodInfo(ConstPool pool, int index) {
        return "#" + index + " = Method "
                + pool.getInterfaceMethodrefClassName(index) + "."
                + pool.getInterfaceMethodrefName(index) + "("
                + pool.getInterfaceMethodrefType(index) + ")";
    }

    private static String methodInfo(ConstPool pool, int index) {
        return "#" + index + " = Method "
                + pool.getMethodrefClassName(index) + "."
                + pool.getMethodrefName(index) + "("
                + pool.getMethodrefType(index) + ")";
    }


    private static String fieldInfo(ConstPool pool, int index) {
        return "#" + index + " = Field "
            + pool.getFieldrefClassName(index) + "."
            + pool.getFieldrefName(index) + "("
            + pool.getFieldrefType(index) + ")";
    }


    private static String lookupSwitch(CodeIterator iter, int pos) {
        StringBuffer buffer = new StringBuffer("lookupswitch {\n");
        int index = (pos & ~3) + 4;
        // default
        buffer.append("\t\tdefault: ").append(pos + iter.s32bitAt(index)).append("\n");
        int npairs = iter.s32bitAt(index += 4);
        int end = npairs * 8 + (index += 4);

        for (; index < end; index += 8) {
            int match = iter.s32bitAt(index);
            int target = iter.s32bitAt(index + 4) + pos;
            buffer.append("\t\t").append(match).append(": ").append(target).append("\n");
        }

        buffer.setCharAt(buffer.length() - 1, '}');
        return buffer.toString();
    }


    private static String tableSwitch(CodeIterator iter, int pos) {
        StringBuffer buffer = new StringBuffer("tableswitch {\n");
        int index = (pos & ~3) + 4;
        // default
        buffer.append("\t\tdefault: ").append(pos + iter.s32bitAt(index)).append("\n");
        int low = iter.s32bitAt(index += 4);
        int high = iter.s32bitAt(index += 4);
        int end = (high - low + 1) * 4 + (index += 4);

        // Offset table
        for (int key = low; index < end; index += 4, key++) {
            int target = iter.s32bitAt(index) + pos;
            buffer.append("\t\t").append(key).append(": ").append(target).append("\n");
        }

        buffer.setCharAt(buffer.length() - 1, '}');
        return buffer.toString();
    }


    private static String ldc(ConstPool pool, int index) {
        int tag = pool.getTag(index);
        switch (tag) {
            case ConstPool.CONST_String:
                return "#" + index + " = \"" + pool.getStringInfo(index) + "\"";
            case ConstPool.CONST_Integer:
                return "#" + index + " = int " + pool.getIntegerInfo(index);
            case ConstPool.CONST_Float:
                return "#" + index + " = float " + pool.getFloatInfo(index);
            case ConstPool.CONST_Long:
                return "#" + index + " = long " + pool.getLongInfo(index);
            case ConstPool.CONST_Double:
                return "#" + index + " = int " + pool.getDoubleInfo(index);
            case ConstPool.CONST_Class:
                return classInfo(pool, index);
            default:
                throw new RuntimeException("bad LDC: " + tag);
        }
    }
}
