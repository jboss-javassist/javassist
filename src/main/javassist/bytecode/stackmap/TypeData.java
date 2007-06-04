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

package javassist.bytecode.stackmap;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.StackMapTable;
import javassist.bytecode.BadBytecode;
import java.util.ArrayList;

public abstract class TypeData {
    /* Memo:
     * array type is a subtype of Cloneable and Serializable 
     */

    protected TypeData() {}

    public abstract void merge(TypeData neighbor);

    /**
     * Sets the type name of this object type.  If the given type name is
     * a subclass of the current type name, then the given name becomes
     * the name of this object type.
     *
     * @param className     dot-separated name unless the type is an array type. 
     */
    static void setType(TypeData td, String className, ClassPool cp) throws BadBytecode {
        if (td == TypeTag.TOP)
            throw new BadBytecode("unset variable");
        else
            td.setType(className, cp);
    }

    public abstract boolean equals(Object obj);

    public abstract int getTypeTag();
    public abstract int getTypeData(ConstPool cp);

    /*
     * See UninitData.getSelf().
     */
    public TypeData getSelf() { return this; } 

    /* An operand value is copied when it is stored in a
     * local variable.
     */
    public abstract TypeData copy();

    public abstract boolean isObjectType();
    public boolean is2WordType() { return false; }
    public boolean isNullType() { return false; }

    public abstract String getName() throws BadBytecode;
    protected abstract void setType(String s, ClassPool cp) throws BadBytecode;
    public abstract void evalExpectedType(ClassPool cp) throws BadBytecode;
    public abstract String getExpected() throws BadBytecode;

    /**
     * Primitive types.
     */
    protected static class BasicType extends TypeData {
        private String name;
        private int typeTag;

        public BasicType(String type, int tag) {
            name = type;
            typeTag = tag;
        }

        public void merge(TypeData neighbor) {}

        public boolean equals(Object obj) {
            return this == obj;
        }

        public int getTypeTag() { return typeTag; }
        public int getTypeData(ConstPool cp) { return 0; }

        public boolean isObjectType() { return false; }

        public boolean is2WordType() {
            return typeTag == StackMapTable.LONG
                    || typeTag == StackMapTable.DOUBLE;
        }

        public TypeData copy() {
            return this;
        }

        public void evalExpectedType(ClassPool cp) throws BadBytecode {}

        public String getExpected() throws BadBytecode {
            return name;
        }

        public String getName() {
            return name;
        }

        protected void setType(String s, ClassPool cp) throws BadBytecode {
            throw new BadBytecode("conflict: " + name + " and " + s);
        }

        public String toString() { return name; }
    }

    protected static abstract class TypeName extends TypeData {
        protected ArrayList equivalences;

        protected String expectedName;
        private CtClass cache;
        private boolean evalDone;

        protected TypeName() {
            equivalences = new ArrayList();
            equivalences.add(this);
            expectedName = null;
            cache = null;
            evalDone = false;
        }

        public void merge(TypeData neighbor) {
            if (this == neighbor)
                return;

            if (!(neighbor instanceof TypeName))
                return;     // neighbor might be UninitData

            TypeName neighbor2 = (TypeName)neighbor;
            ArrayList list = equivalences;
            ArrayList list2 = neighbor2.equivalences;
            if (list == list2)
                return;

            int n = list2.size();
            for (int i = 0; i < n; i++) {
                TypeName tn = (TypeName)list2.get(i);
                add(list, tn);
                tn.equivalences = list;
            }
        }

        private static void add(ArrayList list, TypeData td) {
            int n = list.size();
            for (int i = 0; i < n; i++)
                if (list.get(i) == td)
                    return;

            list.add(td);
        }

        /* NullType overrides this method.
         */
        public int getTypeTag() { return StackMapTable.OBJECT; }

        public int getTypeData(ConstPool cp) {
            String type;
            try {
                type = getExpected();
            } catch (BadBytecode e) {
                throw new RuntimeException("fatal error: ", e); 
            }

            return getTypeData2(cp, type);
        }

        /* NullType overrides this method.
         */
        protected int getTypeData2(ConstPool cp, String type) {
            return cp.addClassInfo(type);
        }

        public boolean equals(Object obj) {
            if (obj instanceof TypeName) {
                try {
                    TypeName tn = (TypeName)obj;
                    return getExpected().equals(tn.getExpected());
                }
                catch (BadBytecode e) {}
            }

            return false;
        }

        public boolean isObjectType() { return true; }

        protected void setType(String typeName, ClassPool cp) throws BadBytecode {
            if (update(cp, expectedName, typeName))
                expectedName = typeName;
        }

        public void evalExpectedType(ClassPool cp) throws BadBytecode {
            if (this.evalDone)
                return;

            ArrayList equiv = this.equivalences;
            int n = equiv.size();
            String name = evalExpectedType2(equiv, n);
            if (name == null) {
                name = this.expectedName;
                for (int i = 0; i < n; i++) {
                    TypeData td = (TypeData)equiv.get(i);
                    if (td instanceof TypeName) {
                        TypeName tn = (TypeName)td;
                        if (update(cp, name, tn.expectedName))
                            name = tn.expectedName;
                    }
                }
            }

            for (int i = 0; i < n; i++) {
                TypeData td = (TypeData)equiv.get(i);
                if (td instanceof TypeName) {
                    TypeName tn = (TypeName)td;
                    tn.expectedName = name;
                    tn.cache = null;
                    tn.evalDone = true;
                }
            }
        }

        private String evalExpectedType2(ArrayList equiv, int n) throws BadBytecode {
            String origName = null;
            for (int i = 0; i < n; i++) {
                TypeData td = (TypeData)equiv.get(i);
                if (!td.isNullType())
                    if (origName == null)
                        origName = td.getName();
                    else if (!origName.equals(td.getName()))
                        return null;
            }

            return origName;
        }

