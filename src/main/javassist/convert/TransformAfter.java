/*
 * This file is part of the Javassist toolkit.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * either http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is Javassist.
 *
 * The Initial Developer of the Original Code is Shigeru Chiba.  Portions
 * created by Shigeru Chiba are Copyright (C) 1999-2003 Shigeru Chiba.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * The development of this software is supported in part by the PRESTO
 * program (Sakigake Kenkyu 21) of Japan Science and Technology Corporation.
 */

package javassist.convert;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.*;
import javassist.CannotCompileException;

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
	iterator.insert(loadCode);
	pos = iterator.next();
	iterator.writeByte(iterator.byteAt(pos), p);
	iterator.write16bit(iterator.u16bitAt(pos + 1), p + 1);
	iterator.writeByte(INVOKESTATIC, pos);
	iterator.write16bit(newIndex, pos + 1);
	iterator.move(p);
	return iterator.next();
    }
}
