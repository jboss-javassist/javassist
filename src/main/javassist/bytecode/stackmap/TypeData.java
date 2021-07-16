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

package javassist.bytecode.stackmap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.StackMapTable;

public abstract class TypeData {
    /* Memo:
     * array type is a subtype of Cloneable and Serializable 
     */

    public static TypeData[] make(int size) {
        TypeData[] array = new TypeData[size];
        for (int i = 0; i < size; i++)
            array[i] = TypeTag.TOP;

        return array;
    }

    protected TypeData() {}

    /**
     * Sets the type name of this object type.  If the given type name is
     * a subclass of the current type name, then the given name becomes
     * the name of this object type.
     *
     * @param className     dot-separated name unless the type is an array type. 
     */
    @SuppressWarnings("unused")
    private static void setType(TypeData td, String className, ClassPool cp) throws BadBytecode {
        td.setType(className, cp);
    }

    public abstract int getTypeTag();
    public abstract int getTypeData(ConstPool cp);

    public TypeData join() { return new TypeVar(this); }

    /**
     * If the type is a basic type, this method normalizes the type
     * and returns a BasicType object.  Otherwise, it returns null.
     */
    public abstract BasicType isBasicType();

    public abstract boolean is2WordType();

    /**
     * Returns false if getName() returns a valid type name.
     */
    public boolean isNullType() { return false; }

    public boolean isUninit() { return false; }

    public abstract boolean eq(TypeData d);

    public abstract String getName();
    public abstract void setType(String s, ClassPool cp) throws BadBytecode;

    /**
     * @param dim		array dimension.  It may be negative.
     */
    public abstract TypeData getArrayType(int dim) throws NotFoundException;

    /**
     * Depth-first search by Tarjan's algorithm
     *
     * @param order			a node stack in the order in which nodes are visited.
     * @param index			the index used by the algorithm.
     */
    public int dfs(List<TypeData> order, int index, ClassPool cp)
        throws NotFoundException
    {
        return index;
    }

    /**
     * Returns this if it is a TypeVar or a TypeVar that this
     * type depends on.  Otherwise, this method returns null.
     * It is used by dfs().
     *
     * @param dim		dimension
     */
    protected TypeVar toTypeVar(int dim) { return null; }

    // see UninitTypeVar and UninitData
    public void constructorCalled(int offset) {}

    @Override
    public String toString() {
        return super.toString() + "(" + toString2(new HashSet<TypeData>()) + ")";
    }

    abstract String toString2(Set<TypeData> set);

    /**
     * Primitive types.
     */
    protected static class BasicType extends TypeData {
        private String name;
        private int typeTag;
        private char decodedName;

        public BasicType(String type, int tag, char decoded) {
            name = type;
            typeTag = tag;
            decodedName = decoded;
        }

        @Override
        public int getTypeTag() { return typeTag; }
        @Override
        public int getTypeData(ConstPool cp) { return 0; }

        @Override
        public TypeData join() {
            if (this == TypeTag.TOP)
                return this;
            return super.join();
        }

        @Override
        public BasicType isBasicType() { return this; }

        @Override
        public boolean is2WordType() {
            return typeTag == StackMapTable.LONG
                    || typeTag == StackMapTable.DOUBLE;
        }

        @Override
        public boolean eq(TypeData d) { return this == d; }

        @Override
        public String getName() {
            return name;
        }

        public char getDecodedName() { return decodedName; }

        @Override
        public void setType(String s, ClassPool cp) throws BadBytecode {
            throw new BadBytecode("conflict: " + name + " and " + s);
        }

        /**
         * @param dim		array dimension.  It may be negative.
         */
        @Override
        public TypeData getArrayType(int dim) throws NotFoundException {
            if (this == TypeTag.TOP)
                return this;
            else if (dim < 0)
                throw new NotFoundException("no element type: " + name);
            else if (dim == 0)
                return this;
            else {
                char[] name = new char[dim + 1];
                for (int i = 0; i < dim; i++)
                    name[i] = '[';

                name[dim] = decodedName;
                return new ClassName(new String(name));
            }
        }

