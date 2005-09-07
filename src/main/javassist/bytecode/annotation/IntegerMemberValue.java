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
 * Integer constant value.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class IntegerMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs an int constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Integer_info structure.
     */
    public IntegerMemberValue(int index, ConstPool cp) {
        super('I', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs an int constant value.
     * Note that this constructor receives <b>the initial value
     * as the second parameter</b>
     * unlike the corresponding constructors in the sibling classes.
     * This is for making a difference from the constructor that receives
     * an index into the constant pool table as the first parameter.
     * Note that the index is also int type.
     *
     * @param value         the initial value.
     */
    public IntegerMemberValue(ConstPool cp, int value) {
        super('I', cp);
        setValue(value);
    }

    /**
     * Constructs an int constant value.  The initial value is 0.
     */
    public IntegerMemberValue(ConstPool cp) {
        super('I', cp);
        setValue(0);
    }

    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return new Integer(getValue());
    }

    Class getType(ClassLoader cl) {
        return int.class;
    }

    /**
     * Obtains the value of the member.
     */
    public int getValue() {
        return cp.getIntegerInfo(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(int newValue) {
        valueIndex = cp.addIntegerInfo(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    public String toString() {
        return Integer.toString(getValue());
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
        visitor.visitIntegerMemberValue(this);
    }
}
