/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 2004 Bill Burke. All Rights Reserved.
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

package javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import javassist.ClassPool;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;

/**
 * Enum constant value.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class EnumMemberValue extends MemberValue {
    int typeIndex, valueIndex;

    /**
     * Constructs an enum constant value.  The initial value is specified
     * by the constant pool entries at the given indexes.
     *
     * @param type      the index of a CONSTANT_Utf8_info structure
     *                  representing the enum type.
     * @param value     the index of a CONSTANT_Utf8_info structure.
     *                  representing the enum value.
     */
    public EnumMemberValue(int type, int value, ConstPool cp) {
        super('e', cp);
        this.typeIndex = type;
        this.valueIndex = value;
    }

    /**
     * Constructs an enum constant value.
     * The initial value is not specified.
     */
    public EnumMemberValue(ConstPool cp) {
        super('e', cp);
        typeIndex = valueIndex = 0;
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m)
        throws ClassNotFoundException
    {
        try {
            return getType(cl).getField(getValue()).get(null);
        }
        catch (NoSuchFieldException e) {
            throw new ClassNotFoundException(getType() + "." + getValue());
        }
        catch (IllegalAccessException e) {
            throw new ClassNotFoundException(getType() + "." + getValue());
        }
    }

    @Override
    Class<?> getType(ClassLoader cl) throws ClassNotFoundException {
        return loadClass(cl, getType());
    }

    @Override
    public void renameClass(String oldname, String newname) {
        String type = cp.getUtf8Info(typeIndex);
        String newType = Descriptor.rename(type, oldname, newname);
        setType(Descriptor.toClassName(newType));
    }

    @Override
    public void renameClass(Map<String, String> classnames) {
        String type = cp.getUtf8Info(typeIndex);
        String newType = Descriptor.rename(type, classnames);
        setType(Descriptor.toClassName(newType));
    }

    /**
     * Obtains the enum type name.
     *
     * @return a fully-qualified type name.
     */
    public String getType() {
        return Descriptor.toClassName(cp.getUtf8Info(typeIndex));
    }

    /**
     * Changes the enum type name.
     *
     * @param typename a fully-qualified type name.
     */
    public void setType(String typename) {
        typeIndex = cp.addUtf8Info(Descriptor.of(typename));
    }

    /**
     * Obtains the name of the enum constant value.
     */
    public String getValue() {
        return cp.getUtf8Info(valueIndex);
    }

    /**
     * Changes the name of the enum constant value.
     */
    public void setValue(String name) {
        valueIndex = cp.addUtf8Info(name);
    }

    @Override
    public String toString() {
        return getType() + "." + getValue();
    }

    /**
     * Writes the value.
     */
    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.enumConstValue(cp.getUtf8Info(typeIndex), getValue());
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitEnumMemberValue(this);
    }
}
