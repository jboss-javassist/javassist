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
