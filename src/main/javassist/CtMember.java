/*
 * This file is part of the Javassist toolkit.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * either http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is Javassist.
 *
 * The Initial Developer of the Original Code is Shigeru Chiba.  Portions
 * created by Shigeru Chiba are Copyright (C) 1999-2003 Shigeru Chiba.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * The development of this software is supported in part by the PRESTO
 * program (Sakigake Kenkyu 21) of Japan Science and Technology Corporation.
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
     * @return		modifiers encoded with
     *			<code>javassist.Modifier</code>.
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
     * @param name		attribute name
     */
    public abstract byte[] getAttribute(String name);

    /**
     * Adds an attribute. The attribute is saved in the class file.
     *
     * @param name	attribute name
     * @param data	attribute value
     */
    public abstract void setAttribute(String name, byte[] data);
}
