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

package javassist.bytecode;

import java.io.DataInputStream;
import java.util.Map;
import java.io.IOException;

/**
 * <code>ConstantValue_attribute</code>.
 */
public class ConstantAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"ConstantValue"</code>.
     */
    public static final String tag = "ConstantValue";

    ConstantAttribute(ConstPool cp, int n, DataInputStream in)
	throws IOException
    {
	super(cp, n, in);
    }

    /**
     * Constructs a ConstantValue attribute.
     *
     * @param cp		a constant pool table.
     * @param index		<code>constantvalue_index</code>
     *				of <code>ConstantValue_attribute</code>.
     */
    public ConstantAttribute(ConstPool cp, int index) {
	super(cp, tag);
	byte[] bvalue = new byte[2];
	bvalue[0] = (byte)(index >>> 8);
	bvalue[1] = (byte)index;
	set(bvalue);
    }

    /**
     * Returns <code>constantvalue_index</code>.
     */
    public int getConstantValue() {
	return ByteArray.readU16bit(get(), 0);
    }

    /**
     * Makes a copy.  Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * @param newCp	the constant pool table used by the new copy.
     * @param classnames	pairs of replaced and substituted
     *				class names.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
	int index = getConstPool().copy(getConstantValue(), newCp,
					classnames);
	return new ConstantAttribute(newCp, index);
    }
}
