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
 * Short integer constant value.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class ShortMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a short constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Integer_info structure.
     */
    public ShortMemberValue(int index, ConstPool cp) {
        super('S', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a short constant value.
     *
     * @param s         the initial value.
     */
    public ShortMemberValue(short s, ConstPool cp) {
        super('S', cp);
        setValue(s);
    }

    /**
     * Constructs a short constant value.  The initial value is 0.
     */
    public ShortMemberValue(ConstPool cp) {
        super('S', cp);
        setValue((short)0);
    }

    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return new Short(getValue());
    }

    Class getType(ClassLoader cl) {
        return short.class;
    }

    /**
     * Obtains the value of the member.
     */
    public short getValue() {
        return (short)cp.getIntegerInfo(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(short newValue) {
        valueIndex = cp.addIntegerInfo(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    public String toString() {
        return Short.toString(getValue());
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
        visitor.visitShortMemberValue(this);
    }
}