        @Override
        String toString2(Set<TypeData> set) { return name; }
    }

    // a type variable
    public static abstract class AbsTypeVar extends TypeData {
        public AbsTypeVar() {}
        public abstract void merge(TypeData t);
        @Override
        public int getTypeTag() { return StackMapTable.OBJECT; }

        @Override
        public int getTypeData(ConstPool cp) {
            return cp.addClassInfo(getName());
        }

        @Override
        public boolean eq(TypeData d) {
            if (d.isUninit())
                return d.eq(this);
            else
                return getName().equals(d.getName());
        }
    }

    /* a type variable representing a class type or a basic type.
     */
    public static class TypeVar extends AbsTypeVar {
        protected List<TypeData> lowers;// lower bounds of this type. ArrayList<TypeData>
        protected List<TypeData> usedBy;// reverse relations of lowers
        protected List<String> uppers;  // upper bounds of this type.
        protected String fixedType;
        private boolean is2WordType;    // cache

        public TypeVar(TypeData t) {
            uppers = null;
            lowers = new ArrayList<TypeData>(2);
            usedBy = new ArrayList<TypeData>(2);
            merge(t);
            fixedType = null;
            is2WordType = t.is2WordType();
        }

        @Override
        public String getName() {
            if (fixedType == null)
                return lowers.get(0).getName();
            return fixedType;
        }

        @Override
        public BasicType isBasicType() {
            if (fixedType == null)
                return lowers.get(0).isBasicType();
            return null;
        }

        @Override
        public boolean is2WordType() {
            if (fixedType == null) {
                return is2WordType;
                // return ((TypeData)lowers.get(0)).is2WordType();
            }
            return false;
        }

        @Override
        public boolean isNullType() {
            if (fixedType == null)
                return lowers.get(0).isNullType();
            return false;
        }

        @Override
        public boolean isUninit() {
            if (fixedType == null)
                return lowers.get(0).isUninit();
            return false;
        }

        @Override
        public void merge(TypeData t) {
            lowers.add(t);
            if (t instanceof TypeVar)
                ((TypeVar)t).usedBy.add(this);
        }

        @Override
        public int getTypeTag() {
            /* If fixedType is null after calling dfs(), then this
               type is NULL, Uninit, or a basic type.  So call
               getTypeTag() on the first element of lowers. */
            if (fixedType == null)
                return lowers.get(0).getTypeTag();
            return super.getTypeTag();
        }

        @Override
        public int getTypeData(ConstPool cp) {
            if (fixedType == null)
                return lowers.get(0).getTypeData(cp);
            return super.getTypeData(cp);
        }

        @Override
        public void setType(String typeName, ClassPool cp) throws BadBytecode {
            if (uppers == null)
                uppers = new ArrayList<String>();

            uppers.add(typeName);
        }

        private int visited = 0;
        private int smallest = 0;
        private boolean inList = false;
        private int dimension = 0;

        @Override
        protected TypeVar toTypeVar(int dim) {
            dimension = dim;
            return this;
        }

        /* When fixTypes() is called, getName() will return the correct
         * (i.e. fixed) type name.
         */
        @Override
        public TypeData getArrayType(int dim) throws NotFoundException {
            if (dim == 0)
                return this;
            BasicType bt = isBasicType();
            if (bt == null)
                if (isNullType())
                    return new NullType();
                else
                    return new ClassName(getName()).getArrayType(dim);
            return bt.getArrayType(dim);
        }

        // depth-first serach
        @Override
        public int dfs(List<TypeData> preOrder, int index, ClassPool cp) throws NotFoundException {
            if (visited > 0)
                return index;		// MapMaker.make() may call an already visited node.

            visited = smallest = ++index;
            preOrder.add(this);
            inList = true;
            int n = lowers.size();
            for (int i = 0; i < n; i++) {
                TypeVar child = lowers.get(i).toTypeVar(dimension);
                if (child != null)
                    if (child.visited == 0) {
                        index = child.dfs(preOrder, index, cp);
                        if (child.smallest < smallest)
                            smallest = child.smallest;
                    }
                    else if (child.inList)
                        if (child.visited < smallest)
                            smallest = child.visited;
            }

            if (visited == smallest) {
                List<TypeData> scc = new ArrayList<TypeData>();    // strongly connected component
                TypeVar cv;
                do {
                    cv = (TypeVar)preOrder.remove(preOrder.size() - 1);
                    cv.inList = false;
                    scc.add(cv);
                } while (cv != this);
                fixTypes(scc, cp);
            }

            return index;
        }

