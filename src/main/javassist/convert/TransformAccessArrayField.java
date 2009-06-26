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
package javassist.convert;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.CodeConverter.ArrayAccessReplacementMethodNames;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.analysis.Analyzer;
import javassist.bytecode.analysis.Frame;

/**
 * A transformer which replaces array access with static method invocations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Jason T. Greene
 * @version $Revision: 1.8 $
 */
public final class TransformAccessArrayField extends Transformer {
    private final String methodClassname;
    private final ArrayAccessReplacementMethodNames names;
    private Frame[] frames;
    private int offset;

    public TransformAccessArrayField(Transformer next, String methodClassname,
            ArrayAccessReplacementMethodNames names) throws NotFoundException {
        super(next);
        this.methodClassname = methodClassname;
        this.names = names;

    }

    public void initialize(ConstPool cp, CtClass clazz, MethodInfo minfo) throws CannotCompileException {
        /*
         * This transformer must be isolated from other transformers, since some
         * of them affect the local variable and stack maximums without updating
         * the code attribute to reflect the changes. This screws up the
         * data-flow analyzer, since it relies on consistent code state. Even
         * if the attribute values were updated correctly, we would have to
         * detect it, and redo analysis, which is not cheap. Instead, we are
         * better off doing all changes in initialize() before everyone else has
         * a chance to muck things up.
         */
        CodeIterator iterator = minfo.getCodeAttribute().iterator();
        while (iterator.hasNext()) {
            try {
                int pos = iterator.next();
                int c = iterator.byteAt(pos);

                if (c == AALOAD)
                    initFrames(clazz, minfo);

                if (c == AALOAD || c == BALOAD || c == CALOAD || c == DALOAD
                        || c == FALOAD || c == IALOAD || c == LALOAD
                        || c == SALOAD) {
                    pos = replace(cp, iterator, pos, c, getLoadReplacementSignature(c));
                } else if (c == AASTORE || c == BASTORE || c == CASTORE
                        || c == DASTORE || c == FASTORE || c == IASTORE
                        || c == LASTORE || c == SASTORE) {
                    pos = replace(cp, iterator, pos, c, getStoreReplacementSignature(c));
                }

            } catch (Exception e) {
                throw new CannotCompileException(e);
            }
        }
    }

    public void clean() {
        frames = null;
        offset = -1;
    }

    public int transform(CtClass tclazz, int pos, CodeIterator iterator,
            ConstPool cp) throws BadBytecode {
        // Do nothing, see above comment
        return pos;
    }

    private Frame getFrame(int pos) throws BadBytecode {
        return frames[pos - offset]; // Adjust pos
    }

    private void initFrames(CtClass clazz, MethodInfo minfo) throws BadBytecode {
        if (frames == null) {
            frames = ((new Analyzer())).analyze(clazz, minfo);
            offset = 0; // start tracking changes
        }
    }

    private int updatePos(int pos, int increment) {
        if (offset > -1)
            offset += increment;

        return pos + increment;
    }

    private String getTopType(int pos) throws BadBytecode {
        Frame frame = getFrame(pos);
        if (frame == null)
            return null;

        CtClass clazz = frame.peek().getCtClass();
        return clazz != null ? Descriptor.toJvmName(clazz) : null;
    }

    private int replace(ConstPool cp, CodeIterator iterator, int pos,
            int opcode, String signature) throws BadBytecode {
        String castType = null;
        String methodName = getMethodName(opcode);
        if (methodName != null) {
            // See if the object must be cast
            if (opcode == AALOAD) {
                castType = getTopType(iterator.lookAhead());
                // Do not replace an AALOAD instruction that we do not have a type for
                // This happens when the state is guaranteed to be null (Type.UNINIT)
                // So we don't really care about this case.
                if (castType == null)
                    return pos;
                if ("java/lang/Object".equals(castType))
                    castType = null;
            }

            // The gap may include extra padding
            // Write a nop in case the padding pushes the instruction forward
            iterator.writeByte(NOP, pos);
            CodeIterator.Gap gap
                = iterator.insertGapAt(pos, castType != null ? 5 : 2, false);
            pos = gap.position;
            int mi = cp.addClassInfo(methodClassname);
            int methodref = cp.addMethodrefInfo(mi, methodName, signature);
            iterator.writeByte(INVOKESTATIC, pos);
            iterator.write16bit(methodref, pos + 1);

            if (castType != null) {
                int index = cp.addClassInfo(castType);
                iterator.writeByte(CHECKCAST, pos + 3);
                iterator.write16bit(index, pos + 4);
            }

            pos = updatePos(pos, gap.length);
        }

        return pos;
    }

    private String getMethodName(int opcode) {
        String methodName = null;
        switch (opcode) {
        case AALOAD:
            methodName = names.objectRead();
            break;
        case BALOAD:
            methodName = names.byteOrBooleanRead();
            break;
        case CALOAD:
            methodName = names.charRead();
            break;
        case DALOAD:
            methodName = names.doubleRead();
            break;
        case FALOAD:
            methodName = names.floatRead();
            break;
        case IALOAD:
            methodName = names.intRead();
            break;
        case SALOAD:
            methodName = names.shortRead();
            break;
        case LALOAD:
            methodName = names.longRead();
            break;
        case AASTORE:
            methodName = names.objectWrite();
            break;
        case BASTORE:
            methodName = names.byteOrBooleanWrite();
            break;
        case CASTORE:
            methodName = names.charWrite();
            break;
        case DASTORE:
            methodName = names.doubleWrite();
            break;
        case FASTORE:
            methodName = names.floatWrite();
            break;
        case IASTORE:
            methodName = names.intWrite();
            break;
        case SASTORE:
            methodName = names.shortWrite();
            break;
        case LASTORE:
            methodName = names.longWrite();
            break;
        }

        if (methodName.equals(""))
            methodName = null;

        return methodName;
    }

    private String getLoadReplacementSignature(int opcode) throws BadBytecode {
        switch (opcode) {
        case AALOAD:
            return "(Ljava/lang/Object;I)Ljava/lang/Object;";
        case BALOAD:
            return "(Ljava/lang/Object;I)B";
        case CALOAD:
            return "(Ljava/lang/Object;I)C";
        case DALOAD:
            return "(Ljava/lang/Object;I)D";
        case FALOAD:
            return "(Ljava/lang/Object;I)F";
        case IALOAD:
            return "(Ljava/lang/Object;I)I";
        case SALOAD:
            return "(Ljava/lang/Object;I)S";
        case LALOAD:
            return "(Ljava/lang/Object;I)J";
        }

        throw new BadBytecode(opcode);
    }

    private String getStoreReplacementSignature(int opcode) throws BadBytecode {
        switch (opcode) {
        case AASTORE:
            return "(Ljava/lang/Object;ILjava/lang/Object;)V";
        case BASTORE:
            return "(Ljava/lang/Object;IB)V";
        case CASTORE:
            return "(Ljava/lang/Object;IC)V";
        case DASTORE:
            return "(Ljava/lang/Object;ID)V";
        case FASTORE:
            return "(Ljava/lang/Object;IF)V";
        case IASTORE:
            return "(Ljava/lang/Object;II)V";
        case SASTORE:
            return "(Ljava/lang/Object;IS)V";
        case LASTORE:
            return "(Ljava/lang/Object;IJ)V";
        }

        throw new BadBytecode(opcode);
    }
}
