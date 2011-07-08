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

package javassist.convert;

import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.*;

public class TransformAfter extends TransformBefore {
    public TransformAfter(Transformer next,
                           CtMethod origMethod, CtMethod afterMethod)
        throws NotFoundException
    {
        super(next, origMethod, afterMethod);
    }

    protected int match2(int pos, CodeIterator iterator) throws BadBytecode {
        iterator.move(pos);
        iterator.insert(saveCode);
        iterator.insert(loadCode);
        int p = iterator.insertGap(3);
        iterator.setMark(p);
        iterator.insert(loadCode);
        pos = iterator.next();
        p = iterator.getMark();
        iterator.writeByte(iterator.byteAt(pos), p);
        iterator.write16bit(iterator.u16bitAt(pos + 1), p + 1);
        iterator.writeByte(INVOKESTATIC, pos);
        iterator.write16bit(newIndex, pos + 1);
        iterator.move(p);
        return iterator.next();
    }
}
