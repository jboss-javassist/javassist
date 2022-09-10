/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
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

package javassist.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

import javassist.CtClass;
import javassist.bytecode.annotation.AnnotationsWriter;
import javassist.bytecode.annotation.MemberValue;

/**
 * A class representing <code>AnnotationDefault_attribute</code>.
 *
 * <p>For example, if you declare the following annotation type:
 *
 * <pre>
 * &#64;interface Author {
 *   String name() default "Shakespeare";
 *   int age() default 99;
 * }
 * </pre>
 *
 * <p>The defautl values of <code>name</code> and <code>age</code>
 * are stored as annotation default attributes in <code>Author.class</code>.
 * The following code snippet obtains the default value of <code>name</code>:
 * 
 * <pre>
 * ClassPool pool = ...
 * CtClass cc = pool.get("Author");
 * CtMethod cm = cc.getDeclaredMethod("age");
 * MethodInfo minfo = cm.getMethodInfo();
 * AnnotationDefaultAttribute ada
 *         = (AnnotationDefaultAttribute)
 *           minfo.getAttribute(AnnotationDefaultAttribute.tag);
 * MemberValue value = ada.getDefaultValue());    // default value of age
 * </pre>
 *
 * <p>If the following statement is executed after the code above,
 * the default value of age is set to 80:
 *
 * <pre>
 * ada.setDefaultValue(new IntegerMemberValue(minfo.getConstPool(), 80));
 * </pre>
 *
 * @see AnnotationsAttribute
 * @see javassist.bytecode.annotation.MemberValue
 */

public class AnnotationDefaultAttribute extends AttributeInfo {
    /**
     * The name of the <code>AnnotationDefault</code> attribute.
     */
    public static final String tag = "AnnotationDefault";

    /**
     * Constructs an <code>AnnotationDefault_attribute</code>.
     *
     * @param cp            constant pool
     * @param info          the contents of this attribute.  It does not
     *                      include <code>attribute_name_index</code> or
     *                      <code>attribute_length</code>.
     */
    public AnnotationDefaultAttribute(ConstPool cp, byte[] info) {
        super(cp, tag, info);
    }

    /**
     * Constructs an empty <code>AnnotationDefault_attribute</code>.
     * The default value can be set by <code>setDefaultValue()</code>.
     *
     * @param cp            constant pool
     * @see #setDefaultValue(javassist.bytecode.annotation.MemberValue)
     */
    public AnnotationDefaultAttribute(ConstPool cp) {
        this(cp, new byte[] { 0, 0 });
    }

    /**
     * @param n     the attribute name.
     */
    AnnotationDefaultAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    /**
     * Copies this attribute and returns a new copy.
     */
    @Override
    public AttributeInfo copy(ConstPool newCp, Map<String,String> classnames) {
        AnnotationsAttribute.Copier copier
            = new AnnotationsAttribute.Copier(info, constPool, newCp, classnames);
        try {
            copier.memberValue(0);
            return new AnnotationDefaultAttribute(newCp, copier.close());
        }
        catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    void renameClass(String oldname, String newname) {
        try {
            MemberValue defaultValue = getDefaultValue();
            defaultValue.renameClass(oldname, newname);
            setDefaultValue(defaultValue);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    void renameClass(Map<String, String> classnames) {
        try {
            MemberValue defaultValue = getDefaultValue();
            defaultValue.renameClass(classnames);
            setDefaultValue(defaultValue);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Obtains the default value represented by this attribute.
     */
    public MemberValue getDefaultValue()
    {
       try {
           return new AnnotationsAttribute.Parser(info, constPool)
                                          .parseMemberValue();
       }
       catch (Exception e) {
           throw new RuntimeException(e.toString());
       }
    }

    /**
     * Changes the default value represented by this attribute.
     *
     * @param value         the new value.
     * @see javassist.bytecode.annotation.Annotation#createMemberValue(ConstPool, CtClass)
     */
    public void setDefaultValue(MemberValue value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AnnotationsWriter writer = new AnnotationsWriter(output, constPool);
        try {
            value.write(writer);
            writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);      // should never reach here.
        }

        set(output.toByteArray());
        
    }

    /**
     * Returns a string representation of this object.
     */
    @Override
    public String toString() {
        return getDefaultValue().toString();
    }
}
