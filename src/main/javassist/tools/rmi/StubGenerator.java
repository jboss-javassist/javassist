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

package javassist.tools.rmi;

import javassist.*;
import java.lang.reflect.Method;
import java.util.Hashtable;
import javassist.CtMethod.ConstParameter;

/**
 * A stub-code generator.  It is used for producing a proxy class.
 *
 * <p>The proxy class for class A is as follows:
 *
 * <ul><pre>public class A implements Proxy, Serializable {
 *   private ObjectImporter importer;
 *   private int objectId;
 *   public int _getObjectId() { return objectId; }
 *   public A(ObjectImporter oi, int id) {
 *     importer = oi; objectId = id;
 *   }
 *
 *   ... the same methods that the original class A declares ...
 * }</pre></ul>
 *
 * <p>Instances of the proxy class is created by an
 * <code>ObjectImporter</code> object.
 */
public class StubGenerator implements Translator {
    private static final String fieldImporter = "importer";
    private static final String fieldObjectId = "objectId";
    private static final String accessorObjectId = "_getObjectId";
    private static final String sampleClass = "javassist.tools.rmi.Sample";

    private ClassPool classPool;
    private Hashtable proxyClasses;
    private CtMethod forwardMethod;
    private CtMethod forwardStaticMethod;

    private CtClass[] proxyConstructorParamTypes;
    private CtClass[] interfacesForProxy;
    private CtClass[] exceptionForProxy;

    /**
     * Constructs a stub-code generator.
     */
    public StubGenerator() {
        proxyClasses = new Hashtable();
    }

    /**
     * Initializes the object.
     * This is a method declared in javassist.Translator.
     *
     * @see javassist.Translator#start(ClassPool)
     */
    public void start(ClassPool pool) throws NotFoundException {
        classPool = pool;
        CtClass c = pool.get(sampleClass);
        forwardMethod = c.getDeclaredMethod("forward");
        forwardStaticMethod = c.getDeclaredMethod("forwardStatic");

        proxyConstructorParamTypes
            = pool.get(new String[] { "javassist.tools.rmi.ObjectImporter",
                                         "int" });
        interfacesForProxy
            = pool.get(new String[] { "java.io.Serializable",
                                         "javassist.tools.rmi.Proxy" });
        exceptionForProxy
            = new CtClass[] { pool.get("javassist.tools.rmi.RemoteException") };
    }

    /**
     * Does nothing.
     * This is a method declared in javassist.Translator.
     * @see javassist.Translator#onLoad(ClassPool,String)
     */
    public void onLoad(ClassPool pool, String classname) {}

    /**
     * Returns <code>true</code> if the specified class is a proxy class
     * recorded by <code>makeProxyClass()</code>.
     *
     * @param name              a fully-qualified class name
     */
    public boolean isProxyClass(String name) {
        return proxyClasses.get(name) != null;
    }

    /**
     * Makes a proxy class.  The produced class is substituted
     * for the original class.
     *
     * @param clazz             the class referenced
     *                          through the proxy class.
     * @return          <code>false</code> if the proxy class
     *                  has been already produced.
     */
    public synchronized boolean makeProxyClass(Class clazz)
        throws CannotCompileException, NotFoundException
    {
        String classname = clazz.getName();
        if (proxyClasses.get(classname) != null)
            return false;
        else {
            CtClass ctclazz = produceProxyClass(classPool.get(classname),
                                                clazz);
            proxyClasses.put(classname, ctclazz);
            modifySuperclass(ctclazz);
            return true;
        }
    }

    private CtClass produceProxyClass(CtClass orgclass, Class orgRtClass)
        throws CannotCompileException, NotFoundException
    {
        int modify = orgclass.getModifiers();
        if (Modifier.isAbstract(modify) || Modifier.isNative(modify)
            || !Modifier.isPublic(modify))
            throw new CannotCompileException(orgclass.getName()
                        + " must be public, non-native, and non-abstract.");

        CtClass proxy = classPool.makeClass(orgclass.getName(),
                                              orgclass.getSuperclass());

        proxy.setInterfaces(interfacesForProxy);

        CtField f
            = new CtField(classPool.get("javassist.tools.rmi.ObjectImporter"),
                          fieldImporter, proxy);
        f.setModifiers(Modifier.PRIVATE);
        proxy.addField(f, CtField.Initializer.byParameter(0));

        f = new CtField(CtClass.intType, fieldObjectId, proxy);
        f.setModifiers(Modifier.PRIVATE);
        proxy.addField(f, CtField.Initializer.byParameter(1)); 

        proxy.addMethod(CtNewMethod.getter(accessorObjectId, f));

        proxy.addConstructor(CtNewConstructor.defaultConstructor(proxy));
        CtConstructor cons
            = CtNewConstructor.skeleton(proxyConstructorParamTypes,
                                        null, proxy);
        proxy.addConstructor(cons);

        try {
            addMethods(proxy, orgRtClass.getMethods());
            return proxy;
        }
        catch (SecurityException e) {
            throw new CannotCompileException(e);
        }
    }

    private CtClass toCtClass(Class rtclass) throws NotFoundException {
        String name;
        if (!rtclass.isArray())
            name = rtclass.getName();
        else {
            StringBuffer sbuf = new StringBuffer();
            do {
                sbuf.append("[]");
                rtclass = rtclass.getComponentType();
            } while(rtclass.isArray());
            sbuf.insert(0, rtclass.getName());
            name = sbuf.toString();
        }
            
        return classPool.get(name);
    }

    private CtClass[] toCtClass(Class[] rtclasses) throws NotFoundException {
        int n = rtclasses.length;
        CtClass[] ctclasses = new CtClass[n];
        for (int i = 0; i < n; ++i)
            ctclasses[i] = toCtClass(rtclasses[i]);

        return ctclasses;
    }

    /* ms must not be an array of CtMethod.  To invoke a method ms[i]
     * on a server, a client must send i to the server.
     */
    private void addMethods(CtClass proxy, Method[] ms)
        throws CannotCompileException, NotFoundException
    {
        CtMethod wmethod;
        for (int i = 0; i < ms.length; ++i) {
            Method m = ms[i];
            int mod = m.getModifiers();
            if (m.getDeclaringClass() != Object.class
                        && !Modifier.isFinal(mod))
                if (Modifier.isPublic(mod)) {
                    CtMethod body;
                    if (Modifier.isStatic(mod))
                        body = forwardStaticMethod;
                    else
                        body = forwardMethod;

                    wmethod
                        = CtNewMethod.wrapped(toCtClass(m.getReturnType()),
                                              m.getName(),
                                              toCtClass(m.getParameterTypes()),
                                              exceptionForProxy,
                                              body,
                                              ConstParameter.integer(i),
                                              proxy);
                    wmethod.setModifiers(mod);
                    proxy.addMethod(wmethod);
                }
                else if (!Modifier.isProtected(mod)
                         && !Modifier.isPrivate(mod))
                    // if package method
                    throw new CannotCompileException(
                        "the methods must be public, protected, or private.");
        }
    }

    /**
     * Adds a default constructor to the super classes.
     */
    private void modifySuperclass(CtClass orgclass)
        throws CannotCompileException, NotFoundException
    {
        CtClass superclazz;
        for (;; orgclass = superclazz) {
            superclazz = orgclass.getSuperclass();
            if (superclazz == null)
                break;

            try {
                superclazz.getDeclaredConstructor(null);
                break;  // the constructor with no arguments is found.
            }
            catch (NotFoundException e) {
            }

            superclazz.addConstructor(
                        CtNewConstructor.defaultConstructor(superclazz));
        }
    }
}
