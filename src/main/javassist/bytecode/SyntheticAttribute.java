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
import java.io.IOException;
import java.util.Map;

/**
 * <code>Synthetic_attribute</code>.
 */
public class SyntheticAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"Synthetic"</code>.
     */
    public static final String tag = "Synthetic";

    SyntheticAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    /**
     * Constructs a Synthetic attribute.
     *
     * @param cp                a constant pool table.
     * @param filename          the name of the source file.
     */
    public SyntheticAttribute(ConstPool cp) {
        super(cp, tag, new byte[0]);
    }

    /**
     * Makes a copy.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        should be null.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        return new SyntheticAttribute(newCp);
    }
}
