/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package javassist;

/**
 * An instance of <code>CtMember</code> represents a field, a constructor,
 * or a method.
 */
public abstract class CtMember {
    protected CtClass declaringClass;

    protected CtMember(CtClass clazz) { declaringClass = clazz; }

    /**
     * Returns the class that declares this member.
     */
    public CtClass getDeclaringClass() { return declaringClass; }

    /**
     * Obtains the modifiers of the member.
     *
     * @return          modifiers encoded with
     *                  <code>javassist.Modifier</code>.
     * @see Modifier
     */
    public abstract int getModifiers();

    /**
     * Sets the encoded modifiers of the member.
     *
     * @see Modifier
     */
    public abstract void setModifiers(int mod);

    /**
     * Obtains the name of the member.
     */
    public abstract String getName();

    /**
     * Obtains an attribute with the given name.
     * If that attribute is not found in the class file, this
     * method returns null.
     *
     * @param name              attribute name
     */
    public abstract byte[] getAttribute(String name);

    /**
     * Adds an attribute. The attribute is saved in the class file.
     *
     * @param name      attribute name
     * @param data      attribute value
     */
    public abstract void setAttribute(String name, byte[] data);
}
