/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
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

package javassist.compiler;

import java.util.List;
import javassist.*;
import javassist.bytecode.*;
import javassist.compiler.ast.*;

/* Code generator methods depending on javassist.* classes.
 */
public class MemberResolver implements TokenId {
    private ClassPool classPool;

    public MemberResolver(ClassPool cp) {
        classPool = cp;
    }

    public ClassPool getClassPool() { return classPool; }

    private static void fatal() throws CompileError {
        throw new CompileError("fatal");
    }

    public static class Method {
        public CtClass declaring;
        public MethodInfo info;

        public Method(CtClass c, MethodInfo i) {
            declaring = c;
            info = i;
        }

        /**
         * Returns true if the invoked method is static.
         */
        public boolean isStatic() {
            int acc = info.getAccessFlags();
            return (acc & AccessFlag.STATIC) != 0;
        }
    }

    public Method lookupMethod(CtClass clazz, MethodInfo current,
                               String methodName,
                               int[] argTypes, int[] argDims,
                               String[] argClassNames, boolean onlyExact)
        throws CompileError
    {
        Method maybe = null;

        // to enable the creation of a recursively called method
        if (current != null)
            if (current.getName().equals(methodName)) {
                int res = compareSignature(current.getDescriptor(),
                                           argTypes, argDims, argClassNames);
                Method r = new Method(clazz, current);
                if (res == YES)
                    return r;
                else if (res == MAYBE && maybe == null)
                    maybe = r;
            }

        List list = clazz.getClassFile2().getMethods();
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            if (minfo.getName().equals(methodName)) {
                int res = compareSignature(minfo.getDescriptor(),
                                           argTypes, argDims, argClassNames);
                Method r = new Method(clazz, minfo);
                if (res == YES)
                    return r;
                else if (res == MAYBE && maybe == null)
                    maybe = r;
            }
        }

        try {
            CtClass pclazz = clazz.getSuperclass();
            if (pclazz != null) {
                Method r = lookupMethod(pclazz, null, methodName, argTypes,
                                        argDims, argClassNames,
                                        (onlyExact || maybe != null));
                if (r != null)
                    return r;
            }
        }
        catch (NotFoundException e) {}

        /* -- not necessary to search implemented interfaces.
        try {
            CtClass[] ifs = clazz.getInterfaces();
            int size = ifs.length;
            for (int i = 0; i < size; ++i) {
                Object[] r = lookupMethod(ifs[i], methodName, argTypes,
                                          argDims, argClassNames);
                if (r != null)
                    return r;
            }
        }
        catch (NotFoundException e) {}
        */

