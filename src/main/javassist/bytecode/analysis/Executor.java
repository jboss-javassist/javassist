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
package javassist.bytecode.analysis;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

/**
 * Executor is responsible for modeling the effects of a JVM instruction on a frame.
 *
 * @author Jason T. Greene
 */
public class Executor implements Opcode {
    private final ConstPool constPool;
    private final ClassPool classPool;
    private final Type STRING_TYPE;
    private final Type CLASS_TYPE;
    private final Type THROWABLE_TYPE;
    private int lastPos;

    public Executor(ClassPool classPool, ConstPool constPool) {
        this.constPool = constPool;
        this.classPool = classPool;

        try {
            STRING_TYPE = getType("java.lang.String");
            CLASS_TYPE = getType("java.lang.Class");
            THROWABLE_TYPE = getType("java.lang.Throwable");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Execute the instruction, modeling the effects on the specified frame and subroutine.
     * If a subroutine is passed, the access flags will be modified if this instruction accesses
     * the local variable table.
     *
     * @param method the method containing the instruction
     * @param pos the position of the instruction in the method
     * @param iter the code iterator used to find the instruction
     * @param frame the frame to modify to represent the result of the instruction
     * @param subroutine the optional subroutine this instruction belongs to.
     * @throws BadBytecode if the bytecode violates the jvm spec
     */
    public void execute(MethodInfo method, int pos, CodeIterator iter, Frame frame, Subroutine subroutine) throws BadBytecode {
        this.lastPos = pos;
        int opcode = iter.byteAt(pos);


        // Declared opcode in order
        switch (opcode) {
            case NOP:
                break;
            case ACONST_NULL:
                frame.push(Type.UNINIT);
                break;
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
                frame.push(Type.INTEGER);
                break;
            case LCONST_0:
            case LCONST_1:
                frame.push(Type.LONG);
                frame.push(Type.TOP);
                break;
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                frame.push(Type.FLOAT);
                break;
            case DCONST_0:
            case DCONST_1:
                frame.push(Type.DOUBLE);
                frame.push(Type.TOP);
                break;
            case BIPUSH:
            case SIPUSH:
                frame.push(Type.INTEGER);
                break;
            case LDC:
                evalLDC(iter.byteAt(pos + 1),  frame);
                break;
            case LDC_W :
            case LDC2_W :
                evalLDC(iter.u16bitAt(pos + 1), frame);
                break;
            case ILOAD:
                evalLoad(Type.INTEGER, iter.byteAt(pos + 1), frame, subroutine);
                break;
            case LLOAD:
                evalLoad(Type.LONG, iter.byteAt(pos + 1), frame, subroutine);
                break;
            case FLOAD:
                evalLoad(Type.FLOAT, iter.byteAt(pos + 1), frame, subroutine);
                break;
            case DLOAD:
                evalLoad(Type.DOUBLE, iter.byteAt(pos + 1), frame, subroutine);
                break;
            case ALOAD:
                evalLoad(Type.OBJECT, iter.byteAt(pos + 1), frame, subroutine);
                break;
            case ILOAD_0:
            case ILOAD_1:
            case ILOAD_2:
            case ILOAD_3:
                evalLoad(Type.INTEGER, opcode - ILOAD_0, frame, subroutine);
                break;
            case LLOAD_0:
            case LLOAD_1:
            case LLOAD_2:
            case LLOAD_3:
                evalLoad(Type.LONG, opcode - LLOAD_0, frame, subroutine);
                break;
            case FLOAD_0:
            case FLOAD_1:
            case FLOAD_2:
            case FLOAD_3:
                evalLoad(Type.FLOAT, opcode - FLOAD_0, frame, subroutine);
                break;
            case DLOAD_0:
            case DLOAD_1:
            case DLOAD_2:
            case DLOAD_3:
                evalLoad(Type.DOUBLE, opcode - DLOAD_0, frame, subroutine);
                break;
            case ALOAD_0:
            case ALOAD_1:
            case ALOAD_2:
            case ALOAD_3:
                evalLoad(Type.OBJECT, opcode - ALOAD_0, frame, subroutine);
                break;
            case IALOAD:
                evalArrayLoad(Type.INTEGER, frame);
                break;
            case LALOAD:
                evalArrayLoad(Type.LONG, frame);
                break;
            case FALOAD:
                evalArrayLoad(Type.FLOAT, frame);
                break;
            case DALOAD:
                evalArrayLoad(Type.DOUBLE, frame);
                break;
            case AALOAD:
                evalArrayLoad(Type.OBJECT, frame);
                break;
            case BALOAD:
            case CALOAD:
            case SALOAD:
                evalArrayLoad(Type.INTEGER, frame);
                break;
            case ISTORE:
                evalStore(Type.INTEGER, iter.byteAt(pos + 1), frame, subroutine);
                break;
            case LSTORE:
                evalStore(Type.LONG, iter.byteAt(pos + 1), frame, subroutine);
                break;
            case FSTORE:
                evalStore(Type.FLOAT, iter.byteAt(pos + 1), frame, subroutine);
                break;
            case DSTORE:
                evalStore(Type.DOUBLE, iter.byteAt(pos + 1), frame, subroutine);
                break;
            case ASTORE:
                evalStore(Type.OBJECT, iter.byteAt(pos + 1), frame, subroutine);
                break;
            case ISTORE_0:
            case ISTORE_1:
            case ISTORE_2:
            case ISTORE_3:
                evalStore(Type.INTEGER, opcode - ISTORE_0, frame, subroutine);
                break;
            case LSTORE_0:
            case LSTORE_1:
            case LSTORE_2:
            case LSTORE_3:
                evalStore(Type.LONG, opcode - LSTORE_0, frame, subroutine);
                break;
            case FSTORE_0:
            case FSTORE_1:
            case FSTORE_2:
            case FSTORE_3:
                evalStore(Type.FLOAT, opcode - FSTORE_0, frame, subroutine);
                break;
            case DSTORE_0:
            case DSTORE_1:
            case DSTORE_2:
            case DSTORE_3:
                evalStore(Type.DOUBLE, opcode - DSTORE_0, frame, subroutine);
                break;
            case ASTORE_0:
            case ASTORE_1:
            case ASTORE_2:
            case ASTORE_3:
                evalStore(Type.OBJECT, opcode - ASTORE_0, frame, subroutine);
                break;
            case IASTORE:
                evalArrayStore(Type.INTEGER, frame);
                break;
            case LASTORE:
                evalArrayStore(Type.LONG, frame);
                break;
            case FASTORE:
                evalArrayStore(Type.FLOAT, frame);
                break;
            case DASTORE:
                evalArrayStore(Type.DOUBLE, frame);
                break;
            case AASTORE:
                evalArrayStore(Type.OBJECT, frame);
                break;
            case BASTORE:
            case CASTORE:
            case SASTORE:
                evalArrayStore(Type.INTEGER, frame);
                break;
            case POP:
                if (frame.pop() == Type.TOP)
                    throw new BadBytecode("POP can not be used with a category 2 value, pos = " + pos);
                break;
            case POP2:
                frame.pop();
                frame.pop();
                break;
            case DUP: {
                Type type = frame.peek();
                if (type == Type.TOP)
                    throw new BadBytecode("DUP can not be used with a category 2 value, pos = " + pos);

                frame.push(frame.peek());
                break;
            }
            case DUP_X1:
            case DUP_X2: {
                Type type = frame.peek();
                if (type == Type.TOP)
                    throw new BadBytecode("DUP can not be used with a category 2 value, pos = " + pos);
                int end = frame.getTopIndex();
                int insert = end - (opcode - DUP_X1) - 1;
                frame.push(type);

                while (end > insert) {
                    frame.setStack(end, frame.getStack(end - 1));
                    end--;
                }
                frame.setStack(insert, type);
                break;
            }
            case DUP2:
                frame.push(frame.getStack(frame.getTopIndex() - 1));
                frame.push(frame.getStack(frame.getTopIndex() - 1));
                break;
            case DUP2_X1:
            case DUP2_X2: {
                int end = frame.getTopIndex();
                int insert = end - (opcode - DUP2_X1) - 1;
                Type type1 = frame.getStack(frame.getTopIndex() - 1);
                Type type2 = frame.peek();
                frame.push(type1);
                frame.push(type2);
                while (end > insert) {
                    frame.setStack(end, frame.getStack(end - 2));
                    end--;
                }
                frame.setStack(insert, type2);
                frame.setStack(insert - 1, type1);
                break;
            }
            case SWAP: {
                Type type1 = frame.pop();
                Type type2 = frame.pop();
                if (type1.getSize() == 2 || type2.getSize() == 2)
                    throw new BadBytecode("Swap can not be used with category 2 values, pos = " + pos);
                frame.push(type1);
                frame.push(type2);
                break;
            }

            // Math
            case IADD:
                evalBinaryMath(Type.INTEGER, frame);
                break;
            case LADD:
                evalBinaryMath(Type.LONG, frame);
                break;
            case FADD:
                evalBinaryMath(Type.FLOAT, frame);
                break;
            case DADD:
                evalBinaryMath(Type.DOUBLE, frame);
                break;
            case ISUB:
                evalBinaryMath(Type.INTEGER, frame);
                break;
            case LSUB:
                evalBinaryMath(Type.LONG, frame);
                break;
            case FSUB:
                evalBinaryMath(Type.FLOAT, frame);
                break;
            case DSUB:
                evalBinaryMath(Type.DOUBLE, frame);
                break;
            case IMUL:
                evalBinaryMath(Type.INTEGER, frame);
                break;
            case LMUL:
                evalBinaryMath(Type.LONG, frame);
                break;
            case FMUL:
                evalBinaryMath(Type.FLOAT, frame);
                break;
            case DMUL:
                evalBinaryMath(Type.DOUBLE, frame);
                break;
            case IDIV:
                evalBinaryMath(Type.INTEGER, frame);
                break;
            case LDIV:
                evalBinaryMath(Type.LONG, frame);
                break;
            case FDIV:
                evalBinaryMath(Type.FLOAT, frame);
                break;
            case DDIV:
                evalBinaryMath(Type.DOUBLE, frame);
                break;
            case IREM:
                evalBinaryMath(Type.INTEGER, frame);
                break;
            case LREM:
                evalBinaryMath(Type.LONG, frame);
                break;
            case FREM:
                evalBinaryMath(Type.FLOAT, frame);
                break;
            case DREM:
                evalBinaryMath(Type.DOUBLE, frame);
                break;

            // Unary
            case INEG:
                verifyAssignable(Type.INTEGER, simplePeek(frame));
                break;
            case LNEG:
                verifyAssignable(Type.LONG, simplePeek(frame));
                break;
            case FNEG:
                verifyAssignable(Type.FLOAT, simplePeek(frame));
                break;
            case DNEG:
                verifyAssignable(Type.DOUBLE, simplePeek(frame));
                break;

            // Shifts
            case ISHL:
                evalShift(Type.INTEGER, frame);
                break;
            case LSHL:
                evalShift(Type.LONG, frame);
                break;
            case ISHR:
                evalShift(Type.INTEGER, frame);
                break;
            case LSHR:
                evalShift(Type.LONG, frame);
                break;
            case IUSHR:
                evalShift(Type.INTEGER,frame);
                break;
            case LUSHR:
                evalShift(Type.LONG, frame);
                break;

            // Bitwise Math
            case IAND:
                evalBinaryMath(Type.INTEGER, frame);
                break;
            case LAND:
                evalBinaryMath(Type.LONG, frame);
                break;
            case IOR:
                evalBinaryMath(Type.INTEGER, frame);
                break;
            case LOR:
                evalBinaryMath(Type.LONG, frame);
                break;
            case IXOR:
                evalBinaryMath(Type.INTEGER, frame);
                break;
            case LXOR:
                evalBinaryMath(Type.LONG, frame);
                break;

            case IINC: {
                int index = iter.byteAt(pos + 1);
                verifyAssignable(Type.INTEGER, frame.getLocal(index));
                access(index, Type.INTEGER, subroutine);
                break;
            }

            // Conversion
            case I2L:
                verifyAssignable(Type.INTEGER, simplePop(frame));
                simplePush(Type.LONG, frame);
                break;
            case I2F:
                verifyAssignable(Type.INTEGER, simplePop(frame));
                simplePush(Type.FLOAT, frame);
                break;
            case I2D:
                verifyAssignable(Type.INTEGER, simplePop(frame));
                simplePush(Type.DOUBLE, frame);
                break;
            case L2I:
                verifyAssignable(Type.LONG, simplePop(frame));
                simplePush(Type.INTEGER, frame);
                break;
            case L2F:
                verifyAssignable(Type.LONG, simplePop(frame));
                simplePush(Type.FLOAT, frame);
                break;
            case L2D:
                verifyAssignable(Type.LONG, simplePop(frame));
                simplePush(Type.DOUBLE, frame);
                break;
            case F2I:
                verifyAssignable(Type.FLOAT, simplePop(frame));
                simplePush(Type.INTEGER, frame);
                break;
            case F2L:
                verifyAssignable(Type.FLOAT, simplePop(frame));
                simplePush(Type.LONG, frame);
                break;
            case F2D:
                verifyAssignable(Type.FLOAT, simplePop(frame));
                simplePush(Type.DOUBLE, frame);
                break;
            case D2I:
                verifyAssignable(Type.DOUBLE, simplePop(frame));
                simplePush(Type.INTEGER, frame);
                break;
            case D2L:
                verifyAssignable(Type.DOUBLE, simplePop(frame));
                simplePush(Type.LONG, frame);
                break;
            case D2F:
                verifyAssignable(Type.DOUBLE, simplePop(frame));
                simplePush(Type.FLOAT, frame);
                break;
            case I2B:
            case I2C:
            case I2S:
                verifyAssignable(Type.INTEGER, frame.peek());
                break;
            case LCMP:
                verifyAssignable(Type.LONG, simplePop(frame));
                verifyAssignable(Type.LONG, simplePop(frame));
                frame.push(Type.INTEGER);
                break;
            case FCMPL:
            case FCMPG:
                verifyAssignable(Type.FLOAT, simplePop(frame));
                verifyAssignable(Type.FLOAT, simplePop(frame));
                frame.push(Type.INTEGER);
                break;
            case DCMPL:
            case DCMPG:
                verifyAssignable(Type.DOUBLE, simplePop(frame));
                verifyAssignable(Type.DOUBLE, simplePop(frame));
                frame.push(Type.INTEGER);
                break;

            // Control flow
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
                verifyAssignable(Type.INTEGER, simplePop(frame));
                break;
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
                verifyAssignable(Type.INTEGER, simplePop(frame));
                verifyAssignable(Type.INTEGER, simplePop(frame));
                break;
            case IF_ACMPEQ:
            case IF_ACMPNE:
                verifyAssignable(Type.OBJECT, simplePop(frame));
                verifyAssignable(Type.OBJECT, simplePop(frame));
                break;
            case GOTO:
                break;
            case JSR:
                frame.push(Type.RETURN_ADDRESS);
                break;
            case RET:
                verifyAssignable(Type.RETURN_ADDRESS, frame.getLocal(iter.byteAt(pos + 1)));
                break;
            case TABLESWITCH:
            case LOOKUPSWITCH:
            case IRETURN:
                verifyAssignable(Type.INTEGER, simplePop(frame));
                break;
            case LRETURN:
                verifyAssignable(Type.LONG, simplePop(frame));
                break;
            case FRETURN:
                verifyAssignable(Type.FLOAT, simplePop(frame));
                break;
            case DRETURN:
                verifyAssignable(Type.DOUBLE, simplePop(frame));
                break;
            case ARETURN:
                try {
                    CtClass returnType = Descriptor.getReturnType(method.getDescriptor(), classPool);
                    verifyAssignable(Type.get(returnType), simplePop(frame));
                } catch (NotFoundException e) {
                   throw new RuntimeException(e);
                }
                break;
            case RETURN:
                break;
            case GETSTATIC:
                evalGetField(opcode, iter.u16bitAt(pos + 1), frame);
                break;
            case PUTSTATIC:
                evalPutField(opcode, iter.u16bitAt(pos + 1), frame);
                break;
            case GETFIELD:
                evalGetField(opcode, iter.u16bitAt(pos + 1), frame);
                break;
            case PUTFIELD:
                evalPutField(opcode, iter.u16bitAt(pos + 1), frame);
                break;
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
            case INVOKESTATIC:
                evalInvokeMethod(opcode, iter.u16bitAt(pos + 1), frame);
                break;
            case INVOKEINTERFACE:
                evalInvokeIntfMethod(opcode, iter.u16bitAt(pos + 1), frame);
                break;
            case 186:
                throw new RuntimeException("Bad opcode 186");
            case NEW:
                frame.push(resolveClassInfo(constPool.getClassInfo(iter.u16bitAt(pos + 1))));
                break;
            case NEWARRAY:
                evalNewArray(pos, iter, frame);
                break;
            case ANEWARRAY:
                evalNewObjectArray(pos, iter, frame);
                break;
            case ARRAYLENGTH: {
                Type array = simplePop(frame);
                if (! array.isArray() && array != Type.UNINIT)
                    throw new BadBytecode("Array length passed a non-array [pos = " + pos + "]: " + array);
                frame.push(Type.INTEGER);
                break;
            }
            case ATHROW:
                verifyAssignable(THROWABLE_TYPE, simplePop(frame));
                break;
            case CHECKCAST:
                verifyAssignable(Type.OBJECT, simplePop(frame));
                frame.push(typeFromDesc(constPool.getClassInfoByDescriptor(iter.u16bitAt(pos + 1))));
                break;
            case INSTANCEOF:
                verifyAssignable(Type.OBJECT, simplePop(frame));
                frame.push(Type.INTEGER);
                break;
            case MONITORENTER:
            case MONITOREXIT:
                verifyAssignable(Type.OBJECT, simplePop(frame));
                break;
            case WIDE:
                evalWide(pos, iter, frame, subroutine);
                break;
            case MULTIANEWARRAY:
                evalNewObjectArray(pos, iter, frame);
                break;
            case IFNULL:
            case IFNONNULL:
                verifyAssignable(Type.OBJECT, simplePop(frame));
                break;
            case GOTO_W:
                break;
            case JSR_W:
                frame.push(Type.RETURN_ADDRESS);
                break;
        }
    }

    private Type zeroExtend(Type type) {
        if (type == Type.SHORT || type == Type.BYTE || type == Type.CHAR || type == Type.BOOLEAN)
            return  Type.INTEGER;

        return type;
    }

    private void evalArrayLoad(Type expectedComponent, Frame frame) throws BadBytecode {
        Type index = frame.pop();
        Type array = frame.pop();

        // Special case, an array defined by aconst_null
        // TODO - we might need to be more inteligent about this
        if (array == Type.UNINIT) {
            verifyAssignable(Type.INTEGER, index);
            if (expectedComponent == Type.OBJECT) {
                simplePush(Type.UNINIT, frame);
            } else {
                simplePush(expectedComponent, frame);
            }
            return;
        }

        Type component = array.getComponent();

        if (component == null)
            throw new BadBytecode("Not an array! [pos = " + lastPos + "]: " + component);

        component = zeroExtend(component);

        verifyAssignable(expectedComponent, component);
        verifyAssignable(Type.INTEGER, index);
        simplePush(component, frame);
    }

    private void evalArrayStore(Type expectedComponent, Frame frame) throws BadBytecode {
        Type value = simplePop(frame);
        Type index = frame.pop();
        Type array = frame.pop();

        if (array == Type.UNINIT) {
            verifyAssignable(Type.INTEGER, index);
            return;
        }

        Type component = array.getComponent();

        if (component == null)
            throw new BadBytecode("Not an array! [pos = " + lastPos + "]: " + component);

        component = zeroExtend(component);

        verifyAssignable(expectedComponent, component);
        verifyAssignable(Type.INTEGER, index);

        // This intentionally only checks for Object on aastore
        // downconverting of an array (no casts)
        // e.g. Object[] blah = new String[];
        //      blah[2] = (Object) "test";
        //      blah[3] = new Integer(); // compiler doesnt catch it (has legal bytecode),
        //                               // but will throw arraystoreexception
        if (expectedComponent == Type.OBJECT) {
            verifyAssignable(expectedComponent, value);
        } else {
            verifyAssignable(component, value);
        }
    }

    private void evalBinaryMath(Type expected, Frame frame) throws BadBytecode {
        Type value2 = simplePop(frame);
        Type value1 = simplePop(frame);

        verifyAssignable(expected, value2);
        verifyAssignable(expected, value1);
        simplePush(value1, frame);
    }

    private void evalGetField(int opcode, int index, Frame frame) throws BadBytecode {
        String desc = constPool.getFieldrefType(index);
        Type type = zeroExtend(typeFromDesc(desc));

        if (opcode == GETFIELD) {
            Type objectType = resolveClassInfo(constPool.getFieldrefClassName(index));
            verifyAssignable(objectType, simplePop(frame));
        }

        simplePush(type, frame);
    }

    private void evalInvokeIntfMethod(int opcode, int index, Frame frame) throws BadBytecode {
        String desc = constPool.getInterfaceMethodrefType(index);
        Type[] types = paramTypesFromDesc(desc);
        int i = types.length;

        while (i > 0)
            verifyAssignable(zeroExtend(types[--i]), simplePop(frame));

        String classInfo = constPool.getInterfaceMethodrefClassName(index);
        Type objectType = resolveClassInfo(classInfo);
        verifyAssignable(objectType, simplePop(frame));

        Type returnType = returnTypeFromDesc(desc);
        if (returnType != Type.VOID)
            simplePush(zeroExtend(returnType), frame);
    }

    private void evalInvokeMethod(int opcode, int index, Frame frame) throws BadBytecode {
        String desc = constPool.getMethodrefType(index);
        Type[] types = paramTypesFromDesc(desc);
        int i = types.length;

        while (i > 0)
            verifyAssignable(zeroExtend(types[--i]), simplePop(frame));

        if (opcode != INVOKESTATIC) {
            Type objectType = resolveClassInfo(constPool.getMethodrefClassName(index));
            verifyAssignable(objectType, simplePop(frame));
        }

        Type returnType = returnTypeFromDesc(desc);
        if (returnType != Type.VOID)
            simplePush(zeroExtend(returnType), frame);
    }


    private void evalLDC(int index, Frame frame) throws BadBytecode {
        int tag = constPool.getTag(index);
        Type type;
        switch (tag) {
        case ConstPool.CONST_String:
            type = STRING_TYPE;
            break;
        case ConstPool.CONST_Integer:
            type = Type.INTEGER;
            break;
        case ConstPool.CONST_Float:
            type = Type.FLOAT;
            break;
        case ConstPool.CONST_Long:
            type = Type.LONG;
            break;
        case ConstPool.CONST_Double:
            type = Type.DOUBLE;
            break;
        case ConstPool.CONST_Class:
            type = CLASS_TYPE;
            break;
        default:
            throw new BadBytecode("bad LDC [pos = " + lastPos + "]: " + tag);
        }

        simplePush(type, frame);
    }

    private void evalLoad(Type expected, int index, Frame frame, Subroutine subroutine) throws BadBytecode {
        Type type = frame.getLocal(index);

        verifyAssignable(expected, type);

        simplePush(type, frame);
        access(index, type, subroutine);
    }

    private void evalNewArray(int pos, CodeIterator iter, Frame frame) throws BadBytecode {
        verifyAssignable(Type.INTEGER, simplePop(frame));
        Type type = null;
        int typeInfo = iter.byteAt(pos + 1);
        switch (typeInfo) {
            case T_BOOLEAN:
                type = getType("boolean[]");
                break;
            case T_CHAR:
                type = getType("char[]");
                break;
            case T_BYTE:
                type = getType("byte[]");
                break;
            case T_SHORT:
                type = getType("short[]");
                break;
            case T_INT:
                type = getType("int[]");
                break;
            case T_LONG:
                type = getType("long[]");
                break;
            case T_FLOAT:
                type = getType("float[]");
                break;
            case T_DOUBLE:
                type = getType("double[]");
                break;
            default:
                throw new BadBytecode("Invalid array type [pos = " + pos + "]: " + typeInfo);

        }

        frame.push(type);
    }

    private void evalNewObjectArray(int pos, CodeIterator iter, Frame frame) throws BadBytecode {
        // Convert to x[] format
        Type type = resolveClassInfo(constPool.getClassInfo(iter.u16bitAt(pos + 1)));
        String name = type.getCtClass().getName();
        int opcode = iter.byteAt(pos);
        int dimensions;

        if (opcode == MULTIANEWARRAY) {
            dimensions = iter.byteAt(pos + 3);
        } else {
            name = name + "[]";
            dimensions = 1;
        }

        while (dimensions-- > 0) {
            verifyAssignable(Type.INTEGER, simplePop(frame));
        }

        simplePush(getType(name), frame);
    }

    private void evalPutField(int opcode, int index, Frame frame) throws BadBytecode {
        String desc = constPool.getFieldrefType(index);
        Type type = zeroExtend(typeFromDesc(desc));

        verifyAssignable(type, simplePop(frame));

        if (opcode == PUTFIELD) {
            Type objectType = resolveClassInfo(constPool.getFieldrefClassName(index));
            verifyAssignable(objectType, simplePop(frame));
        }
    }

    private void evalShift(Type expected, Frame frame) throws BadBytecode {
        Type value2 = simplePop(frame);
        Type value1 = simplePop(frame);

        verifyAssignable(Type.INTEGER, value2);
        verifyAssignable(expected, value1);
        simplePush(value1, frame);
    }

    private void evalStore(Type expected, int index, Frame frame, Subroutine subroutine) throws BadBytecode {
        Type type = simplePop(frame);

        // RETURN_ADDRESS is allowed by ASTORE
        if (! (expected == Type.OBJECT && type == Type.RETURN_ADDRESS))
            verifyAssignable(expected, type);
        simpleSetLocal(index, type, frame);
        access(index, type, subroutine);
    }

    private void evalWide(int pos, CodeIterator iter, Frame frame, Subroutine subroutine) throws BadBytecode {
        int opcode = iter.byteAt(pos + 1);
        int index = iter.u16bitAt(pos + 2);
        switch (opcode) {
            case ILOAD:
                evalLoad(Type.INTEGER, index, frame, subroutine);
                break;
            case LLOAD:
                evalLoad(Type.LONG, index, frame, subroutine);
                break;
            case FLOAD:
                evalLoad(Type.FLOAT, index, frame, subroutine);
                break;
            case DLOAD:
                evalLoad(Type.DOUBLE, index, frame, subroutine);
                break;
            case ALOAD:
                evalLoad(Type.OBJECT, index, frame, subroutine);
                break;
            case ISTORE:
                evalStore(Type.INTEGER, index, frame, subroutine);
                break;
            case LSTORE:
                evalStore(Type.LONG, index, frame, subroutine);
                break;
            case FSTORE:
                evalStore(Type.FLOAT, index, frame, subroutine);
                break;
            case DSTORE:
                evalStore(Type.DOUBLE, index, frame, subroutine);
                break;
            case ASTORE:
                evalStore(Type.OBJECT, index, frame, subroutine);
                break;
            case IINC:
                verifyAssignable(Type.INTEGER, frame.getLocal(index));
                break;
            case RET:
                verifyAssignable(Type.RETURN_ADDRESS, frame.getLocal(index));
                break;
            default:
                throw new BadBytecode("Invalid WIDE operand [pos = " + pos + "]: " + opcode);
        }

    }

    private Type getType(String name) throws BadBytecode {
        try {
            return Type.get(classPool.get(name));
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find class [pos = " + lastPos + "]: " + name);
        }
    }

    private Type[] paramTypesFromDesc(String desc) throws BadBytecode {
        CtClass classes[] = null;
        try {
            classes = Descriptor.getParameterTypes(desc, classPool);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find class in descriptor [pos = " + lastPos + "]: " + e.getMessage());
        }

        if (classes == null)
            throw new BadBytecode("Could not obtain parameters for descriptor [pos = " + lastPos + "]: " + desc);

        Type[] types = new Type[classes.length];
        for (int i = 0; i < types.length; i++)
            types[i] = Type.get(classes[i]);

        return types;
    }

    private Type returnTypeFromDesc(String desc) throws BadBytecode {
        CtClass clazz = null;
        try {
            clazz = Descriptor.getReturnType(desc, classPool);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find class in descriptor [pos = " + lastPos + "]: " + e.getMessage());
        }

        if (clazz == null)
            throw new BadBytecode("Could not obtain return type for descriptor [pos = " + lastPos + "]: " + desc);

        return Type.get(clazz);
    }

    private Type simplePeek(Frame frame) {
        Type type = frame.peek();
        return (type == Type.TOP) ? frame.getStack(frame.getTopIndex() - 1) : type;
    }

    private Type simplePop(Frame frame) {
        Type type = frame.pop();
        return (type == Type.TOP) ? frame.pop() : type;
    }

    private void simplePush(Type type, Frame frame) {
        frame.push(type);
        if (type.getSize() == 2)
            frame.push(Type.TOP);
    }

    private void access(int index, Type type, Subroutine subroutine) {
        if (subroutine == null)
            return;
        subroutine.access(index);
        if (type.getSize() == 2)
            subroutine.access(index + 1);
    }

    private void simpleSetLocal(int index, Type type, Frame frame) {
        frame.setLocal(index, type);
        if (type.getSize() == 2)
            frame.setLocal(index + 1, Type.TOP);
    }

    private Type resolveClassInfo(String info) throws BadBytecode {
        CtClass clazz = null;
        try {
            if (info.charAt(0) == '[') {
                clazz = Descriptor.toCtClass(info, classPool);
            } else {
                clazz = classPool.get(info);
            }

        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find class in descriptor [pos = " + lastPos + "]: " + e.getMessage());
        }

        if (clazz == null)
            throw new BadBytecode("Could not obtain type for descriptor [pos = " + lastPos + "]: " + info);

        return Type.get(clazz);
    }

    private Type typeFromDesc(String desc) throws BadBytecode {
        CtClass clazz = null;
        try {
            clazz = Descriptor.toCtClass(desc, classPool);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find class in descriptor [pos = " + lastPos + "]: " + e.getMessage());
        }

        if (clazz == null)
            throw new BadBytecode("Could not obtain type for descriptor [pos = " + lastPos + "]: " + desc);

        return Type.get(clazz);
    }

    private void verifyAssignable(Type expected, Type type) throws BadBytecode {
        if (! expected.isAssignableFrom(type))
            throw new BadBytecode("Expected type: " + expected + " Got: " + type + " [pos = " + lastPos + "]");
    }
}
