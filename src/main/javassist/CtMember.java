/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2005 Shigeru Chiba. All Rights Reserved.
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

package javassist;

/**
 * An instance of <code>CtMember</code> represents a field, a constructor,
 * or a method.
 */
public abstract class CtMember {
    protected CtMember next;          // for internal use
    protected CtClass declaringClass;

    protected CtMember(CtClass clazz) { declaringClass = clazz; }

    static CtMember append(CtMember list, CtMember tail) {
        tail.next = null;
        if (list == null)
            return tail;
        else {
            CtMember lst = list;
            while (lst.next != null)
                lst = lst.next;

            lst.next = tail;
            return list;
        }
    }

    static int count(CtMember f) {
        int n = 0;
        while (f != null) {
            ++n;
            f = f.next;
        }

        return n;
    }

    static CtMember remove(CtMember list, CtMember m) {
        CtMember top = list;
        if (list == null)
            return null;
        else if (list == m)
            return list.next;
        else
            while (list.next != null) {
                if (list.next == m) {
                    list.next = list.next.next;
                    break;
                }

                list = list.next;
            }

        return top;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer(getClass().getName());
        buffer.append("@");
        buffer.append(Integer.toHexString(hashCode()));
        buffer.append("[");
        buffer.append(Modifier.toString(getModifiers()));
        extendToString(buffer);
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * Invoked by {@link #toString()} to add to the buffer and provide the
     * complete value.  Subclasses should invoke this method, adding a
     * space before each token.  The modifiers for the member are
     * provided first; subclasses should provide additional data such
     * as return type, field or method name, etc.
     */
    protected abstract void extendToString(StringBuffer buffer);

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
     *
     * <p>As for constructor names, see <code>getName()</code>
     * in <code>CtConstructor</code>.
     *
     * @see CtConstructor#getName()
     */
    public abstract String getName();

    /**
     * Obtains a user-defined attribute with the given name.
     * If that attribute is not found in the class file, this
     * method returns null.
     *
     * @param name              attribute name
     */
    public abstract byte[] getAttribute(String name);

    /**
     * Adds a user-defined attribute. The attribute is saved in the class file.
     *
     * @param name      attribute name
     * @param data      attribute value
     */
    public abstract void setAttribute(String name, byte[] data);
}