        if (onlyExact)
            return null;
        else
            return maybe;
    }

    private static final int YES = 2;
    private static final int MAYBE = 1;
    private static final int NO = 0;

    /*
     * Returns YES if actual parameter types matches the given signature.
     *
     * argTypes, argDims, and argClassNames represent actual parameters.
     *
     * This method does not correctly implement the Java method dispatch
     * algorithm.
     */
    private int compareSignature(String desc, int[] argTypes,
                                 int[] argDims, String[] argClassNames)
        throws CompileError
    {
        int result = YES;
        int i = 1;
        int nArgs = argTypes.length;
        if (nArgs != Descriptor.numOfParameters(desc))
            return NO;

        int len = desc.length();
        for (int n = 0; i < len; ++n) {
            char c = desc.charAt(i++);
            if (c == ')')
                return (n == nArgs ? result : NO);
            else if (n >= nArgs)
                return NO;

            int dim = 0;
            while (c == '[') {
                ++dim;
                c = desc.charAt(i++);
            }

            if (argTypes[n] == NULL) {
                if (dim == 0 && c != 'L')
                    return NO;
            }
            else if (argDims[n] != dim) {
                if (!(dim == 0 && c == 'L'
                      && desc.startsWith("java/lang/Object;", i)))
                    return NO;

                // if the thread reaches here, c must be 'L'.
                i = desc.indexOf(';', i) + 1;
                result = MAYBE;
                if (i <= 0)
                    return NO;  // invalid descriptor?
            }
            else if (c == 'L') {        // not compare
                int j = desc.indexOf(';', i);
                if (j < 0 || argTypes[n] != CLASS)
                    return NO;

                String cname = desc.substring(i, j);
                if (!cname.equals(argClassNames[n])) {
                    CtClass clazz = lookupClassByJvmName(argClassNames[n]);
                    try {
                        if (clazz.subtypeOf(lookupClassByJvmName(cname)))
                            result = MAYBE;
                        else
                            return NO;
                    }
                    catch (NotFoundException e) {
                        result = MAYBE; // should be NO?
                    }
                }

                i = j + 1;
            }
            else {
                int t = descToType(c);
                int at = argTypes[n];
                if (t != at)
                    if (t == INT
                        && (at == SHORT || at == BYTE || at == CHAR))
                        result = MAYBE;
                    else
                        return NO;
            }
        }

        return NO;
    }

    /**
     * @param jvmClassName  a JVM class name.  e.g. java/lang/String
     */
    public CtField lookupFieldByJvmName(String jvmClassName, Symbol fieldName)
        throws CompileError
    {
        return lookupField(jvmToJavaName(jvmClassName), fieldName);
    }

    // never used??
    private CtField lookupField2(ASTList className, Symbol fieldName)
        throws CompileError
    {
        return lookupField(Declarator.astToClassName(className, '.'),
                            fieldName);
    }

    /**
     * @param name      a qualified class name. e.g. java.lang.String
     */
    public CtField lookupField(String className, Symbol fieldName)
        throws CompileError
    {
        CtClass cc = lookupClass(className);
        try {
            return cc.getField(fieldName.get());
        }
        catch (NotFoundException e) {}
        throw new CompileError("no such field: " + fieldName.get());
    }

    public CtClass lookupClassByName(ASTList name) throws CompileError {
        return lookupClass(Declarator.astToClassName(name, '.'));
    }

    public CtClass lookupClassByJvmName(String jvmName) throws CompileError {
        return lookupClass(jvmToJavaName(jvmName));
    }

    public CtClass lookupClass(Declarator decl) throws CompileError {
        return lookupClass(decl.getType(), decl.getArrayDim(),
                           decl.getClassName());
    }

    /**
     * @parma classname         jvm class name.
     */
    public CtClass lookupClass(int type, int dim, String classname)
        throws CompileError
    {
        String cname = "";
        CtClass clazz;
        switch (type) {
        case CLASS :
            clazz = lookupClassByJvmName(classname);
            if (dim > 0)
                cname = clazz.getName();
            else
                return clazz;

            break;
        case BOOLEAN :
            cname = "boolean";
            break;
        case CHAR :
            cname = "char";
            break;
        case BYTE :
            cname = "byte";
            break;
        case SHORT :
            cname = "short";
            break;
        case INT :
            cname = "int";
            break;
        case LONG :
            cname = "long";
            break;
        case FLOAT :
            cname = "float";
            break;
        case DOUBLE :
            cname = "double";
            break;
        case VOID :
            cname = "void";
            break;
        default :
            fatal();
        }

        while (dim-- > 0)
            cname += "[]";

        return lookupClass(cname);
    }

    /**
     * @param name      a qualified class name. e.g. java.lang.String
     */
    public CtClass lookupClass(String name) throws CompileError {
        try {
            return classPool.get(name);
        }
        catch (NotFoundException e) {}

        try {
            if (name.indexOf('.') < 0)
                return classPool.get("java.lang." + name);
        }
        catch (NotFoundException e) {}

        throw new CompileError("no such class: " + name);
    }

    /* Converts a class name into a JVM-internal representation.
     *
     * It may also expand a simple class name to java.lang.*.
     * For example, this converts Object into java/lang/Object.
     */
    public String resolveClassName(ASTList name) throws CompileError {
        if (name == null)
            return null;
        else
            return javaToJvmName(lookupClassByName(name).getName());
    }

    /* Expands a simple class name to java.lang.*.
     * For example, this converts Object into java/lang/Object.
     */
    public String resolveJvmClassName(String jvmName) throws CompileError {
        if (jvmName == null)
            return null;
        else
            return javaToJvmName(lookupClassByJvmName(jvmName).getName());
    }

    public static CtClass getSuperclass(CtClass c) throws CompileError {
        try {
            return c.getSuperclass();
        }
        catch (NotFoundException e) {
            throw new CompileError("cannot find the super class of "
                                   + c.getName());
        }
    }

    public static String javaToJvmName(String classname) {
        return classname.replace('.', '/');
    }

    public static String jvmToJavaName(String classname) {
        return classname.replace('/', '.');
    }

    public static int jvmTypeNameToExprType(char type) {
        switch(type) {
        case 'Z' :
            return BOOLEAN;
        case 'B' :
            return BYTE;
        case 'C' :
            return CHAR;
        case 'S' :
            return SHORT;
        case 'I' :
            return INT;
        case 'J' :
            return LONG;
        case 'F' :
            return FLOAT;
        case 'D' :
            return DOUBLE;
        case 'V' :
            return VOID;
        default :
            return CLASS;
        }
    }

    public static int descToType(char c) throws CompileError {
        switch (c) {
        case 'Z' :
            return BOOLEAN;
        case 'C' :
            return CHAR;
        case 'B' :
            return  BYTE;
        case 'S' :
            return SHORT;
        case 'I' :
            return INT;
        case 'J' :
            return LONG;
        case 'F' :
            return FLOAT;
        case 'D' :
            return DOUBLE;
        case 'V' :
            return VOID;
        case 'L' :
        case '[' :
            return CLASS;
        default :
            fatal();
            return VOID;
        }
    }

    public static int getModifiers(ASTList mods) {
        int m = 0;
        while (mods != null) {
            Keyword k = (Keyword)mods.head();
            mods = mods.tail();
            switch (k.get()) {
            case STATIC :
                m |= Modifier.STATIC;
                break;
            case FINAL :
                m |= Modifier.FINAL;
                break;
            case SYNCHRONIZED :
                m |= Modifier.SYNCHRONIZED;
                break;
            case ABSTRACT :
                m |= Modifier.ABSTRACT;
                break;
            case PUBLIC :
                m |= Modifier.PUBLIC;
                break;
            case PROTECTED :
                m |= Modifier.PROTECTED;
                break;
            case PRIVATE :
                m |= Modifier.PRIVATE;
                break;
            case VOLATILE :
                m |= Modifier.VOLATILE;
                break;
            case TRANSIENT :
                m |= Modifier.TRANSIENT;
                break;
            case STRICT :
                m |= Modifier.STRICT;
                break;
            }
        }

        return m;
    }
}
