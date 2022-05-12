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
 * The value of a member declared in an annotation.
 *
 * @see Annotation#getMemberValue(String)
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public abstract class MemberValue {
    ConstPool cp;
    char tag;

    MemberValue(char tag, ConstPool cp) {
        this.cp = cp;
        this.tag = tag;
    }

    /**
     * Returns the value.  If the value type is a primitive type, the
     * returned value is boxed.
     */
    abstract Object getValue(ClassLoader cl, ClassPool cp, Method m)
        throws ClassNotFoundException;

    abstract Class<?> getType(ClassLoader cl) throws ClassNotFoundException;

    static Class<?> loadClass(ClassLoader cl, String classname)
        throws ClassNotFoundException, NoSuchClassError
    {
        try {
            return Class.forName(convertFromArray(classname), true, cl);
        }
        catch (LinkageError e) {
            throw new NoSuchClassError(classname, e);
        }
    }

    private static String convertFromArray(String classname)
    {
        int index = classname.indexOf("[]");
        if (index != -1) {
            String rawType = classname.substring(0, index);
            StringBuilder sb = new StringBuilder(Descriptor.of(rawType));
            while (index != -1) {
                sb.insert(0, '[');
                index = classname.indexOf("[]", index + 1);
            }
            return sb.toString().replace('/', '.');
        }
        return classname;
    }

    /* The following two methods are used to implement
     * ClassFile.renameClass().
     * Only ArrayMemberValue, ClassMemberValue, EnumMemberValue
     * override these methods.
     */
    public void renameClass(String oldname, String newname) {}
    public void renameClass(Map<String, String> classnames) {}

    /**
     * Accepts a visitor.
     */
    public abstract void accept(MemberValueVisitor visitor);

    /**
     * Writes the value.
     */
    public abstract void write(AnnotationsWriter w) throws IOException;
}


