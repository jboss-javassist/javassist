/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2005 Shigeru Chiba. All Rights Reserved.
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
 * <code>Signature_attribute</code>.
 */
public class SignatureAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"Signature"</code>.
     */
    public static final String tag = "Signature";

    SignatureAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    /**
     * Constructs a Signature attribute.
     *
     * @param cp                a constant pool table.
     * @param signature         the signature represented by this attribute.
     */
    public SignatureAttribute(ConstPool cp, String signature) {
        super(cp, tag);
        int index = cp.addUtf8Info(signature);
        byte[] bvalue = new byte[2];
        bvalue[0] = (byte)(index >>> 8);
        bvalue[1] = (byte)index;
        set(bvalue);
    }

    /**
     * Returns the signature indicated by <code>signature_index</code>.
     */
    public String getSignature() {
        return getConstPool().getUtf8Info(ByteArray.readU16bit(get(), 0));
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
        return new SignatureAttribute(newCp, getSignature());
    }
}
