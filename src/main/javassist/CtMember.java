/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba. All Rights Reserved.
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
    CtMember next;          // for internal use
    protected CtClass declaringClass;

    /* Make a circular link of CtMembers declared in the
     * same class so that they are garbage-collected together
     * at the same time.
     */
    static class Cache extends CtMember {
        protected void extendToString(StringBuffer buffer) {}
        public boolean hasAnnotation(Class clz) { return false; }
        public Object getAnnotation(Class clz)
            throws ClassNotFoundException { return null; }
        public Object[] getAnnotations()
            throws ClassNotFoundException { return null; }
        public byte[] getAttribute(String name) { return null; }
        public Object[] getAvailableAnnotations() { return null; }
        public int getModifiers() { return 0; }
        public String getName() { return null; }
        public String getSignature() { return null; }
        public void setAttribute(String name, byte[] data) {}
        public void setModifiers(int mod) {}

        private CtMember methodTail;
        private CtMember consTail;     // constructor tail
        private CtMember fieldTail;

        Cache(CtClassType decl) {
            super(decl);
            methodTail = this;
            consTail = this;
            fieldTail = this;
            fieldTail.next = this;
        }

        CtMember methodHead() { return this; }
        CtMember lastMethod() { return methodTail; }
        CtMember consHead() { return methodTail; }      // may include a static initializer
        CtMember lastCons() { return consTail; }
        CtMember fieldHead() { return consTail; }
        CtMember lastField() { return fieldTail; }

        void addMethod(CtMember method) {
            method.next = methodTail.next;
            methodTail.next = method;
            if (methodTail == consTail) {
                consTail = method;
                if (methodTail == fieldTail)
                    fieldTail = method;
            }

            methodTail = method;
        }

        /* Both constructors and a class initializer.
         */
        void addConstructor(CtMember cons) {
            cons.next = consTail.next;
            consTail.next = cons;
            if (consTail == fieldTail)
                fieldTail = cons;

            consTail = cons;
        }

        void addField(CtMember field) {
            field.next = this; // or fieldTail.next
            fieldTail.next = field;
            fieldTail = field;
        }

        static int count(CtMember head, CtMember tail) {
            int n = 0;
            while (head != tail) {
                n++;
                head = head.next;
            }

            return n;
        }

        void remove(CtMember mem) {
            CtMember m = this;
            CtMember node;
            while ((node = m.next) != this) {
                if (node == mem) {
                    m.next = node.next;
                    if (node == methodTail)
                        methodTail = m;

                    if (node == consTail)
                        consTail = m;

                    if (node == fieldTail)
                        fieldTail = m;

                    break;
                }
                else
                    m = m.next;
            }
        }
    }

    protected CtMember(CtClass clazz) {
        declaringClass = clazz;
        next = null;
    }

    final CtMember next() { return next; }

    /**
     * This method is invoked when setName() or replaceClassName()
     * in CtClass is called.
     *
     * @see CtMethod#nameReplaced()
     */
    void nameReplaced() {}

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
     * Returns true if this member is accessible from the given class.
     */
    public boolean visibleFrom(CtClass clazz) {
        int mod = getModifiers();
        if (Modifier.isPublic(mod))
            return true;
        else if (Modifier.isPrivate(mod))
            return clazz == declaringClass;
        else {  // package or protected
            String declName = declaringClass.getPackageName();
            String fromName = clazz.getPackageName();
            boolean visible;
            if (declName == null)
                visible = fromName == null;
            else
                visible = declName.equals(fromName);

            if (!visible && Modifier.isProtected(mod))
                return clazz.subclassOf(declaringClass);

            return visible;
        }
    }

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
     * Returns true if the class has the specified annotation class.
     *
     * @param clz the annotation class.
     * @return <code>true</code> if the annotation is found, otherwise <code>false</code>.
     * @since 3.11
     */
    public abstract boolean hasAnnotation(Class clz);

    /**
     * Returns the annotation if the class has the specified annotation class.
     * For example, if an annotation <code>@Author</code> is associated
     * with this member, an <code>Author</code> object is returned.
     * The member values can be obtained by calling methods on
     * the <code>Author</code> object.
     *
     * @param clz the annotation class.
     * @return the annotation if found, otherwise <code>null</code>.
     * @since 3.11
     */
    public abstract Object getAnnotation(Class clz) throws ClassNotFoundException;

    /**
     * Returns the annotations associated with this member.
     * For example, if an annotation <code>@Author</code> is associated
     * with this member, the returned array contains an <code>Author</code>
     * object.  The member values can be obtained by calling methods on
     * the <code>Author</code> object.
     *
     * @return an array of annotation-type objects.
     * @see CtClass#getAnnotations()
     */
    public abstract Object[] getAnnotations() throws ClassNotFoundException;

    /**
     * Returns the annotations associated with this member.
     * This method is equivalent to <code>getAnnotations()</code>
     * except that, if any annotations are not on the classpath,
     * they are not included in the returned array.
     *
     * @return an array of annotation-type objects.
     * @see #getAnnotations()
     * @see CtClass#getAvailableAnnotations()
     * @since 3.3
     */
    public abstract Object[] getAvailableAnnotations();

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
     * Returns the character string representing the signature of the member.
     * If two members have the same signature (parameter types etc.),
     * <code>getSignature()</code> returns the same string.
     */
    public abstract String getSignature();

    /**
     * Obtains a user-defined attribute with the given name.
     * If that attribute is not found in the class file, this
     * method returns null.
     *
     * <p>Note that an attribute is a data block specified by
     * the class file format.
     * See {@link javassist.bytecode.AttributeInfo}.
     *
     * @param name              attribute name
     */
    public abstract byte[] getAttribute(String name);

    /**
     * Adds a user-defined attribute. The attribute is saved in the class file.
     *
     * <p>Note that an attribute is a data block specified by
     * the class file format.
     * See {@link javassist.bytecode.AttributeInfo}.
     *
     * @param name      attribute name
     * @param data      attribute value
     */
    public abstract void setAttribute(String name, byte[] data);
}
