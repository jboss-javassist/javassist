/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2004 Shigeru Chiba. All Rights Reserved.
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

import java.io.IOException;
import java.io.DataInputStream;

import javassist.bytecode.annotation.*;

/**
 * A class representing <code>RuntimeVisibleAnnotations_attribute</code> and
 * <code>RuntimeInvisibleAnnotations_attribute</code>.
 */
public class ParameterAnnotationsAttribute extends AnnotationsAttribute {
    /**
     * The name of the <code>RuntimeVisibleParameterAnnotations</code>
     * attribute.
     */
    public static final String visibleTag
        = "RuntimeVisibleParameterAnnotations";

    /**
     * The name of the <code>RuntimeInvisibleParameterAnnotations</code>
     * attribute.
     */
    public static final String invisibleTag
        = "RuntimeInvisibleParameterAnnotations";
    /**
     * Constructs
     * a <code>Runtime(In)VisisbleParameterAnnotations_attribute</code>.
     *
     * @param cp            constant pool
     * @param attrname      attribute name (<code>visibleTag</code> or
     *                      <code>invisibleTag</code>).
     * @param info          the contents of this attribute.  It does not
     *                      include <code>attribute_name_index</code> or
     *                      <code>attribute_length</code>.
     */
    public ParameterAnnotationsAttribute(ConstPool cp, String attrname,
                                         byte[] info) {
        super(cp, attrname, info);
    }

    /**
     * Constructs an empty
     * <code>Runtime(In)VisisbleParameterAnnotations_attribute</code>.
     *
     * @param cp            constant pool
     * @param attrname      attribute name (<code>visibleTag</code> or
     *                      <code>invisibleTag</code>).
     */
    public ParameterAnnotationsAttribute(ConstPool cp, String attrname) {
        this(cp, attrname, new byte[] { 0 });
    }

    /**
     * @param n     the attribute name.
     */
    ParameterAnnotationsAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    /**
     * Returns <code>num_parameters</code>. 
     */
    public int numParameters() {
        return info[0] & 0xff;
    }

    AnnotationsAttribute makeCopy(ConstPool newCp, byte[] info) {
        return new ParameterAnnotationsAttribute(newCp, getName(), info);
    }

    /**
     * Runs the parser to analyze this annotation.
     *
     * @see AnnotationsWriter
     */
    public void accept(AnnotationsVisitor visitor) throws Exception {
        int numParam = numParameters();
        int pos = 1;
        visitor.beginParameters(numParam);
        for (int i = 0; i < numParam; ++i) {
            int num= ByteArray.readU16bit(info, pos);
            visitor.beginAnnotationsArray(num);
            for (int j = 0; j < num; ++j) {
                pos = readAnnotation(visitor, pos);
            }

            visitor.endAnnotationsArray();
        }

        visitor.endParameters();
    }
}
