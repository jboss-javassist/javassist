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
 * <code>InnerClasses_attribute</code>.
 */
public class InnerClassesAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"InnerClasses"</code>.
     */
    public static final String tag = "InnerClasses";

    InnerClassesAttribute(ConstPool cp, int n, DataInputStream in)
	throws IOException
    {
	super(cp, n, in);
    }

    private InnerClassesAttribute(ConstPool cp, byte[] info) {
	super(cp, tag, info);
    }

    /**
     * Returns <code>number_of_classes</code>.
     */
    public int length() { return ByteArray.readU16bit(get(), 0); }

    /**
     * Returns <code>classes[nth].inner_class_info_index</code>.
     */
    public int innerClass(int nth) {
	return ByteArray.readU16bit(get(), nth * 8 + 2);
    }

    /**
     * Returns <code>classes[nth].outer_class_info_index</code>.
     */
    public int outerClass(int nth) {
	return ByteArray.readU16bit(get(), nth * 8 + 4);
    }

    /**
     * Returns <code>classes[nth].inner_name_index</code>.
     */
    public int innerName(int nth) {
	return ByteArray.readU16bit(get(), nth * 8 + 6);
    }

    /**
     * Returns <code>classes[nth].inner_class_access_flags</code>.
     */
    public int accessFlags(int nth) {
	return ByteArray.readU16bit(get(), nth * 8 + 8);
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
	byte[] src = get();
	byte[] dest = new byte[src.length];
	ConstPool cp = getConstPool();
	InnerClassesAttribute attr = new InnerClassesAttribute(newCp, dest);
	int n = ByteArray.readU16bit(src, 0);
	ByteArray.write16bit(n, dest, 0);
	int j = 2;
	for (int i = 0; i < n; ++i) {
	    int innerClass = ByteArray.readU16bit(src, j);
	    int outerClass = ByteArray.readU16bit(src, j + 2);
	    int innerName = ByteArray.readU16bit(src, j + 4);
	    int innerAccess = ByteArray.readU16bit(src, j + 6);

	    if (innerClass != 0)
		innerClass = cp.copy(innerClass, newCp, classnames);

	    ByteArray.write16bit(innerClass, dest, j);

	    if (outerClass != 0)
		outerClass = cp.copy(outerClass, newCp, classnames);

	    ByteArray.write16bit(outerClass, dest, j + 2);

	    if (innerName != 0)
		innerName = cp.copy(innerName, newCp, classnames);

	    ByteArray.write16bit(innerName, dest, j + 4);
	    ByteArray.write16bit(innerAccess, dest, j + 6);
	    j += 8;
	}

	return attr;
    }
}
