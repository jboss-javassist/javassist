/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 2004 Bill Burke. All Rights Reserved.
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
package javassist.bytecode.annotation;

import javassist.bytecode.ConstPool;
import java.io.IOException;

/**
 * Array member.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class ArrayMemberValue extends MemberValue {
    MemberValue type;
    MemberValue[] values;

    /**
     * Constructs an array.  The initial value or type are not specified.
     */
    public ArrayMemberValue(ConstPool cp) {
        super('[', cp);
        type = null;
        values = null;
    }

    /**
     * Constructs an array.  The initial value is not specified.
     *
     * @param t         the type of the array elements.
     */
    public ArrayMemberValue(MemberValue t, ConstPool cp) {
        super('[', cp);
        type = t;
        values = null;
    }

    /**
     * Obtains the type of the elements.
     *
     * @return null if the type is not specified.
     */
    public MemberValue getType() {
        return type;
    }

    /**
     * Obtains the elements of the array.
     */
    public MemberValue[] getValue() {
        return values;
    }

    /**
     * Sets the elements of the array.
     */
    public void setValue(MemberValue[] elements) {
        values = elements;
        if (elements != null && elements.length > 0)
            type = elements[0];
    }

    /**
     * Obtains the string representation of this object.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                buf.append(values[i].toString());
                if (i + 1 < values.length)
                    buf.append(", ");
                }
        }

        buf.append("}");
        return buf.toString();
    }

    /**
     * Writes the value.
     */
    public void write(AnnotationsWriter writer) throws IOException {
        int num = values.length;
        writer.arrayValue(num);
        for (int i = 0; i < num; ++i)
            values[i].write(writer);
    }

    /**
     * Accepts a visitor.
     */
    public void accept(MemberValueVisitor visitor) {
        visitor.visitArrayMemberValue(this);
    }
}
