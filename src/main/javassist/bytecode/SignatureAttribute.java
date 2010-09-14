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

package javassist.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.ArrayList;
import javassist.CtClass;

/**
 * <code>Signature_attribute</code>.
 */
public class SignatureAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"Signature"</code>.
     */
    public static final String tag = "Signature";

    SignatureAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    /**
     * Constructs a Signature attribute.
     *
     * @param cp                a constant pool table.
     * @param signature         the signature represented by this attribute.
     */
    public SignatureAttribute(ConstPool cp, String signature) {
        super(cp, tag);
        int index = cp.addUtf8Info(signature);
        byte[] bvalue = new byte[2];
        bvalue[0] = (byte)(index >>> 8);
        bvalue[1] = (byte)index;
        set(bvalue);
    }

    /**
     * Returns the signature indicated by <code>signature_index</code>.
     *
     * @see #toClassSignature(String)
     * @see #toMethodSignature(String)
     */
    public String getSignature() {
        return getConstPool().getUtf8Info(ByteArray.readU16bit(get(), 0));
    }

    /**
     * Sets <code>signature_index</code> to the index of the given signature,
     * which is added to a constant pool.
     *
     * @param sig       new signature.
     * @since 3.11
     */
    public void setSignature(String sig) {
        int index = getConstPool().addUtf8Info(sig);
        ByteArray.write16bit(index, info, 0);
    }

    /**
     * Makes a copy.  Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        pairs of replaced and substituted
     *                          class names.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        return new SignatureAttribute(newCp, getSignature());
    }

    void renameClass(String oldname, String newname) {
        String sig = renameClass(getSignature(), oldname, newname);
        setSignature(sig);
    }

    void renameClass(Map classnames) {
        String sig = renameClass(getSignature(), classnames);
        setSignature(sig);
    }

    static String renameClass(String desc, String oldname, String newname) {
        Map map = new java.util.HashMap();
        map.put(oldname, newname);
        return renameClass(desc, map);
    }

    static String renameClass(String desc, Map map) {
        if (map == null)
            return desc;

        StringBuilder newdesc = new StringBuilder();
        int head = 0;
        int i = 0;
        for (;;) {
            int j = desc.indexOf('L', i);
            if (j < 0)
                break;

            StringBuilder nameBuf = new StringBuilder();
            int k = j;
            char c;
            try {
                while ((c = desc.charAt(++k)) != ';') {
                    nameBuf.append(c);
                    if (c == '<') {
                        while ((c = desc.charAt(++k)) != '>')
                            nameBuf.append(c);

                        nameBuf.append(c);
                    }
                }
            }
            catch (IndexOutOfBoundsException e) { break; }
            i = k + 1;
            String name = nameBuf.toString();
            String name2 = (String)map.get(name);
            if (name2 != null) {
                newdesc.append(desc.substring(head, j));
                newdesc.append('L');
                newdesc.append(name2);
                newdesc.append(c);
                head = i;
            }
        }

        if (head == 0)
            return desc;
        else {
            int len = desc.length();
            if (head < len)
                newdesc.append(desc.substring(head, len));

            return newdesc.toString();
        }
    }

    private static boolean isNamePart(int c) {
        return c != ';' && c != '<';
    }

    static private class Cursor {
        int position = 0;

        int indexOf(String s, int ch) throws BadBytecode {
            int i = s.indexOf(ch, position);
            if (i < 0)
                throw error(s);
            else {
                position = i + 1;
                return i;
            }
        }
    }

    /**
     * Class signature.
     */
    public static class ClassSignature {
        TypeParameter[] params;
        ClassType superClass;
        ClassType[] interfaces;
        ClassSignature(TypeParameter[] p, ClassType s, ClassType[] i) {
            params = p;
            superClass = s;
            interfaces = i;
        }

        /**
         * Returns the type parameters.
         *
         * @return a zero-length array if the type parameters are not specified.
         */
        public TypeParameter[] getParameters() {
            return params;
        }

        /**
         * Returns the super class.
         */
        public ClassType getSuperClass() { return superClass; }

        /**
         * Returns the super interfaces.
         *
         * @return a zero-length array if the super interfaces are not specified.
         */
        public ClassType[] getInterfaces() { return interfaces; }

        /**
         * Returns the string representation.
         */
        public String toString() {
            StringBuffer sbuf = new StringBuffer();

            TypeParameter.toString(sbuf, params);
            sbuf.append(" extends ").append(superClass);
            if (interfaces.length > 0) {
                sbuf.append(" implements ");
                Type.toString(sbuf, interfaces);
            }

            return sbuf.toString();
        }
    }

    /**
     * Method type signature.
     */
    public static class MethodSignature {
        TypeParameter[] typeParams;
        Type[] params;
        Type retType;
        ObjectType[] exceptions;

        MethodSignature(TypeParameter[] tp, Type[] p, Type ret, ObjectType[] ex) {
            typeParams = tp;
            params = p;
            retType = ret;
            exceptions = ex;
        }

        /**
         * Returns the formal type parameters.
         *
         * @return a zero-length array if the type parameters are not specified.
         */
        public TypeParameter[] getTypeParameters() { return typeParams; }

        /**
         * Returns the types of the formal parameters.
         *
         * @return a zero-length array if no formal parameter is taken.
         */
        public Type[] getParameterTypes() { return params; }

        /**
         * Returns the type of the returned value.
         */
        public Type getReturnType() { return retType; }

        /**
         * Returns the types of the exceptions that may be thrown.
         *
         * @return a zero-length array if exceptions are never thrown or
         * the exception types are not parameterized types or type variables.
         */
        public ObjectType[] getExceptionTypes() { return exceptions; }

        /**
         * Returns the string representation.
         */
        public String toString() {
            StringBuffer sbuf = new StringBuffer();

            TypeParameter.toString(sbuf, typeParams);
            sbuf.append(" (");
            Type.toString(sbuf, params);
            sbuf.append(") ");
            sbuf.append(retType);
            if (exceptions.length > 0) {
                sbuf.append(" throws ");
                Type.toString(sbuf, exceptions);
            }

            return sbuf.toString();
        }
    }

    /**
     * Formal type parameters.
     */
    public static class TypeParameter {
        String name;
        ObjectType superClass;
        ObjectType[] superInterfaces;

        TypeParameter(String sig, int nb, int ne, ObjectType sc, ObjectType[] si) {
            name = sig.substring(nb, ne);
            superClass = sc;
            superInterfaces = si;
        }

        /**
         * Returns the name of the type parameter.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the class bound of this parameter.
         *
         * @return null if the class bound is not specified.
         */
        public ObjectType getClassBound() { return superClass; }

        /**
         * Returns the interface bound of this parameter.
         *
         * @return a zero-length array if the interface bound is not specified.
         */
        public ObjectType[] getInterfaceBound() { return superInterfaces; }

        /**
         * Returns the string representation.
         */
        public String toString() {
            StringBuffer sbuf = new StringBuffer(getName());
            if (superClass != null)
                sbuf.append(" extends ").append(superClass.toString());

            int len = superInterfaces.length;
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    if (i > 0 || superClass != null)
                        sbuf.append(" & ");
                    else
                        sbuf.append(" extends ");

                    sbuf.append(superInterfaces[i].toString());
                }
            }

            return sbuf.toString();
        }

        static void toString(StringBuffer sbuf, TypeParameter[] tp) {
            sbuf.append('<');
            for (int i = 0; i < tp.length; i++) {
                if (i > 0)
                    sbuf.append(", ");

                sbuf.append(tp[i]);
            }

            sbuf.append('>');
        }
    }

    /**
     * Type argument.
     */
    public static class TypeArgument {
        ObjectType arg;
        char wildcard;

        TypeArgument(ObjectType a, char w) {
            arg = a;
            wildcard = w;
        }

        /**
         * Returns the kind of this type argument.
         *
         * @return <code>' '</code> (not-wildcard), <code>'*'</code> (wildcard), <code>'+'</code> (wildcard with
         * upper bound), or <code>'-'</code> (wildcard with lower bound). 
         */
        public char getKind() { return wildcard; }

        /**
         * Returns true if this type argument is a wildcard type
         * such as <code>?</code>, <code>? extends String</code>, or <code>? super Integer</code>.
         */
        public boolean isWildcard() { return wildcard != ' '; }

        /**
         * Returns the type represented by this argument
         * if the argument is not a wildcard type.  Otherwise, this method
         * returns the upper bound (if the kind is '+'),
         * the lower bound (if the kind is '-'), or null (if the upper or lower
         * bound is not specified). 
         */
        public ObjectType getType() { return arg; }

        /**
         * Returns the string representation.
         */
        public String toString() {
            if (wildcard == '*')
                return "?";

            String type = arg.toString();
            if (wildcard == ' ')
                return type;
            else if (wildcard == '+')
                return "? extends " + type;
            else
                return "? super " + type;
        }
    }

    /**
     * Primitive types and object types.
     */
    public static abstract class Type {
        static void toString(StringBuffer sbuf, Type[] ts) {
            for (int i = 0; i < ts.length; i++) {
                if (i > 0)
                    sbuf.append(", ");

                sbuf.append(ts[i]);
            }
        }
    }

    /**
     * Primitive types.
     */
    public static class BaseType extends Type {
        char descriptor;
        BaseType(char c) { descriptor = c; }

        /**
         * Returns the descriptor representing this primitive type.
         *
         * @see javassist.bytecode.Descriptor
         */
        public char getDescriptor() { return descriptor; }

        /**
         * Returns the <code>CtClass</code> representing this
         * primitive type. 
         */
        public CtClass getCtlass() {
            return Descriptor.toPrimitiveClass(descriptor);
        }

        /**
         * Returns the string representation.
         */
        public String toString() {
            return Descriptor.toClassName(Character.toString(descriptor));
        }
    }

    /**
     * Class types, array types, and type variables.
     */
    public static abstract class ObjectType extends Type {}

    /**
     * Class types.
     */
    public static class ClassType extends ObjectType {
        String name;
        TypeArgument[] arguments;

        static ClassType make(String s, int b, int e,
                              TypeArgument[] targs, ClassType parent) {
            if (parent == null)
                return new ClassType(s, b, e, targs);
            else
                return new NestedClassType(s, b, e, targs, parent);
        }

        ClassType(String signature, int begin, int end, TypeArgument[] targs) {
            name = signature.substring(begin, end).replace('/', '.');
            arguments = targs;
        }

        /**
         * Returns the class name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the type arguments.
         *
         * @return null if no type arguments are given to this class.
         */
        public TypeArgument[] getTypeArguments() { return arguments; }

        /**
         * If this class is a member of another class, returns the 
         * class in which this class is declared.
         *
         * @return null if this class is not a member of another class.
         */
        public ClassType getDeclaringClass() { return null; }

        /**
         * Returns the string representation.
         */
        public String toString() {
            StringBuffer sbuf = new StringBuffer();
            ClassType parent = getDeclaringClass();
            if (parent != null)
                sbuf.append(parent.toString()).append('.');

            sbuf.append(name);
            if (arguments != null) {
                sbuf.append('<');
                int n = arguments.length;
                for (int i = 0; i < n; i++) {
                    if (i > 0)
                        sbuf.append(", ");

                    sbuf.append(arguments[i].toString());
                }

                sbuf.append('>');
            }

            return sbuf.toString();
        }
    }

    /**
     * Nested class types.
     */
    public static class NestedClassType extends ClassType {
        ClassType parent;
        NestedClassType(String s, int b, int e,
                        TypeArgument[] targs, ClassType p) {
            super(s, b, e, targs);
            parent = p;
        }

        /**
         * Returns the class that declares this nested class.
         * This nested class is a member of that declaring class.
         */
        public ClassType getDeclaringClass() { return parent; }
    }

    /**
     * Array types.
     */
    public static class ArrayType extends ObjectType {
        int dim;
        Type componentType;

        public ArrayType(int d, Type comp) {
            dim = d;
            componentType = comp;
        }

        /**
         * Returns the dimension of the array. 
         */
        public int getDimension() { return dim; }

        /**
         * Returns the component type.
         */
        public Type getComponentType() {
            return componentType;
        }

        /**
         * Returns the string representation.
         */
        public String toString() {
            StringBuffer sbuf = new StringBuffer(componentType.toString());
            for (int i = 0; i < dim; i++)
                sbuf.append("[]");

            return sbuf.toString();
        }
    }

    /**
     * Type variables.
     */
    public static class TypeVariable extends ObjectType {
        String name;

        TypeVariable(String sig, int begin, int end) {
            name = sig.substring(begin, end);
        }

        /**
         * Returns the variable name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the string representation.
         */
        public String toString() {
            return name;
        }
    }

    /**
     * Parses the given signature string as a class signature.
     *
     * @param  sig                  the signature.
     * @throws BadBytecode          thrown when a syntactical error is found.
     * @since 3.5
     */
    public static ClassSignature toClassSignature(String sig) throws BadBytecode {
        try {
            return parseSig(sig);
        }
        catch (IndexOutOfBoundsException e) {
            throw error(sig);
        }
    }

    /**
     * Parses the given signature string as a method type signature.
     *
     * @param  sig                  the signature.
     * @throws BadBytecode          thrown when a syntactical error is found.
     * @since 3.5
     */
    public static MethodSignature toMethodSignature(String sig) throws BadBytecode {
        try {
            return parseMethodSig(sig);
        }
        catch (IndexOutOfBoundsException e) {
            throw error(sig);
        }
    }

    /**
     * Parses the given signature string as a field type signature.
     *
     * @param  sig                  the signature string.
     * @return the field type signature.
     * @throws BadBytecode          thrown when a syntactical error is found.
     * @since 3.5
     */
    public static ObjectType toFieldSignature(String sig) throws BadBytecode {
        try {
            return parseObjectType(sig, new Cursor(), false);
        }
        catch (IndexOutOfBoundsException e) {
            throw error(sig);
        }
    }

    private static ClassSignature parseSig(String sig)
        throws BadBytecode, IndexOutOfBoundsException
    {
        Cursor cur = new Cursor();
        TypeParameter[] tp = parseTypeParams(sig, cur);
        ClassType superClass = parseClassType(sig, cur);
        int sigLen = sig.length();
        ArrayList ifArray = new ArrayList();
        while (cur.position < sigLen && sig.charAt(cur.position) == 'L')
            ifArray.add(parseClassType(sig, cur));

        ClassType[] ifs
            = (ClassType[])ifArray.toArray(new ClassType[ifArray.size()]);
        return new ClassSignature(tp, superClass, ifs);
    }

    private static MethodSignature parseMethodSig(String sig)
        throws BadBytecode
    {
        Cursor cur = new Cursor();
        TypeParameter[] tp = parseTypeParams(sig, cur);
        if (sig.charAt(cur.position++) != '(')
            throw error(sig);

        ArrayList params = new ArrayList();
        while (sig.charAt(cur.position) != ')') {
            Type t = parseType(sig, cur);
            params.add(t);
        }

        cur.position++;
        Type ret = parseType(sig, cur);
        int sigLen = sig.length();
        ArrayList exceptions = new ArrayList();
        while (cur.position < sigLen && sig.charAt(cur.position) == '^') {
            cur.position++;
            ObjectType t = parseObjectType(sig, cur, false);
            if (t instanceof ArrayType)
                throw error(sig);

            exceptions.add(t);
        }

        Type[] p = (Type[])params.toArray(new Type[params.size()]);
        ObjectType[] ex = (ObjectType[])exceptions.toArray(new ObjectType[exceptions.size()]);
        return new MethodSignature(tp, p, ret, ex);
    }

    private static TypeParameter[] parseTypeParams(String sig, Cursor cur)
        throws BadBytecode
    {
        ArrayList typeParam = new ArrayList();
        if (sig.charAt(cur.position) == '<') {
            cur.position++;
            while (sig.charAt(cur.position) != '>') {
                int nameBegin = cur.position; 
                int nameEnd = cur.indexOf(sig, ':');
                ObjectType classBound = parseObjectType(sig, cur, true);
                ArrayList ifBound = new ArrayList();
                while (sig.charAt(cur.position) == ':') {
                    cur.position++;
                    ObjectType t = parseObjectType(sig, cur, false);
                    ifBound.add(t);
                }

                TypeParameter p = new TypeParameter(sig, nameBegin, nameEnd,
                        classBound, (ObjectType[])ifBound.toArray(new ObjectType[ifBound.size()]));
                typeParam.add(p);
            }

            cur.position++;
        }

        return (TypeParameter[])typeParam.toArray(new TypeParameter[typeParam.size()]);
    }

    private static ObjectType parseObjectType(String sig, Cursor c, boolean dontThrow)
        throws BadBytecode
    {
        int i;
        int begin = c.position;
        switch (sig.charAt(begin)) {
        case 'L' :
            return parseClassType2(sig, c, null);
        case 'T' :
            i = c.indexOf(sig, ';');
            return new TypeVariable(sig, begin + 1, i);
        case '[' :
            return parseArray(sig, c);
        default :
            if (dontThrow)
                return null;
            else
                throw error(sig);
        }
    }

    private static ClassType parseClassType(String sig, Cursor c)
        throws BadBytecode
    {
        if (sig.charAt(c.position) == 'L')
            return parseClassType2(sig, c, null);
        else
            throw error(sig);
    }

    private static ClassType parseClassType2(String sig, Cursor c, ClassType parent)
        throws BadBytecode
    {
        int start = ++c.position;
        char t;
        do {
            t = sig.charAt(c.position++);
        } while (t != '$' && t != '<' && t != ';');
        int end = c.position - 1;
        TypeArgument[] targs;
        if (t == '<') {
            targs = parseTypeArgs(sig, c);
            t = sig.charAt(c.position++);
        }
        else
            targs = null;

        ClassType thisClass = ClassType.make(sig, start, end, targs, parent);
        if (t == '$') {
            c.position--;
            return parseClassType2(sig, c, thisClass);
        }
        else
            return thisClass; 
    }

    private static TypeArgument[] parseTypeArgs(String sig, Cursor c) throws BadBytecode {
        ArrayList args = new ArrayList();
        char t;
        while ((t = sig.charAt(c.position++)) != '>') {
            TypeArgument ta;
            if (t == '*' )
                ta = new TypeArgument(null, '*');
            else {
                if (t != '+' && t != '-') {
                    t = ' ';
                    c.position--;
                }

                ta = new TypeArgument(parseObjectType(sig, c, false), t);
            }

            args.add(ta);
        }

        return (TypeArgument[])args.toArray(new TypeArgument[args.size()]);
    }

    private static ObjectType parseArray(String sig, Cursor c) throws BadBytecode {
        int dim = 1;
        while (sig.charAt(++c.position) == '[')
            dim++;

        return new ArrayType(dim, parseType(sig, c));
    }

    private static Type parseType(String sig, Cursor c) throws BadBytecode {
        Type t = parseObjectType(sig, c, true);
        if (t == null)
            t = new BaseType(sig.charAt(c.position++));

        return t;
    }

    private static BadBytecode error(String sig) {
        return new BadBytecode("bad signature: " + sig);
    }
}
