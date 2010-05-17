/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba, and others. All Rights Reserved.
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
package javassist.bytecode.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * Represents a JVM type in data-flow analysis. This abstraction is necessary since
 * a JVM type not only includes all normal Java types, but also a few special types
 * that are used by the JVM internally. See the static field types on this class for
 * more info on these special types.
 *
 * All primitive and special types reuse the same instance, so identity comparison can
 * be used when examining them. Normal java types must use {@link #equals(Object)} to
 * compare type instances.
 *
 * In most cases, applications which consume this API, only need to call {@link #getCtClass()}
 * to obtain the needed type information.
 *
 * @author Jason T. Greene
 */
public class Type {
    private final CtClass clazz;
    private final boolean special;

    private static final Map prims = new IdentityHashMap();
    /** Represents the double primitive type */
    public static final Type DOUBLE = new Type(CtClass.doubleType);
    /** Represents the boolean primitive type */
    public static final Type BOOLEAN = new Type(CtClass.booleanType);
    /** Represents the long primitive type */
    public static final Type LONG = new Type(CtClass.longType);
    /** Represents the char primitive type */
    public static final Type CHAR = new Type(CtClass.charType);
    /** Represents the byte primitive type */
    public static final Type BYTE = new Type(CtClass.byteType);
    /** Represents the short primitive type */
    public static final Type SHORT = new Type(CtClass.shortType);
    /** Represents the integer primitive type */
    public static final Type INTEGER = new Type(CtClass.intType);
    /** Represents the float primitive type */
    public static final Type FLOAT = new Type(CtClass.floatType);
    /** Represents the void primitive type */
    public static final Type VOID = new Type(CtClass.voidType);

    /**
     * Represents an unknown, or null type. This occurs when aconst_null is used.
     * It is important not to treat this type as java.lang.Object, since a null can
     * be assigned to any reference type. The analyzer will replace these with
     * an actual known type if it can be determined by a merged path with known type
     * information. If this type is encountered on a frame then it is guaranteed to
     * be null, and the type information is simply not available. Any attempts to
     * infer the type, without further information from the compiler would be a guess.
     */
    public static final Type UNINIT = new Type(null);

    /**
     * Represents an internal JVM return address, which is used by the RET
     * instruction to return to a JSR that invoked the subroutine.
     */
    public static final Type RETURN_ADDRESS = new Type(null, true);

    /** A placeholder used by the analyzer for the second word position of a double-word type */
    public static final Type TOP = new Type(null, true);

    /**
     * Represents a non-accessible value. Code cannot access the value this type
     * represents. It occurs when bytecode reuses a local variable table
     * position with non-mergable types. An example would be compiled code which
     * uses the same position for a primitive type in one branch, and a reference type
     * in another branch.
     */
    public static final Type BOGUS = new Type(null, true);

    /** Represents the java.lang.Object reference type */
    public static final Type OBJECT = lookupType("java.lang.Object");
    /** Represents the java.io.Serializable reference type */
    public static final Type SERIALIZABLE = lookupType("java.io.Serializable");
    /** Represents the java.lang.Coneable reference type */
    public static final Type CLONEABLE = lookupType("java.lang.Cloneable");
    /** Represents the java.lang.Throwable reference type */
    public static final Type THROWABLE = lookupType("java.lang.Throwable");

    static {
        prims.put(CtClass.doubleType, DOUBLE);
        prims.put(CtClass.longType, LONG);
        prims.put(CtClass.charType, CHAR);
        prims.put(CtClass.shortType, SHORT);
        prims.put(CtClass.intType, INTEGER);
        prims.put(CtClass.floatType, FLOAT);
        prims.put(CtClass.byteType, BYTE);
        prims.put(CtClass.booleanType, BOOLEAN);
        prims.put(CtClass.voidType, VOID);

    }

    /**
     * Obtain the Type for a given class. If the class is a primitive,
     * the the unique type instance for the primitive will be returned.
     * Otherwise a new Type instance representing the class is returned.
     *
     * @param clazz The java class
     * @return a type instance for this class
     */
    public static Type get(CtClass clazz) {
        Type type = (Type)prims.get(clazz);
        return type != null ? type : new Type(clazz);
    }

    private static Type lookupType(String name) {
        try {
             return new Type(ClassPool.getDefault().get(name));
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    Type(CtClass clazz) {
        this(clazz, false);
    }

    private Type(CtClass clazz, boolean special) {
        this.clazz = clazz;
        this.special = special;
    }

    // Used to indicate a merge internally triggered a change
    boolean popChanged() {
        return false;
    }

    /**
     * Gets the word size of this type. Double-word types, such as long and double
     * will occupy two positions on the local variable table or stack.
     *
     * @return the number of words needed to hold this type
     */
    public int getSize() {
        return clazz == CtClass.doubleType || clazz == CtClass.longType || this == TOP ? 2 : 1;
    }

    /**
     * Returns the class this type represents. If the type is special, null will be returned.
     *
     * @return the class for this type, or null if special
     */
    public CtClass getCtClass() {
        return clazz;
    }

    /**
     * Returns whether or not this type is a normal java reference, i.e. it is or extends java.lang.Object.
     *
     * @return true if a java reference, false if a primitive or special
     */
    public boolean isReference() {
        return !special && (clazz == null || !clazz.isPrimitive());
    }

    /**
     * Returns whether or not the type is special. A special type is one that is either used
     * for internal tracking, or is only used internally by the JVM.
     *
     * @return true if special, false if not
     */
    public boolean isSpecial() {
        return special;
    }

    /**
     * Returns whether or not this type is an array.
     *
     * @return true if an array, false if not
     */
    public boolean isArray() {
        return clazz != null && clazz.isArray();
    }

    /**
     * Returns the number of dimensions of this array. If the type is not an
     * array zero is returned.
     *
     * @return zero if not an array, otherwise the number of array dimensions.
     */
    public int getDimensions() {
        if (!isArray()) return 0;

        String name = clazz.getName();
        int pos = name.length() - 1;
        int count = 0;
        while (name.charAt(pos) == ']' ) {
            pos -= 2;
            count++;
        }

        return count;
    }

    /**
     * Returns the array component if this type is an array. If the type
     * is not an array null is returned.
     *
     * @return the array component if an array, otherwise null
     */
    public Type getComponent() {
        if (this.clazz == null || !this.clazz.isArray())
            return null;

        CtClass component;
        try {
            component = this.clazz.getComponentType();
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }

        Type type = (Type)prims.get(component);
        return (type != null) ? type : new Type(component);
    }

    /**
     * Determines whether this type is assignable, to the passed type.
     * A type is assignable to another if it is either the same type, or
     * a sub-type.
     *
     * @param type the type to test assignability to
     * @return true if this is assignable to type, otherwise false
     */
    public boolean isAssignableFrom(Type type) {
        if (this == type)
            return true;

        if ((type == UNINIT && isReference()) || this == UNINIT && type.isReference())
            return true;

        if (type instanceof MultiType)
            return ((MultiType)type).isAssignableTo(this);

        if (type instanceof MultiArrayType)
            return ((MultiArrayType)type).isAssignableTo(this);


        // Primitives and Special types must be identical
        if (clazz == null || clazz.isPrimitive())
            return false;

        try {
            return type.clazz.subtypeOf(clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds the common base type, or interface which both this and the specified
     * type can be assigned. If there is more than one possible answer, then a {@link MultiType},
     * or a {@link MultiArrayType} is returned. Multi-types have special rules,
     * and successive merges and assignment tests on them will alter their internal state,
     * as well as other multi-types they have been merged with. This method is used by
     * the data-flow analyzer to merge the type state from multiple branches.
     *
     * @param type the type to merge with
     * @return the merged type
     */
    public Type merge(Type type) {
        if (type == this)
            return this;
        if (type == null)
            return this;
        if (type == Type.UNINIT)
            return this;
        if (this == Type.UNINIT)
            return type;

        // Unequal primitives and special types can not be merged
        if (! type.isReference() || ! this.isReference())
            return BOGUS;

        // Centralize merging of multi-interface types
        if (type instanceof MultiType)
            return type.merge(this);

        if (type.isArray() && this.isArray())
            return mergeArray(type);

        try {
            return mergeClasses(type);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

   Type getRootComponent(Type type) {
        while (type.isArray())
            type = type.getComponent();

        return type;
    }

    private Type createArray(Type rootComponent, int dims) {
        if (rootComponent instanceof MultiType)
            return new MultiArrayType((MultiType) rootComponent, dims);

        String name = arrayName(rootComponent.clazz.getName(), dims);

        Type type;
        try {
            type = Type.get(getClassPool(rootComponent).get(name));
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }

        return type;
    }

    String arrayName(String component, int dims) {
     // Using char[] since we have no StringBuilder in JDK4, and StringBuffer is slow.
        // Although, this is more efficient even if we did have one.
        int i = component.length();
        int size = i + dims * 2;
        char[] string = new char[size];
        component.getChars(0, i, string, 0);
        while (i < size) {
            string[i++] = '[';
            string[i++] = ']';
        }
        component = new String(string);
        return component;
    }

    private ClassPool getClassPool(Type rootComponent) {
        ClassPool pool = rootComponent.clazz.getClassPool();
        return pool != null ? pool : ClassPool.getDefault();
    }

    private Type mergeArray(Type type) {
        Type typeRoot = getRootComponent(type);
        Type thisRoot = getRootComponent(this);
        int typeDims = type.getDimensions();
        int thisDims = this.getDimensions();

        // Array commponents can be merged when the dimensions are equal
        if (typeDims == thisDims) {
            Type mergedComponent = thisRoot.merge(typeRoot);

            // If the components can not be merged (a primitive component mixed with a different type)
            // then Object is the common type.
            if (mergedComponent == Type.BOGUS)
                return Type.OBJECT;

            return createArray(mergedComponent, thisDims);
        }

        Type targetRoot;
        int targetDims;

        if (typeDims < thisDims) {
            targetRoot = typeRoot;
            targetDims = typeDims;
        } else {
            targetRoot = thisRoot;
            targetDims = thisDims;
        }

        // Special case, arrays are cloneable and serializable, so prefer them when dimensions differ
        if (eq(CLONEABLE.clazz, targetRoot.clazz) || eq(SERIALIZABLE.clazz, targetRoot.clazz))
            return createArray(targetRoot, targetDims);

        return createArray(OBJECT, targetDims);
    }

    private static CtClass findCommonSuperClass(CtClass one, CtClass two) throws NotFoundException {
        CtClass deep = one;
        CtClass shallow = two;
        CtClass backupShallow = shallow;
        CtClass backupDeep = deep;

        // Phase 1 - Find the deepest hierarchy, set deep and shallow correctly
        for (;;) {
            // In case we get lucky, and find a match early
            if (eq(deep, shallow) && deep.getSuperclass() != null)
                return deep;

            CtClass deepSuper = deep.getSuperclass();
            CtClass shallowSuper = shallow.getSuperclass();

            if (shallowSuper == null) {
                // right, now reset shallow
                shallow = backupShallow;
                break;
            }

            if (deepSuper == null) {
                // wrong, swap them, since deep is now useless, its our tmp before we swap it
                deep = backupDeep;
                backupDeep = backupShallow;
                backupShallow = deep;

                deep = shallow;
                shallow = backupShallow;
                break;
            }

            deep = deepSuper;
            shallow = shallowSuper;
        }

        // Phase 2 - Move deepBackup up by (deep end - deep)
        for (;;) {
            deep = deep.getSuperclass();
            if (deep == null)
                break;

            backupDeep = backupDeep.getSuperclass();
        }

        deep = backupDeep;

        // Phase 3 - The hierarchy positions are now aligned
        // The common super class is easy to find now
        while (!eq(deep, shallow)) {
            deep = deep.getSuperclass();
            shallow = shallow.getSuperclass();
        }

        return deep;
    }

    private Type mergeClasses(Type type) throws NotFoundException {
        CtClass superClass = findCommonSuperClass(this.clazz, type.clazz);

        // If its Object, then try and find a common interface(s)
        if (superClass.getSuperclass() == null) {
            Map interfaces = findCommonInterfaces(type);
            if (interfaces.size() == 1)
                return new Type((CtClass) interfaces.values().iterator().next());
            if (interfaces.size() > 1)
                return new MultiType(interfaces);

            // Only Object is in common
            return new Type(superClass);
        }

        // Check for a common interface that is not on the found supertype
        Map commonDeclared = findExclusiveDeclaredInterfaces(type, superClass);
        if (commonDeclared.size() > 0) {
            return new MultiType(commonDeclared, new Type(superClass));
        }

        return new Type(superClass);
    }

    private Map findCommonInterfaces(Type type) {
        Map typeMap = getAllInterfaces(type.clazz, null);
        Map thisMap = getAllInterfaces(this.clazz, null);

        return findCommonInterfaces(typeMap, thisMap);
    }

    private Map findExclusiveDeclaredInterfaces(Type type, CtClass exclude) {
        Map typeMap = getDeclaredInterfaces(type.clazz, null);
        Map thisMap = getDeclaredInterfaces(this.clazz, null);
        Map excludeMap = getAllInterfaces(exclude, null);

        Iterator i = excludeMap.keySet().iterator();
        while (i.hasNext()) {
            Object intf = i.next();
            typeMap.remove(intf);
            thisMap.remove(intf);
        }

        return findCommonInterfaces(typeMap, thisMap);
    }


    Map findCommonInterfaces(Map typeMap, Map alterMap) {
        Iterator i = alterMap.keySet().iterator();
        while (i.hasNext()) {
            if (! typeMap.containsKey(i.next()))
                i.remove();
        }

        // Reduce to subinterfaces
        // This does not need to be recursive since we make a copy,
        // and that copy contains all super types for the whole hierarchy
        i = new ArrayList(alterMap.values()).iterator();
        while (i.hasNext()) {
            CtClass intf = (CtClass) i.next();
            CtClass[] interfaces;
            try {
                interfaces = intf.getInterfaces();
            } catch (NotFoundException e) {
                throw new RuntimeException(e);
            }

            for (int c = 0; c < interfaces.length; c++)
                alterMap.remove(interfaces[c].getName());
        }

        return alterMap;
    }

    Map getAllInterfaces(CtClass clazz, Map map) {
        if (map == null)
            map = new HashMap();

        if (clazz.isInterface())
            map.put(clazz.getName(), clazz);
        do {
            try {
                CtClass[] interfaces = clazz.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    CtClass intf = interfaces[i];
                    map.put(intf.getName(), intf);
                    getAllInterfaces(intf, map);
                }

                clazz = clazz.getSuperclass();
            } catch (NotFoundException e) {
                throw new RuntimeException(e);
            }
        } while (clazz != null);

        return map;
    }

    Map getDeclaredInterfaces(CtClass clazz, Map map) {
        if (map == null)
            map = new HashMap();

        if (clazz.isInterface())
            map.put(clazz.getName(), clazz);

        CtClass[] interfaces;
        try {
            interfaces = clazz.getInterfaces();
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < interfaces.length; i++) {
            CtClass intf = interfaces[i];
            map.put(intf.getName(), intf);
            getDeclaredInterfaces(intf, map);
        }

        return map;
    }

    public boolean equals(Object o) {
        if (! (o instanceof Type))
            return false;

        return o.getClass() == getClass() && eq(clazz, ((Type)o).clazz);
    }

    static boolean eq(CtClass one, CtClass two) {
        return one == two || (one != null && two != null && one.getName().equals(two.getName()));
    }

    public String toString() {
        if (this == BOGUS)
            return "BOGUS";
        if (this == UNINIT)
            return "UNINIT";
        if (this == RETURN_ADDRESS)
            return "RETURN ADDRESS";
        if (this == TOP)
            return "TOP";

        return clazz == null ? "null" : clazz.getName();
    }
}
