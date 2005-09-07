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
 * Char constant value.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class CharMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a char constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Integer_info structure.
     */
    public CharMemberValue(int index, ConstPool cp) {
        super('C', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a char constant value.
     *
     * @param c     the initial value.
     */
    public CharMemberValue(char c, ConstPool cp) {
        super('C', cp);
        setValue(c);
    }

    /**
     * Constructs a char constant value.  The initial value is '\0'.
     */
    public CharMemberValue(ConstPool cp) {
        super('C', cp);
        setValue('\0');
    }

    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return new Character(getValue());
    }

    Class getType(ClassLoader cl) {
        return char.class;
    }

    /**
     * Obtains the value of the member.
     */
    public char getValue() {
        return (char)cp.getIntegerInfo(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(char newValue) {
        valueIndex = cp.addIntegerInfo(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    public String toString() {
        return Character.toString(getValue());
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
        visitor.visitCharMemberValue(this);
    }
}
