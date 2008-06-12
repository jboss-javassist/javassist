/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba, and others. All Rights Reserved.
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
package javassist.bytecode.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ExceptionTable;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

/**
 * Discovers the subroutines in a method, and tracks all callers.
 *
 * @author Jason T. Greene
 */
public class SubroutineScanner implements Opcode {

    private Subroutine[] subroutines;
    Map subTable = new HashMap();
    Set done = new HashSet();


    public Subroutine[] scan(MethodInfo method) throws BadBytecode {
        CodeAttribute code = method.getCodeAttribute();
        CodeIterator iter = code.iterator();

        subroutines = new Subroutine[code.getCodeLength()];
        subTable.clear();
        done.clear();

        scan(0, iter, null);

        ExceptionTable exceptions = code.getExceptionTable();
        for (int i = 0; i < exceptions.size(); i++) {
            int handler = exceptions.handlerPc(i);
            // If an exception is thrown in subroutine, the handler
            // is part of the same subroutine.
            scan(handler, iter, subroutines[exceptions.startPc(i)]);
        }

        return subroutines;
    }

    private void scan(int pos, CodeIterator iter, Subroutine sub) throws BadBytecode {
        // Skip already processed blocks
        if (done.contains(new Integer(pos)))
            return;

        done.add(new Integer(pos));

        int old = iter.lookAhead();
        iter.move(pos);

        boolean next;
        do {
            pos = iter.next();
            next = scanOp(pos, iter, sub) && iter.hasNext();
        } while (next);

        iter.move(old);
    }

    private boolean scanOp(int pos, CodeIterator iter, Subroutine sub) throws BadBytecode {
        subroutines[pos] = sub;

        int opcode = iter.byteAt(pos);

        if (opcode == TABLESWITCH) {
            scanTableSwitch(pos, iter, sub);

            return false;
        }

        if (opcode == LOOKUPSWITCH) {
            scanLookupSwitch(pos, iter, sub);

            return false;
        }

        // All forms of return and throw end current code flow
        if (Util.isReturn(opcode) || opcode == RET || opcode == ATHROW)
            return false;

        if (Util.isJumpInstruction(opcode)) {
            int target = Util.getJumpTarget(pos, iter);
            if (opcode == JSR || opcode == JSR_W) {
                Subroutine s = (Subroutine) subTable.get(new Integer(target));
                if (s == null) {
                    s = new Subroutine(target, pos);
                    subTable.put(new Integer(target), s);
                    scan(target, iter, s);
                } else {
                    s.addCaller(pos);
                }
            } else {
                scan(target, iter, sub);

                // GOTO ends current code flow
                if (Util.isGoto(opcode))
                    return false;
            }
        }

        return true;
    }

    private void scanLookupSwitch(int pos, CodeIterator iter, Subroutine sub) throws BadBytecode {
        int index = (pos & ~3) + 4;
        // default
        scan(pos + iter.s32bitAt(index), iter, sub);
        int npairs = iter.s32bitAt(index += 4);
        int end = npairs * 8 + (index += 4);

        // skip "match"
        for (index += 4; index < end; index += 8) {
            int target = iter.s32bitAt(index) + pos;
            scan(target, iter, sub);
        }
    }

    private void scanTableSwitch(int pos, CodeIterator iter, Subroutine sub) throws BadBytecode {
        // Skip 4 byte alignment padding
        int index = (pos & ~3) + 4;
        // default
        scan(pos + iter.s32bitAt(index), iter, sub);
        int low = iter.s32bitAt(index += 4);
        int high = iter.s32bitAt(index += 4);
        int end = (high - low + 1) * 4 + (index += 4);

        // Offset table
        for (; index < end; index += 4) {
            int target = iter.s32bitAt(index) + pos;
            scan(target, iter, sub);
        }
    }


}
