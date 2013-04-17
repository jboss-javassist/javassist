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
     * Constructs a <code>Signature</code> attribute.
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
     * Returns the generic signature indicated by <code>signature_index</code>.
     *
     * @see #toClassSignature(String)
     * @see #toMethodSignature(String)
     * @see #toFieldSignature(String)
     */
    public String getSignature() {
        return getConstPool().getUtf8Info(ByteArray.readU16bit(get(), 0));
    }

    /**
     * Sets <code>signature_index</code> to the index of the given generic signature,
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

        /**
         * Constructs a class signature.
         *
         * @param params             type parameters.
         * @param superClass         the super class.
         * @param interfaces         the interface types.
         */
        public ClassSignature(TypeParameter[] params, ClassType superClass, ClassType[] interfaces) {
            this.params = params == null ? new TypeParameter[0] : params;
            this.superClass = superClass == null ? ClassType.OBJECT : superClass;
            this.interfaces = interfaces == null ? new ClassType[0] : interfaces;
        }

        /**
         * Constructs a class signature.
         *
         * @param p         type parameters.
         */
        public ClassSignature(TypeParameter[] p) {
            this(p, null, null);
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

        /**
         * Returns the encoded string representing the method type signature.
         */
        public String encode() {
            StringBuffer sbuf = new StringBuffer();
            if (params.length > 0) {
                sbuf.append('<');
                for (int i = 0; i < params.length; i++)
                    params[i].encode(sbuf);

                sbuf.append('>');
            }

            superClass.encode(sbuf);
            for (int i = 0; i < interfaces.length; i++)
                interfaces[i].encode(sbuf);

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

        /**
         * Constructs a method type signature.  Any parameter can be null
         * to represent <code>void</code> or nothing.
         *
         * @param tp        type parameters.
         * @param params    parameter types.
         * @param ret       a return type, or null if the return type is <code>void</code>.
         * @param ex        exception types.
         */
        public MethodSignature(TypeParameter[] tp, Type[] params, Type ret, ObjectType[] ex) {
            typeParams = tp == null ? new TypeParameter[0] : tp;
            this.params = params == null ? new Type[0] : params;
            retType = ret == null ? new BaseType("void") : ret;
            exceptions = ex == null ? new ObjectType[0] : ex;
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

        /**
         * Returns the encoded string representing the method type signature.
         */
        public String encode() {
            StringBuffer sbuf = new StringBuffer();
            if (typeParams.length > 0) {
                sbuf.append('<');
                for (int i = 0; i < typeParams.length; i++)
                    typeParams[i].encode(sbuf);

                sbuf.append('>');
            }

            sbuf.append('(');
            for (int i = 0; i < params.length; i++)
                params[i].encode(sbuf);

            sbuf.append(')');
            retType.encode(sbuf);
            if (exceptions.length > 0)
                for (int i = 0; i < exceptions.length; i++) {
                    sbuf.append('^');
                    exceptions[i].encode(sbuf);
                }

            return sbuf.toString();
        }
    }

    /**
     * Formal type parameters.
     *
     * @see TypeArgument
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
         * Constructs a <code>TypeParameter</code> representing a type parametre
         * like <code>&lt;T extends ... &gt;<code>.
         *
         * @param name      parameter name.
         * @param superClass    an upper bound class-type (or null).
         * @param superInterfaces   an upper bound interface-type (or null).
         */
        public TypeParameter(String name, ObjectType superClass, ObjectType[] superInterfaces) {
            this.name = name;
            this.superClass = superClass;
            if (superInterfaces == null)
                this.superInterfaces = new ObjectType[0];
            else
                this.superInterfaces = superInterfaces;
        }

        /**
         * Constructs a <code>TypeParameter</code> representing a type parameter
         * like <code>&lt;T&gt;<code>.
         *
         * @param name          parameter name.
         */
        public TypeParameter(String name) {
            this(name, null, null);
        }

        /**
         * Returns the name of the type parameter.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the class bound of this parameter.
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

        void encode(StringBuffer sb) {
            sb.append(name);
            if (superClass == null)
                sb.append(":Ljava/lang/Object;");
            else {
                sb.append(':');
                superClass.encode(sb);
            }

            for (int i = 0; i < superInterfaces.length; i++) {
                sb.append(':');
                superInterfaces[i].encode(sb);
            }
        }
    }

    /**
     * Type argument.
     *
     * @see TypeParameter
     */
    public static class TypeArgument {
        ObjectType arg;
        char wildcard;

        TypeArgument(ObjectType a, char w) {
            arg = a;
            wildcard = w;
        }

        /**
         * Constructs a <code>TypeArgument</code>.
         * A type argument is <code>&lt;String&gt;</code>, <code>&lt;int[]&gt;</code>,
         * or a type variable <code>&lt;T&gt;</code>, etc.
         *
         * @param t         a class type, an array type, or a type variable.  
         */
        public TypeArgument(ObjectType t) {
            this(t, ' ');
        }

        /**
         * Constructs a <code>TypeArgument</code> representing <code>&lt;?&gt;</code>.
         */
        public TypeArgument() {
            this(null, '*');
        }

        /**
         * A factory method constructing a <code>TypeArgument</code> with an upper bound.
         * It represents <code>&lt;? extends ... &gt;</code> 
         * 
         * @param t     an upper bound type.
         */
        public static TypeArgument subclassOf(ObjectType t) {
            return new TypeArgument(t, '+');
        }

        /**
         * A factory method constructing a <code>TypeArgument</code> with an lower bound.
         * It represents <code>&lt;? super ... &gt;</code>
         * 
         * @param t     an lower bbound type.
         */
        public static TypeArgument superOf(ObjectType t) {
            return new TypeArgument(t, '-');
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

        static void encode(StringBuffer sb, TypeArgument[] args) {
            sb.append('<');
            for (int i = 0; i < args.length; i++) {
                TypeArgument ta = args[i];
                if (ta.isWildcard())
                    sb.append(ta.wildcard);

                if (ta.getType() != null)
                    ta.getType().encode(sb);
            }

            sb.append('>');
        }
    }

    /**
     * Primitive types and object types.
     */
    public static abstract class Type {
        abstract void encode(StringBuffer sb);
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
         * Constructs a <code>BaseType</code>.
         *
         * @param typeName      <code>void</code>, <code>int</code>, ... 
         */
        public BaseType(String typeName) {
            this(Descriptor.of(typeName).charAt(0));
        }

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

        void encode(StringBuffer sb) {
            sb.append(descriptor);
        }
    }

    /**
     * Class types, array types, and type variables.
     * This class is also used for representing a field type.
     */
    public static abstract class ObjectType extends Type {
        /**
         * Returns the encoded string representing the object type signature.
         */
        public String encode() {
            StringBuffer sb = new StringBuffer();
            encode(sb);
            return sb.toString();
        }
    }

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
         * A class type representing <code>java.lang.Object</code>.
         */
        public static ClassType OBJECT = new ClassType("java.lang.Object", null); 

        /**
         * Constructs a <code>ClassType</code>.  It represents
         * the name of a non-nested class.
         *
         * @param className     a fully qualified class name.
         * @param args          type arguments or null.
         */
        public ClassType(String className, TypeArgument[] args) {
            name = className;
            arguments = args;
        }

        /**
         * Constructs a <code>ClassType</code>.  It represents
         * the name of a non-nested class.
         *
         * @param className     a fully qualified class name.
         */
        public ClassType(String className) {
            this(className, null);
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

        void encode(StringBuffer sb) {
            sb.append('L');
            encode2(sb);
            sb.append(';');
        }

        void encode2(StringBuffer sb) {
            ClassType parent = getDeclaringClass();
            if (parent != null) {
                parent.encode2(sb);
                sb.append('$');
            }

            sb.append(name.replace('.', '/'));
            if (arguments != null)
                TypeArgument.encode(sb, arguments);
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
         * Constructs a <code>NestedClassType</code>.
         *
         * @param parent        the class surrounding this class type.
         * @param className     a simple class name.  It does not include
         *                      a package name or a parent's class name.
         * @param args          type parameters or null.
         */
        public NestedClassType(ClassType parent, String className, TypeArgument[] args) {
            super(className, args);
            this.parent = parent;
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

        /**
         * Constructs an <code>ArrayType</code>.
         *
         * @param d         dimension.
         * @param comp      the component type.
         */
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

        void encode(StringBuffer sb) {
            for (int i = 0; i < dim; i++)
                sb.append('[');

            componentType.encode(sb);
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
         * Constructs a <code>TypeVariable</code>.
         *
         * @param name      the name of a type variable.
         */
        public TypeVariable(String name) {
            this.name = name;
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

        void encode(StringBuffer sb) {
            sb.append('T').append(name).append(';');
        }
    }

    /**
     * Parses the given signature string as a class signature.
     *
     * @param  sig                  the signature obtained from the <code>SignatureAttribute</code>
     *                              of a <code>ClassFile</code>.
     * @return  a tree-like data structure representing a class signature.  It provides
     *          convenient accessor methods.
     * @throws BadBytecode          thrown when a syntactical error is found.
     * @see #getSignature()
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
     * @param  sig                  the signature obtained from the <code>SignatureAttribute</code>
     *                              of a <code>MethodInfo</code>.
     * @return  @return  a tree-like data structure representing a method signature.  It provides
     *          convenient accessor methods.
     * @throws BadBytecode          thrown when a syntactical error is found.
     * @see #getSignature()
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
     * @param  sig                  the signature string obtained from the <code>SignatureAttribute</code>
     *                              of a <code>FieldInfo</code>.
     * @return the field type signature.
     * @throws BadBytecode          thrown when a syntactical error is found.
     * @see #getSignature()
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

    /**
     * Parses the given signature string as a type signature.
     * The type signature is either the field type signature or a base type
     * descriptor including <code>void</code> type. 
     *
     * @throws BadBytecode		thrown when a syntactical error is found.
     * @since 3.18
     */
    public static Type toTypeSignature(String sig) throws BadBytecode {
    	try {
    		return parseType(sig, new Cursor());
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
        if (t == '$' || t == '.') {
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