        private void fixTypes(List<TypeData> scc, ClassPool cp) throws NotFoundException {
            Set<String> lowersSet = new HashSet<String>();
            boolean isBasicType = false;
            TypeData kind = null;
            int size = scc.size();
            for (int i = 0; i < size; i++) {
                TypeVar tvar = (TypeVar)scc.get(i);
                List<TypeData> tds = tvar.lowers;
                int size2 = tds.size();
                for (int j = 0; j < size2; j++) {
                    TypeData td = tds.get(j);
                    TypeData d = td.getArrayType(tvar.dimension);
                    BasicType bt = d.isBasicType();
                    if (kind == null) {
                        if (bt == null) {
                            isBasicType = false;
                            kind = d;
                            /* If scc has only an UninitData, fixedType is kept null.
                               So lowerSet must be empty.  If scc has not only an UninitData
                               but also another TypeData, an error must be thrown but this
                               error detection has not been implemented. */
                            if (d.isUninit())
                                break;
                        }
                        else {
                            isBasicType = true;
                            kind = bt;
                        }
                    }
                    else {
                        if ((bt == null && isBasicType) || (bt != null && kind != bt)) {
                            isBasicType = true;
                            kind = TypeTag.TOP;
                            break;
                         }
                    }

                    if (bt == null && !d.isNullType())
                        lowersSet.add(d.getName());
                }
            }

            if (isBasicType) {
                is2WordType = kind.is2WordType();	// necessary?
                fixTypes1(scc, kind);
            }
            else {
                String typeName = fixTypes2(scc, lowersSet, cp);
                fixTypes1(scc, new ClassName(typeName));
            }
        }

        private void fixTypes1(List<TypeData> scc, TypeData kind) throws NotFoundException {
            int size = scc.size();
            for (int i = 0; i < size; i++) {
                TypeVar cv = (TypeVar)scc.get(i);
                TypeData kind2 = kind.getArrayType(-cv.dimension);
                if (kind2.isBasicType() == null)
                    cv.fixedType = kind2.getName();
                else {
                    cv.lowers.clear();
                    cv.lowers.add(kind2);
                    cv.is2WordType = kind2.is2WordType();
                }
            }
        }

        private String fixTypes2(List<TypeData> scc, Set<String> lowersSet, ClassPool cp) throws NotFoundException {
            Iterator<String> it = lowersSet.iterator();
            if (lowersSet.isEmpty())
                return null;      // only NullType
            else if (lowersSet.size() == 1)
                return it.next();
            else {
                CtClass cc = cp.get(it.next());
                while (it.hasNext())
                    cc = commonSuperClassEx(cc, cp.get(it.next()));

                if (cc.getSuperclass() == null || isObjectArray(cc))
                    cc = fixByUppers(scc, cp, new HashSet<TypeData>(), cc);

                if (cc.isArray())
                    return Descriptor.toJvmName(cc);

                return cc.getName();
            }
        }

        private static boolean isObjectArray(CtClass cc) throws NotFoundException {
            return cc.isArray() && cc.getComponentType().getSuperclass() == null;
        }

        private CtClass fixByUppers(List<TypeData> users, ClassPool cp, Set<TypeData> visited, CtClass type)
            throws NotFoundException
        {
            if (users == null)
                return type;

            int size = users.size();
            for (int i = 0; i < size; i++) {
                TypeVar t = (TypeVar)users.get(i);
                if (!visited.add(t))
                    return type;

                if (t.uppers != null) {
                    int s = t.uppers.size();
                    for (int k = 0; k < s; k++) {
                        CtClass cc = cp.get(t.uppers.get(k));
                        if (cc.subtypeOf(type))
                            type = cc;
                    }
                }

                type = fixByUppers(t.usedBy, cp, visited, type);
            }

            return type;
        }

