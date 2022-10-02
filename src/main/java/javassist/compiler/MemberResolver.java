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

package javassist.compiler;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.compiler.ast.ASTList;
import javassist.compiler.ast.ASTree;
import javassist.compiler.ast.Declarator;
import javassist.compiler.ast.Keyword;
import javassist.compiler.ast.Symbol;

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
        public int notmatch;

        public Method(CtClass c, MethodInfo i, int n) {
            declaring = c;
            info = i;
            notmatch = n;
        }

        /**
         * Returns true if the invoked method is static.
         */
        public boolean isStatic() {
            int acc = info.getAccessFlags();
            return (acc & AccessFlag.STATIC) != 0;
        }
    }

    public Method lookupMethod(CtClass clazz, CtClass currentClass, MethodInfo current,
                                String methodName,
                                int[] argTypes, int[] argDims,
                                String[] argClassNames)
        throws CompileError
    {
        Method maybe = null;
        // to enable the creation of a recursively called method
        if (current != null && clazz == currentClass)
            if (current.getName().equals(methodName)) {
                int res = compareSignature(current.getDescriptor(),
                                           argTypes, argDims, argClassNames);
                if (res != NO) {
                    Method r = new Method(clazz, current, res);
                    if (res == YES)
                        return r;
                    maybe = r;
                }
            }

        Method m = lookupMethod(clazz, methodName, argTypes, argDims,
                                argClassNames, maybe != null);
        if (m != null)
            return m;
        return maybe;
    }

    private Method lookupMethod(CtClass clazz, String methodName,
                               int[] argTypes, int[] argDims,
                               String[] argClassNames, boolean onlyExact)
        throws CompileError
    {
        Method maybe = null;
        ClassFile cf = clazz.getClassFile2();
        // If the class is an array type, the class file is null.
        // If so, search the super class java.lang.Object for clone() etc.
        if (cf != null) {
            List<MethodInfo> list = cf.getMethods();
            for (MethodInfo minfo:list) {
                if (minfo.getName().equals(methodName)
                    && (minfo.getAccessFlags() & AccessFlag.BRIDGE) == 0) {
                    int res = compareSignature(minfo.getDescriptor(),
                                           argTypes, argDims, argClassNames);
                    if (res != NO) {
                        Method r = new Method(clazz, minfo, res);
                        if (res == YES)
                            return r;
                        else if (maybe == null || maybe.notmatch > res)
                            maybe = r;
                    }
                }
            }
        }

        if (onlyExact)
            maybe = null;
        else
            if (maybe != null)
                return maybe;

        int mod = clazz.getModifiers();
        boolean isIntf = Modifier.isInterface(mod);
        try {
            // skip searching java.lang.Object if clazz is an interface type.
            if (!isIntf) {
                CtClass pclazz = clazz.getSuperclass();
                if (pclazz != null) {
                    Method r = lookupMethod(pclazz, methodName, argTypes,
                                            argDims, argClassNames, onlyExact);
                    if (r != null)
                        return r;
                }
            }
        }
        catch (NotFoundException e) {}

        try {
            CtClass[] ifs = clazz.getInterfaces();
            for (CtClass intf:ifs) {
                Method r = lookupMethod(intf, methodName,
                        argTypes, argDims, argClassNames,
                        onlyExact);
                if (r != null)
                    return r;
            }

            if (isIntf) {
                // finally search java.lang.Object.
                CtClass pclazz = clazz.getSuperclass();
                if (pclazz != null) {
                    Method r = lookupMethod(pclazz, methodName, argTypes,
                                            argDims, argClassNames, onlyExact);
                    if (r != null)
                        return r;
                }
            }
        }
        catch (NotFoundException e) {}

        return maybe;
    }

    private static final int YES = 0;
    private static final int NO = -1;

    /*
     * Returns YES if actual parameter types matches the given signature.
     *
     * argTypes, argDims, and argClassNames represent actual parameters.
     *
     * This method does not correctly implement the Java method dispatch
     * algorithm.
     *
     * If some of the parameter types exactly match but others are subtypes of
     * the corresponding type in the signature, this method returns the number
     * of parameter types that do not exactly match.
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

                if (c == 'L')
                    i = desc.indexOf(';', i) + 1;
            }
            else if (argDims[n] != dim) {
                if (!(dim == 0 && c == 'L'
                      && desc.startsWith("java/lang/Object;", i)))
                    return NO;

                // if the thread reaches here, c must be 'L'.
                i = desc.indexOf(';', i) + 1;
                result++;
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
                            result++;
                        else
                            return NO;
                    }
                    catch (NotFoundException e) {
                        result++; // should be NO?
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
                        result++;
                    else
                        return NO;
            }
        }

        return NO;
    }

    /**
     * Only used by fieldAccess() in MemberCodeGen and TypeChecker.
     *
     * @param jvmClassName  a JVM class name.  e.g. java/lang/String
     * @see #lookupClass(String, boolean)
     */
    public CtField lookupFieldByJvmName2(String jvmClassName, Symbol fieldSym,
                                         ASTree expr) throws NoFieldException
    {
        String field = fieldSym.get();
        CtClass cc = null;
        try {
            cc = lookupClass(jvmToJavaName(jvmClassName), true);
        }
        catch (CompileError e) {
            // EXPR might be part of a qualified class name.
            throw new NoFieldException(jvmClassName + "/" + field, expr);
        }

        try {
            return cc.getField(field);
        }
        catch (NotFoundException e) {
            // maybe an inner class.
            jvmClassName = javaToJvmName(cc.getName());
            throw new NoFieldException(jvmClassName + "$" + field, expr);
        }
    }

    /**
     * @param jvmClassName  a JVM class name.  e.g. java/lang/String
     */
    public CtField lookupFieldByJvmName(String jvmClassName, Symbol fieldName)
        throws CompileError
    {
        return lookupField(jvmToJavaName(jvmClassName), fieldName);
    }

    /**
     * @param className      a qualified class name. e.g. java.lang.String
     */
    public CtField lookupField(String className, Symbol fieldName)
        throws CompileError
    {
        CtClass cc = lookupClass(className, false);
        try {
            return cc.getField(fieldName.get());
        }
        catch (NotFoundException e) {}
        throw new CompileError("no such field: " + fieldName.get());
    }

    public CtClass lookupClassByName(ASTList name) throws CompileError {
        return lookupClass(Declarator.astToClassName(name, '.'), false);
    }

    public CtClass lookupClassByJvmName(String jvmName) throws CompileError {
        return lookupClass(jvmToJavaName(jvmName), false);
    }

    public CtClass lookupClass(Declarator decl) throws CompileError {
        return lookupClass(decl.getType(), decl.getArrayDim(),
                           decl.getClassName());
    }

    /**
     * @param classname         jvm class name.
     */
    public CtClass lookupClass(int type, int dim, String classname)
        throws CompileError
    {
        String cname = "";
        CtClass clazz;
        if (type == CLASS) {
            clazz = lookupClassByJvmName(classname);
            if (dim > 0)
                cname = clazz.getName();
            else
                return clazz;
        }
        else
            cname = getTypeName(type);

        while (dim-- > 0)
            cname += "[]";

        return lookupClass(cname, false);
    }

    /*
     * type cannot be CLASS
     */
    static String getTypeName(int type) throws CompileError {
        String cname = "";
        switch (type) {
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

        return cname;
    }

    /**
     * @param name      a qualified class name. e.g. java.lang.String
     */
    public CtClass lookupClass(String name, boolean notCheckInner)
        throws CompileError
    {
        Map<String,String> cache = getInvalidNames();
        String found = cache.get(name);
        if (found == INVALID)
            throw new CompileError("no such class: " + name);
        else if (found != null)
            try {
                return classPool.get(found);
            }
            catch (NotFoundException e) {}

        CtClass cc = null;
        try {
            cc = lookupClass0(name, notCheckInner);
        }
        catch (NotFoundException e) {
            cc = searchImports(name);
        }

        cache.put(name, cc.getName());
        return cc;
    }

    private static final String INVALID = "<invalid>";
    private static Map<ClassPool, Reference<Map<String,String>>> invalidNamesMap =
            new WeakHashMap<ClassPool, Reference<Map<String,String>>>();
    private Map<String,String> invalidNames = null;

    // for unit tests
    public static int getInvalidMapSize() { return invalidNamesMap.size(); }

    private Map<String,String> getInvalidNames() {
        Map<String,String> ht = invalidNames;
        if (ht == null) {
            synchronized (MemberResolver.class) {
                Reference<Map<String,String>> ref = invalidNamesMap.get(classPool);
                if (ref != null)
                    ht = ref.get();

                if (ht == null) {
                    ht = new Hashtable<String,String>();
                    invalidNamesMap.put(classPool, new WeakReference<Map<String,String>>(ht));
                }
            }

            invalidNames = ht;
        }

        return ht;
    }

    private CtClass searchImports(String orgName)
        throws CompileError
    {
        if (orgName.indexOf('.') < 0) {
            Iterator<String> it = classPool.getImportedPackages();
            while (it.hasNext()) {
                String pac = it.next();
                String fqName = pac.replaceAll("\\.$","") + "." + orgName;
                try {
                    return classPool.get(fqName);
                }
                catch (NotFoundException e) {
                    try {
                        if (pac.endsWith("." + orgName))
                            return classPool.get(pac);
                    }
                    catch (NotFoundException e2) {}
                }
            }
        }

        getInvalidNames().put(orgName, INVALID);
        throw new CompileError("no such class: " + orgName);
    }

    private CtClass lookupClass0(String classname, boolean notCheckInner)
        throws NotFoundException
    {
        CtClass cc = null;
        do {
            try {
                cc = classPool.get(classname);
            }
            catch (NotFoundException e) {
                int i = classname.lastIndexOf('.');
                if (notCheckInner || i < 0)
                    throw e;
                StringBuilder sbuf = new StringBuilder(classname);
                sbuf.setCharAt(i, '$');
                classname = sbuf.toString();
            }
        } while (cc == null);
        return cc;
    }

    /* Converts a class name into a JVM-internal representation.
     *
     * It may also expand a simple class name to java.lang.*.
     * For example, this converts Object into java/lang/Object.
     */
    public String resolveClassName(ASTList name) throws CompileError {
        if (name == null)
            return null;
        return javaToJvmName(lookupClassByName(name).getName());
    }

    /* Expands a simple class name to java.lang.*.
     * For example, this converts Object into java/lang/Object.
     */
    public String resolveJvmClassName(String jvmName) throws CompileError {
        if (jvmName == null)
            return null;
        return javaToJvmName(lookupClassByJvmName(jvmName).getName());
    }

    public static CtClass getSuperclass(CtClass c) throws CompileError {
        try {
            CtClass sc = c.getSuperclass();
            if (sc != null)
                return sc;
        }
        catch (NotFoundException e) {}
        throw new CompileError("cannot find the super class of "
                               + c.getName());
    }

    public static CtClass getSuperInterface(CtClass c, String interfaceName)
        throws CompileError
    {
        try {
            CtClass[] intfs = c.getInterfaces();
            for (int i = 0; i < intfs.length; i++)
                if (intfs[i].getName().equals(interfaceName))
                    return intfs[i];
        } catch (NotFoundException e) {}
        throw new CompileError("cannot find the super interface " + interfaceName
                               + " of " + c.getName());
    }

    public static String javaToJvmName(String classname) {
        return classname.replace('.', '/');
    }

    public static String jvmToJavaName(String classname) {
        return classname.replace('/', '.');
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
            return VOID;    // never reach here
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