        protected boolean isTypeName() { return true; }

        private boolean update(ClassPool cp, String oldName, String typeName) throws BadBytecode {
            if (typeName == null)
                return false;
            else if (oldName == null)
                return true;
            else if (oldName.equals(typeName))
                return false;
            else if (typeName.charAt(0) == '['
                     && oldName.equals("[Ljava.lang.Object;")) {
                /* this rule is not correct but Tracer class sets the type
                   of the operand of arraylength to java.lang.Object[].
                   Thus, int[] etc. must be a subtype of java.lang.Object[].
                 */
                return true;
            }

            try {
                if (cache == null)
                    cache = cp.get(oldName);
    
                CtClass cache2 = cp.get(typeName);
                if (cache2.subtypeOf(cache)) {
                    cache = cache2;
                    return true;
                }
                else
                    return false;
            }
            catch (NotFoundException e) {
                throw new BadBytecode("cannot find " + e.getMessage());
            }
        }

        /* See also NullType.getExpected().
         */
        public String getExpected() throws BadBytecode {
            ArrayList equiv = equivalences;
            if (equiv.size() == 1)
                return getName();
            else {
                String en = expectedName;
                if (en == null)
                    return "java.lang.Object";
                else
                    return en;
            }
        }

        public String toString() {
            try {
                String en = expectedName;
                if (en != null)
                    return en;

                String name = getName();
                if (equivalences.size() == 1)
                    return name;
                else
                    return name + "?";
            }
            catch (BadBytecode e) {
                return "<" + e.getMessage() + ">";
            }
        }
    }

    /**
     * Type data for OBJECT.
     */
    public static class ClassName extends TypeName {
        private String name;    // dot separated.  null if this object is a copy of another.
    
        public ClassName(String n) {
            name = n;
        }

        public TypeData copy() {
            return new ClassName(name);
        }

        public String getName() {   // never returns null.
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
            super("null");      // type name
        }

        public TypeData copy() {
            return new NullType();
        }

        public boolean isNullType() { return true; }

        public int getTypeTag() {
            try {
                if ("null".equals(getExpected()))
                    return StackMapTable.NULL;
                else
                    return super.getTypeTag();
            }
            catch (BadBytecode e) {
                throw new RuntimeException("fatal error: ", e); 
            }
        }

        protected int getTypeData2(ConstPool cp, String type) {
            if ("null".equals(type))
                return 0;
            else
                return super.getTypeData2(cp, type);
        }

        public String getExpected() throws BadBytecode {
            String en = expectedName;
            if (en == null) {
              // ArrayList equiv = equivalences;
              // if (equiv.size() == 1)
              //    return getName();
              // else
                    return "java.lang.Object";
            }
            else
                return en;
        }
    }

    /**
     * Type data for OBJECT if the type is an object type and is
     * derived as an element type from an array type by AALOAD.
     */
    public static class ArrayElement extends TypeName {
        TypeData array;
    
        public ArrayElement(TypeData a) {   // a is never null
            array = a;
        }

        public TypeData copy() {
            return new ArrayElement(array);
        }

        protected void setType(String typeName, ClassPool cp) throws BadBytecode {
            super.setType(typeName, cp);
            array.setType(getArrayType(typeName), cp);
        }

        public String getName() throws BadBytecode {
            String name = array.getName();
            if (name.length() > 1 && name.charAt(0) == '[') {
                char c = name.charAt(1);
                if (c == 'L')
                    return name.substring(2, name.length() - 1).replace('/', '.');                    
                else if (c == '[')
                    return name.substring(1);
            }
    
            throw new BadBytecode("bad array type for AALOAD: "
                                  + name);
        }

        public static String getArrayType(String elementType) {
            if (elementType.charAt(0) == '[')
                return "[" + elementType;
            else
                return "[L" + elementType.replace('.', '/') + ";";
        }

        public static String getElementType(String arrayType) {
            char c = arrayType.charAt(1);
            if (c == 'L')
                return arrayType.substring(2, arrayType.length() - 1).replace('/', '.');                    
            else if (c == '[')
                return arrayType.substring(1);
            else
                return arrayType;
        }
    }

    /**
     * Type data for UNINIT.
     */
    public static class UninitData extends TypeData {
        String className;
        int offset;
        boolean initialized;

        UninitData(int offset, String className) {
            this.className = className;
            this.offset = offset;
            this.initialized = false;
        }

        public void merge(TypeData neighbor) {}

        public int getTypeTag() { return StackMapTable.UNINIT; }
        public int getTypeData(ConstPool cp) { return offset; }

        public boolean equals(Object obj) {
            if (obj instanceof UninitData) {
                UninitData ud = (UninitData)obj;
                return offset == ud.offset && className.equals(ud.className);
            }
            else
                return false;
        }

        public TypeData getSelf() {
            if (initialized)
                return copy();
            else
                return this;
        }

        public TypeData copy() {
            return new ClassName(className);
        }

        public boolean isObjectType() { return true; }

        protected void setType(String typeName, ClassPool cp) throws BadBytecode {
            initialized = true;
        }

        public void evalExpectedType(ClassPool cp) throws BadBytecode {}

        public String getName() {
            return className;
        }

        public String getExpected() { return className; }

        public String toString() { return "uninit:" + className + "@" + offset; }
    }

    public static class UninitThis extends UninitData {
        UninitThis(String className) {
            super(-1, className);
        }

        public int getTypeTag() { return StackMapTable.THIS; }
        public int getTypeData(ConstPool cp) { return 0; }

        public boolean equals(Object obj) {
            return obj instanceof UninitThis;
        }

        public String toString() { return "uninit:this"; }
    }
}