        @Override
        String toString2(Set<TypeData> hash) {
            hash.add(this);
            if (lowers.size() > 0) {
                TypeData e = lowers.get(0);
                if (e != null && !hash.contains(e))
                    return e.toString2(hash);
            }

            return "?";
        }
    }

    /**
     * Finds the most specific common super class of the given classes
     * by considering array types.
     */
    public static CtClass commonSuperClassEx(CtClass one, CtClass two) throws NotFoundException {
        if (one == two)
            return one;
        else if (one.isArray() && two.isArray()) {
            CtClass ele1 = one.getComponentType();
            CtClass ele2 = two.getComponentType();
            CtClass element = commonSuperClassEx(ele1, ele2);
            if (element == ele1)
                return one;
            else if (element == ele2)
                return two;
            else
                return one.getClassPool().get(element == null ? "java.lang.Object"
                                                : element.getName() + "[]");
        }
        else if (one.isPrimitive() || two.isPrimitive())
            return null;    // TOP
        else if (one.isArray() || two.isArray())    // but !(one.isArray() && two.isArray())
            return one.getClassPool().get("java.lang.Object");
        else
            return commonSuperClass(one, two);
    }

    /**
     * Finds the most specific common super class of the given classes.
     * This method is a copy from javassist.bytecode.analysis.Type.
     */
    public static CtClass commonSuperClass(CtClass one, CtClass two) throws NotFoundException {
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

    static boolean eq(CtClass one, CtClass two) {
        return one == two || (one != null && two != null && one.getName().equals(two.getName()));
    }

    public static void aastore(TypeData array, TypeData value, ClassPool cp) throws BadBytecode {
        if (array instanceof AbsTypeVar)
            if (!value.isNullType())
                ((AbsTypeVar)array).merge(ArrayType.make(value));

        if (value instanceof AbsTypeVar)
            if (array instanceof AbsTypeVar)
                ArrayElement.make(array);   // should call value.setType() later.
            else if (array instanceof ClassName) {
                if (!array.isNullType()) {
                    String type = ArrayElement.typeName(array.getName());
                    value.setType(type, cp);
                }
            }
            else
                throw new BadBytecode("bad AASTORE: " + array);
    }

    /* A type variable representing an array type.
     * It is a decorator of another type variable.  
     */
    public static class ArrayType extends AbsTypeVar {
        private AbsTypeVar element;

        private ArrayType(AbsTypeVar elementType) {
            element = elementType;
        }

        static TypeData make(TypeData element) throws BadBytecode {
            if (element instanceof ArrayElement)
                return ((ArrayElement)element).arrayType();
            else if (element instanceof AbsTypeVar)
                return new ArrayType((AbsTypeVar)element);
            else if (element instanceof ClassName)
                if (!element.isNullType())
                    return new ClassName(typeName(element.getName()));

            throw new BadBytecode("bad AASTORE: " + element);
        }

        @Override
        public void merge(TypeData t) {
            try {
                if (!t.isNullType())
                    element.merge(ArrayElement.make(t));
            }
            catch (BadBytecode e) {
                // never happens
                throw new RuntimeException("fatal: " + e);
            }
        }

        @Override
        public String getName() {
            return typeName(element.getName());
        }

        public AbsTypeVar elementType() { return element; }

        @Override
        public BasicType isBasicType() { return null; }
        @Override
        public boolean is2WordType() { return false; }

        /* elementType must be a class name.  Basic type names
         * are not allowed.
         */
        public static String typeName(String elementType) {
            if (elementType.charAt(0) == '[')
                return "[" + elementType;
            return "[L" + elementType.replace('.', '/') + ";";
        }

        @Override
        public void setType(String s, ClassPool cp) throws BadBytecode {
            element.setType(ArrayElement.typeName(s), cp);
        }

        @Override
        protected TypeVar toTypeVar(int dim) { return element.toTypeVar(dim + 1); }

        @Override
        public TypeData getArrayType(int dim) throws NotFoundException {
            return element.getArrayType(dim + 1);
        }

        @Override
        public int dfs(List<TypeData> order, int index, ClassPool cp) throws NotFoundException {
            return element.dfs(order, index, cp);
        }

        @Override
        String toString2(Set<TypeData> set) {
            return "[" + element.toString2(set);
        }
    }

    /* A type variable representing an array-element type.
     * It is a decorator of another type variable.  
     */ 
    public static class ArrayElement extends AbsTypeVar {
        private AbsTypeVar array;

        private ArrayElement(AbsTypeVar a) {   // a is never null
            array = a;
        }

        public static TypeData make(TypeData array) throws BadBytecode {
            if (array instanceof ArrayType)
                return ((ArrayType)array).elementType();
            else if (array instanceof AbsTypeVar)
                return new ArrayElement((AbsTypeVar)array);
            else if (array instanceof ClassName)
                if (!array.isNullType())
                    return new ClassName(typeName(array.getName()));

            throw new BadBytecode("bad AASTORE: " + array);
        }

        @Override
        public void merge(TypeData t) {
            try {
                if (!t.isNullType())
                    array.merge(ArrayType.make(t));
            }
            catch (BadBytecode e) {
                // never happens
                throw new RuntimeException("fatal: " + e);
            }
        }

        @Override
        public String getName() {
            return typeName(array.getName());
        }

        public AbsTypeVar arrayType() { return array; }

        /* arrayType must be a class name.  Basic type names are
         * not allowed.
         */

        @Override
        public BasicType isBasicType() { return null; }

        @Override
        public boolean is2WordType() { return false; }

        private static String typeName(String arrayType) {
            if (arrayType.length() > 1 && arrayType.charAt(0) == '[') {
                char c = arrayType.charAt(1);
                if (c == 'L')
                    return arrayType.substring(2, arrayType.length() - 1).replace('/', '.');
                else if (c == '[')
                    return arrayType.substring(1);
            }

            return "java.lang.Object";      // the array type may be NullType
        }

        @Override
        public void setType(String s, ClassPool cp) throws BadBytecode {
            array.setType(ArrayType.typeName(s), cp);
        }

        @Override
        protected TypeVar toTypeVar(int dim) { return array.toTypeVar(dim - 1); }

        @Override
        public TypeData getArrayType(int dim) throws NotFoundException {
            return array.getArrayType(dim - 1);
        }

        @Override
        public int dfs(List<TypeData> order, int index, ClassPool cp) throws NotFoundException {
            return array.dfs(order, index, cp);
        }

        @Override
        String toString2(Set<TypeData> set) {
            return "*" + array.toString2(set);
        }
    }

    public static class UninitTypeVar extends AbsTypeVar {
        protected TypeData type;    // UninitData or TOP

        public UninitTypeVar(UninitData t) { type = t; }
        @Override
        public int getTypeTag() { return type.getTypeTag(); }
        @Override
        public int getTypeData(ConstPool cp) { return type.getTypeData(cp); }
        @Override
        public BasicType isBasicType() { return type.isBasicType(); }
        @Override
        public boolean is2WordType() { return type.is2WordType(); }
        @Override
        public boolean isUninit() { return type.isUninit(); }
        @Override
        public boolean eq(TypeData d) { return type.eq(d); }
        @Override
        public String getName() { return type.getName(); }

        @Override
        protected TypeVar toTypeVar(int dim) { return null; }
        @Override
        public TypeData join() { return type.join(); }

        @Override
        public void setType(String s, ClassPool cp) throws BadBytecode {
            type.setType(s, cp);
        }

        @Override
        public void merge(TypeData t) {
            if (!t.eq(type))
                type = TypeTag.TOP;
        }

        @Override
        public void constructorCalled(int offset) {
            type.constructorCalled(offset);
        }

        public int offset() {
            if (type instanceof UninitData)
                return ((UninitData)type).offset;
            throw new RuntimeException("not available");
        }

        @Override
        public TypeData getArrayType(int dim) throws NotFoundException {
            return type.getArrayType(dim);
        }

        @Override
        String toString2(Set<TypeData> set) { return ""; }
    }

    /**
     * Type data for OBJECT.
     */
    public static class ClassName extends TypeData {
        private String name;    	// dot separated.

        public ClassName(String n) {
            name = n;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public BasicType isBasicType() { return null; }

        @Override
        public boolean is2WordType() { return false; }

        @Override
        public int getTypeTag() { return StackMapTable.OBJECT; }

        @Override
        public int getTypeData(ConstPool cp) {
            return cp.addClassInfo(getName());
        }

        @Override
        public boolean eq(TypeData d) {
            if (d.isUninit())
                return d.eq(this);
            else
                return name.equals(d.getName());
        }

        @Override
        public void setType(String typeName, ClassPool cp) throws BadBytecode {}

        @Override
        public TypeData getArrayType(int dim) throws NotFoundException {
            if (dim == 0)
                return this;
            else if (dim > 0) {
                char[] dimType = new char[dim];
                for (int i = 0; i < dim; i++)
                    dimType[i] = '[';

                String elementType = getName();
                if (elementType.charAt(0) != '[')
                    elementType = "L" + elementType.replace('.', '/') + ";";

                return new ClassName(new String(dimType) + elementType);
            }
            else {
                for (int i = 0; i < -dim; i++)
                    if (name.charAt(i) != '[')
                        throw new NotFoundException("no " + dim + " dimensional array type: " + getName());

                char type = name.charAt(-dim);
                if (type == '[')
                    return new ClassName(name.substring(-dim));
                else if (type == 'L')
                    return new ClassName(name.substring(-dim + 1, name.length() - 1).replace('/', '.'));
                else if (type == TypeTag.DOUBLE.decodedName)
                    return TypeTag.DOUBLE;
                else if (type == TypeTag.FLOAT.decodedName)
                    return TypeTag.FLOAT;
                else if (type == TypeTag.LONG.decodedName)
                    return TypeTag.LONG;
                else
                    return TypeTag.INTEGER;
            }
        }

        @Override
        String toString2(Set<TypeData> set) {
            return name;
        }
    }

    /**
     * Type data for NULL or OBJECT.
     * The types represented by the instances of this class are
     * initially NULL but will be OBJECT.
     */
    public static class NullType extends ClassName {
        public NullType() {
            super("null-type");      // type name
        }

        @Override
        public int getTypeTag() {
            return StackMapTable.NULL;
        }

        @Override
        public boolean isNullType() { return true; }
        @Override
        public int getTypeData(ConstPool cp) { return 0; }

        @Override
        public TypeData getArrayType(int dim) { return this; }
    }

    /**
     * Type data for UNINIT.
     */
    public static class UninitData extends ClassName {
        int offset;
        boolean initialized;

        UninitData(int offset, String className) {
            super(className);
            this.offset = offset;
            this.initialized = false;
        }

        public UninitData copy() { return new UninitData(offset, getName()); }

        @Override
        public int getTypeTag() {
            return StackMapTable.UNINIT;
        }

        @Override
        public int getTypeData(ConstPool cp) {
            return offset;
        }

        @Override
        public TypeData join() {
            if (initialized)
                return new TypeVar(new ClassName(getName()));
            return new UninitTypeVar(copy());
        }

        @Override
        public boolean isUninit() { return true; }

        @Override
        public boolean eq(TypeData d) {
            if (d instanceof UninitData) {
                UninitData ud = (UninitData)d;
                return offset == ud.offset && getName().equals(ud.getName());
            }
            return false;
        }

        public int offset() { return offset; }

        @Override
        public void constructorCalled(int offset) {
            if (offset == this.offset)
                initialized = true;
        }

        @Override
        String toString2(Set<TypeData> set) { return getName() + "," + offset; }
    }

    public static class UninitThis extends UninitData {
        UninitThis(String className) {
            super(-1, className);
        }

        @Override
        public UninitData copy() { return new UninitThis(getName()); }

        @Override
        public int getTypeTag() {
            return StackMapTable.THIS;
        }

        @Override
        public int getTypeData(ConstPool cp) {
            return 0;
        }

        @Override
        String toString2(Set<TypeData> set) { return "uninit:this"; }
    }
}
