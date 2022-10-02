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

import javassist.bytecode.CodeIterator;
import javassist.bytecode.Opcode;

/**
 * A set of common utility methods.
 *
 * @author Jason T. Greene
 */
public class Util implements Opcode {
    public static int getJumpTarget(int pos, CodeIterator iter) {
        int opcode = iter.byteAt(pos);
        pos += (opcode == JSR_W || opcode == GOTO_W) ? iter.s32bitAt(pos + 1) : iter.s16bitAt(pos + 1);
        return pos;
    }

    public static boolean isJumpInstruction(int opcode) {
        return (opcode >= IFEQ && opcode <= JSR) || opcode == IFNULL || opcode == IFNONNULL || opcode == JSR_W || opcode == GOTO_W;
    }

    public static boolean isGoto(int opcode) {
        return opcode == GOTO || opcode == GOTO_W;
    }

    public static boolean isJsr(int opcode) {
        return opcode == JSR || opcode == JSR_W;
    }

    public static boolean isReturn(int opcode) {
        return (opcode >= IRETURN && opcode <= RETURN);
    }
}
