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
     * @param cp                a constant pool table.
     * @param index             <code>constantvalue_index</code>
     *                          of <code>ConstantValue_attribute</code>.
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
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        pairs of replaced and substituted
     *                          class names.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        int index = getConstPool().copy(getConstantValue(), newCp,
                                        classnames);
        return new ConstantAttribute(newCp, index);
    }
}
