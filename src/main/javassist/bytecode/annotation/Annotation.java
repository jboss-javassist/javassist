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

import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.CtClass;
import javassist.CtMethod;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

/**
 * The <code>annotation</code> structure.
 *
 * <p>An instance of this class is returned by
 * <code>getAnnotations()</code> in <code>AnnotationsAttribute</code>
 * or in <code>ParameterAnnotationsAttribute</code>.
 *
 * @see javassist.bytecode.AnnotationsAttribute#getAnnotations()
 * @see javassist.bytecode.ParameterAnnotationsAttribute#getAnnotations()
 * @see MemberValue
 * @see MemberValueVisitor
 * @see AnnotationsWriter
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class Annotation {
    static class Pair {
        int name;
        MemberValue value;
    }

    ConstPool pool;
    int typeIndex;
    HashMap members;    // this sould be LinkedHashMap
                        // but it is not supported by JDK 1.3.

    /**
     * Constructs an annotation including no members.  A member can be
     * later added to the created annotation by <code>addMemberValue()</code>. 
     *
     * @param type  the index into the constant pool table.
     *              the entry at that index must be the
     *              <code>CONSTANT_Utf8_Info</code> structure
     *              repreenting the name of the annotation interface type.
     * @param cp    the constant pool table.
     *
     * @see #addMemberValue(String, MemberValue)
     */
    public Annotation(int type, ConstPool cp) {
        pool = cp;
        typeIndex = type;
        members = null;
    }

    /**
     * Constructs an annotation including no members.  A member can be
     * later added to the created annotation by <code>addMemberValue()</code>. 
     *
     * @param typeName  the name of the annotation interface type.
     * @param cp        the constant pool table.
     *
     * @see #addMemberValue(String, MemberValue)
     */
    public Annotation(String typeName, ConstPool cp) {
        this(cp.addUtf8Info(Descriptor.of(typeName)), cp);
    }

    /**
     * Constructs an annotation that can be accessed through the interface
     * represented by <code>clazz</code>.  The values of the members are
     * not specified.
     *
     * @param cp        the constant pool table.
     * @param clazz     the interface.
     */
    public Annotation(ConstPool cp, CtClass clazz)
        throws javassist.NotFoundException
    {
        // todo Enums are not supported right now.
        this(cp.addUtf8Info(Descriptor.of(clazz.getName())), cp);

        if (!clazz.isInterface())
            throw new RuntimeException(
                "Only interfaces are allowed for Annotation creation.");

        CtMethod methods[] = clazz.getDeclaredMethods();
        if (methods.length > 0)
            members = new HashMap();

        for (int i = 0; i < methods.length; i++) {
            CtClass returnType = methods[i].getReturnType();
            addMemberValue(methods[i].getName(),
                           createMemberValue(cp, returnType));
        }
    }

    /**
     * Makes an instance of <code>MemberValue</code>.
     *
     * @param cp            the constant pool table.
     * @param type          the type of the member.
     */
    public static MemberValue createMemberValue(ConstPool cp, CtClass type)
        throws javassist.NotFoundException
    {
        if (type == CtClass.booleanType)
            return new BooleanMemberValue(cp);
        else if (type == CtClass.byteType)
            return new ByteMemberValue(cp);
        else if (type == CtClass.charType)
            return new CharMemberValue(cp);
        else if (type == CtClass.shortType)
            return new ShortMemberValue(cp);
        else if (type == CtClass.intType)
            return new IntegerMemberValue(cp);
        else if (type == CtClass.longType)
            return new LongMemberValue(cp);
        else if (type == CtClass.floatType)
            return new FloatMemberValue(cp);
        else if (type == CtClass.doubleType)
            return new DoubleMemberValue(cp);
        else if (type.getName().equals("java.lang.Class"))
            return new ClassMemberValue(cp);
        else if (type.getName().equals("java.lang.String"))
            return new StringMemberValue(cp);
        else if (type.isArray()) {
            CtClass arrayType = type.getComponentType();
            MemberValue member = createMemberValue(cp, arrayType);
            return new ArrayMemberValue(member, cp);
        }
        else if (type.isInterface()) {
            Annotation info = new Annotation(cp, type);
            return new AnnotationMemberValue(info, cp);
        }
        else {
            // treat as enum.  I know this is not typed,
            // but JBoss has an Annotation Compiler for JDK 1.4
            // and I want it to work with that. - Bill Burke
            EnumMemberValue emv = new EnumMemberValue(cp);
            emv.setType(type.getName());
            return emv;
        }
    }

    /**
     * Adds a new member.
     *
     * @param nameIndex     the index into the constant pool table.
     *                      The entry at that index must be
     *                      a <code>CONSTANT_Utf8_info</code> structure.
     *                      structure representing the member name.
     * @param value         the member value.
     */
    public void addMemberValue(int nameIndex, MemberValue value) {
        Pair p = new Pair();
        p.name = nameIndex;
        p.value = value;
        addMemberValue(p);
    }

    /**
     * Adds a new member.
     *
     * @param name      the member name.
     * @param value     the member value.
     */
    public void addMemberValue(String name, MemberValue value) {
        Pair p = new Pair();
        p.name = pool.addUtf8Info(name);
        p.value = value;
        if (members == null)
            members = new HashMap();

        members.put(name, p);
    }

    private void addMemberValue(Pair pair) {
        String name = pool.getUtf8Info(pair.name);
        if (members == null)
            members = new HashMap();

        members.put(name, pair);
    }

    /**
     * Returns a string representation of this object.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer("@");
        buf.append(getTypeName());
        if (members != null) {
            buf.append("(");
            Iterator mit = members.keySet().iterator();
            while (mit.hasNext()) {
                String name = (String)mit.next();
                buf.append(name).append("=").append(getMemberValue(name));
                if (mit.hasNext())
                    buf.append(", ");
            }
            buf.append(")");
        }

        return buf.toString();
    }

    /**
     * Obtains the name of the annotation type.
     */
    public String getTypeName() {
        return Descriptor.toClassName(pool.getUtf8Info(typeIndex));
    }

    /**
     * Obtains all the member names.
     *
     * @return null if no members are defined.
     */
    public Set getMemberNames() {
        if (members == null)
            return null;
        else
            return members.keySet();
    }

    /**
     * Obtains the member value with the given name.
     *
     * @return null if the member cannot be found.
     */
    public MemberValue getMemberValue(String name) {
        if (members == null)
            return null;
        else {
            Pair p = (Pair)members.get(name);
            if (p == null)
                return null;
            else
                return p.value;
        }
    }

    /**
     * Writes this annotation.
     *
     * @param writer            the output.
     */
    public void write(AnnotationsWriter writer) throws IOException {
        if (members == null) {
            writer.annotation(typeIndex, 0);
            return;
        }

        writer.annotation(typeIndex, members.size());
        Iterator it = members.values().iterator();
        while (it.hasNext()) {
            Pair pair = (Pair)it.next();
            writer.memberValuePair(pair.name);
            pair.value.write(writer);
        }
    }
}
