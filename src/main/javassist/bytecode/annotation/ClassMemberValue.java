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
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.SignatureAttribute;

/**
 * Class value.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class ClassMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a class value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index the index of a CONSTANT_Utf8_info structure.
     */
    public ClassMemberValue(int index, ConstPool cp) {
        super('c', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a class value.
     *
     * @param className         the initial value.
     */
    public ClassMemberValue(String className, ConstPool cp) {
        super('c', cp);
        setValue(className);
    }

    /**
     * Constructs a class value.
     * The initial value is java.lang.Class.
     */
    public ClassMemberValue(ConstPool cp) {
        super('c', cp);
        setValue("java.lang.Class");
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m)
            throws ClassNotFoundException {
        final String classname = getValue();
        if (classname.equals("void"))
            return void.class;
        else if (classname.equals("int"))
            return int.class;
        else if (classname.equals("byte"))
            return byte.class;
        else if (classname.equals("long"))
            return long.class;
        else if (classname.equals("double"))
            return double.class;
        else if (classname.equals("float"))
            return float.class;
        else if (classname.equals("char"))
            return char.class;
        else if (classname.equals("short"))
            return short.class;
        else if (classname.equals("boolean"))
            return boolean.class;
        else
            return loadClass(cl, classname);
    }

    @Override
    Class<?> getType(ClassLoader cl) throws ClassNotFoundException {
        return loadClass(cl, "java.lang.Class");
    }

    @Override
    public void renameClass(String oldname, String newname) {
        String value = cp.getUtf8Info(valueIndex);
        String newValue = Descriptor.rename(value, oldname, newname);
        setValue(Descriptor.toClassName(newValue));
    }

    @Override
    public void renameClass(Map<String, String> classnames) {
        String value = cp.getUtf8Info(valueIndex);
        String newValue = Descriptor.rename(value, classnames);
        setValue(Descriptor.toClassName(newValue));
    }

    /**
     * Obtains the value of the member.
     *
     * @return fully-qualified class name.
     */
    public String getValue() {
        String v = cp.getUtf8Info(valueIndex);
        try {
            return SignatureAttribute.toTypeSignature(v).jvmTypeName();
        } catch (BadBytecode e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the value of the member.
     *
     * @param newClassName      fully-qualified class name.
     */
    public void setValue(String newClassName) {
        String setTo = Descriptor.of(newClassName);
        valueIndex = cp.addUtf8Info(setTo);
    }

    /**
     * Obtains the string representation of this object.
     */
    @Override
    public String toString() {
        return getValue().replace('$', '.') + ".class";
    }

    /**
     * Writes the value.
     */
    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.classInfoIndex(cp.getUtf8Info(valueIndex));
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitClassMemberValue(this);
    }
}
