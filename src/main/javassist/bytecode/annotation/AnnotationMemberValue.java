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
 * Nested annotation.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class AnnotationMemberValue extends MemberValue {
    Annotation value;

    /**
     * Constructs an annotation member.  The initial value is not specified.
     */
    public AnnotationMemberValue(ConstPool cp) {
        this(null, cp);
    }

    /**
     * Constructs an annotation member.  The initial value is specified by
     * the first parameter.
     */
    public AnnotationMemberValue(Annotation a, ConstPool cp) {
        super('@', cp);
        value = a;
    }

    Object getValue(ClassLoader cl, ClassPool cp, Method m)
        throws ClassNotFoundException
    {
        return AnnotationImpl.make(cl, getType(cl), cp, value);
    }

    Class getType(ClassLoader cl) throws ClassNotFoundException {
        if (value == null)
            throw new ClassNotFoundException("no type specified");
        else
            return loadClass(cl, value.getTypeName());
    }

    /**
     * Obtains the value.
     */
    public Annotation getValue() {
        return value;
    }

    /**
     * Sets the value of this member.
     */
    public void setValue(Annotation newValue) {
        value = newValue;
    }

    /**
     * Obtains the string representation of this object.
     */
    public String toString() {
        return value.toString();
    }

    /**
     * Writes the value.
     */
    public void write(AnnotationsWriter writer) throws IOException {
        writer.annotationValue();
        value.write(writer);
    }

    /**
     * Accepts a visitor.
     */
    public void accept(MemberValueVisitor visitor) {
        visitor.visitAnnotationMemberValue(this);
    }
}
