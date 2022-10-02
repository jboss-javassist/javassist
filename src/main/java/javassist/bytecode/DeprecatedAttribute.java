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

package javassist.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * <code>Deprecated_attribute</code>.
 */
public class DeprecatedAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"Deprecated"</code>.
     */
    public static final String tag = "Deprecated";

    DeprecatedAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    /**
     * Constructs a Deprecated attribute.
     *
     * @param cp                a constant pool table.
     */
    public DeprecatedAttribute(ConstPool cp) {
        super(cp, tag, new byte[0]);
    }

    /**
     * Makes a copy.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        should be null.
     */
    @Override
    public AttributeInfo copy(ConstPool newCp, Map<String,String> classnames) {
        return new DeprecatedAttribute(newCp);
    }
}
