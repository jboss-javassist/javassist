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

    /**
     * Returns true if the given object represents the same constant value
     * as this object.
     */
    public boolean equals(Object obj) {
        if (obj instanceof ConstantAttribute) {
            ConstantAttribute sa = (ConstantAttribute)obj;
            return sa.getValue().equals(getValue()); 
        }
        else
            return false;
    }

	/**
	 * Dumps the content of the attribute
	 * @return human readable content
	 */
    public String Dump()
    {
    	Object value = getValue();
    	return "ConstantValue: " + value.getClass() + ": " + value.toString() + ";";
    }

    Object getValue()
    {
    	int  index = getConstantValue();  

        if (index == 0)
            return null;

        switch (constPool.getTag(index)) {
            case ConstPool.CONST_Long :
                return new Long(constPool.getLongInfo(index));
            case ConstPool.CONST_Float :
                return new Float(constPool.getFloatInfo(index));
            case ConstPool.CONST_Double :
                return new Double(constPool.getDoubleInfo(index));
            case ConstPool.CONST_Integer :
                int value = constPool.getIntegerInfo(index);
                    return new Integer(value);
            case ConstPool.CONST_String :
                return constPool.getStringInfo(index);
            default :
                throw new RuntimeException("bad tag: " + constPool.getTag(index)
                                           + " at " + index);
        }
    }

}
