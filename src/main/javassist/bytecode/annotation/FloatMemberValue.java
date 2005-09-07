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
 * Floating-point number constant value.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 * @version $Revision: 1.7 $
 */
public class FloatMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a float constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Float_info structure.
     */
    public FloatMemberValue(int index, ConstPool cp) {
        super('F', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a float constant value.
     *
     * @param f         the initial value.
     */
    public FloatMemberValue(float f, ConstPool cp) {
        super('F', cp);
        setValue(f);
    }

    /**
     * Constructs a float constant value.  The initial value is 0.0.
     */
    public FloatMemberValue(ConstPool cp) {
        super('F', cp);
        setValue(0.0F);
    }

    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return new Float(getValue());
    }

    Class getType(ClassLoader cl) {
        return float.class;
    }

    /**
     * Obtains the value of the member.
     */
    public float getValue() {
        return cp.getFloatInfo(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(float newValue) {
        valueIndex = cp.addFloatInfo(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    public String toString() {
        return Float.toString(getValue());
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
        visitor.visitFloatMemberValue(this);
    }
}
