/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
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
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        pairs of replaced and substituted
     *                          class names.
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
