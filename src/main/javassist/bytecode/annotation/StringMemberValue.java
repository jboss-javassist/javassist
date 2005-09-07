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

import javassist.ClassPool;
import javassist.bytecode.ConstPool;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * String constant value.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class StringMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a string constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Utf8_info structure.
     */
    public StringMemberValue(int index, ConstPool cp) {
        super('s', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a string constant value.
     *
     * @param str         the initial value.
     */
    public StringMemberValue(String str, ConstPool cp) {
        super('s', cp);
        setValue(str);
    }

    /**
     * Constructs a string constant value.  The initial value is "".
     */
    public StringMemberValue(ConstPool cp) {
        super('s', cp);
        setValue("");
    }

    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return getValue();
    }

    Class getType(ClassLoader cl) {
        return String.class;
    }

    /**
     * Obtains the value of the member.
     */
    public String getValue() {
        return cp.getUtf8Info(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(String newValue) {
        valueIndex = cp.addUtf8Info(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    public String toString() {
        return "\"" + getValue() + "\"";
    }

    /**
     * Writes the value.
     */
    public void write(AnnotationsWriter writer) throws IOException {
        writer.constValueIndex(getValue());
    }

    /**
     * Accepts a visitor.
     */
    public void accept(MemberValueVisitor visitor) {
        visitor.visitStringMemberValue(this);
    }
}
