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

package javassist.util.proxy;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.lang.invoke.MethodHandles.Lookup;

import javassist.CannotCompileException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.DuplicateMemberException;
import javassist.bytecode.ExceptionsAttribute;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.StackMapTable;

/*
 * This class is implemented only with the lower-level API of Javassist.
 * This design decision is for maximizing performance.
 */

/**
 * Factory of dynamic proxy classes.
 *
 * <p>This factory generates a class that extends the given super class and implements
 * the given interfaces.  The calls of the methods inherited from the super class are
 * forwarded and then <code>invoke()</code> is called on the method handler
 * associated with instances of the generated class.  The calls of the methods from
 * the interfaces are also forwarded to the method handler.
 *
 * <p>For example, if the following code is executed,
 * 
 * <pre>
 * ProxyFactory f = new ProxyFactory();
 * f.setSuperclass(Foo.class);
 * f.setFilter(new MethodFilter() {
 *     public boolean isHandled(Method m) {
 *         // ignore finalize()
 *         return !m.getName().equals("finalize");
 *     }
 * });
 * Class c = f.createClass();
 * MethodHandler mi = new MethodHandler() {
 *     public Object invoke(Object self, Method m, Method proceed,
 *                          Object[] args) throws Throwable {
 *         System.out.println("Name: " + m.getName());
 *         return proceed.invoke(self, args);  // execute the original method.
 *     }
 * };
 * Foo foo = (Foo)c.newInstance();
 * ((Proxy)foo).setHandler(mi);
 * </pre>
 *
 * <p>Here, <code>Method</code> is <code>java.lang.reflect.Method</code>.</p>
 *
 * <p>Then, the following method call will be forwarded to MethodHandler
 * <code>mi</code> and prints a message before executing the originally called method
 * <code>bar()</code> in <code>Foo</code>.
 *
 * <pre>
 * foo.bar();
 * </pre>
 *
 * <p>The last three lines of the code shown above can be replaced with a call to
 * the helper method <code>create</code>, which generates a proxy class, instantiates
 * it, and sets the method handler of the instance:
 *
 * <pre>
 *     :
 * Foo foo = (Foo)f.create(new Class[0], new Object[0], mi);
 * </pre>
 *
 * <p>To change the method handler during runtime,
 * execute the following code:
 *
 * <pre>
 * MethodHandler mi = ... ;    // alternative handler
 * ((Proxy)foo).setHandler(mi);
 * </pre>
 *
 * <p> If setHandler is never called for a proxy instance then it will
 * employ the default handler which proceeds by invoking the original method.
 * The behaviour of the default handler is identical to the following
 * handler:
 *
 * <pre>
 * class EmptyHandler implements MethodHandler {
 *     public Object invoke(Object self, Method m,
 *                          Method proceed, Object[] args) throws Exception {
 *         return proceed.invoke(self, args);
 *     }
 * }
 * </pre>
 *
 * <p>A proxy factory caches and reuses proxy classes by default. It is possible to reset
 * this default globally by setting static field {@link ProxyFactory#useCache} to false.
 * Caching may also be configured for a specific factory by calling instance method
 * {@link ProxyFactory#setUseCache(boolean)}. It is strongly recommended that new clients
 * of class ProxyFactory enable caching. Failure to do so may lead to exhaustion of
 * the heap memory area used to store classes.
 *
 * <p>Caching is automatically disabled for any given proxy factory if deprecated instance
 * method {@link ProxyFactory#setHandler(MethodHandler)} is called. This method was
 * used to specify a default handler which newly created proxy classes should install
 * when they create their instances. It is only retained to provide backward compatibility
 * with previous releases of javassist. Unfortunately,this legacy behaviour makes caching
 * and reuse of proxy classes impossible. The current programming model expects javassist
 * clients to set the handler of a proxy instance explicitly by calling method
 * {@link Proxy#setHandler(MethodHandler)} as shown in the sample code above. New
 * clients are strongly recommended to use this model rather than calling
 * {@link ProxyFactory#setHandler(MethodHandler)}.
 *
 * <p>A proxy object generated by <code>ProxyFactory</code> is serializable
 * if its super class or any of its interfaces implement <code>java.io.Serializable</code>.
 * However, a serialized proxy object may not be compatible with future releases.
 * The serialization support should be used for short-term storage or RMI.
 *
 * <p>For compatibility with older releases serialization of proxy objects is implemented by
 * adding a writeReplace method to the proxy class. This allows a proxy to be serialized
 * to a conventional {@link java.io.ObjectOutputStream} and deserialized from a corresponding
 * {@link java.io.ObjectInputStream}. However this method suffers from several problems, the most
 * notable one being that it fails to serialize state inherited from the proxy's superclass.
 * <p>
 * An alternative method of serializing proxy objects is available which fixes these problems. It
 * requires inhibiting generation of the writeReplace method and instead using instances of
 * {@link javassist.util.proxy.ProxyObjectOutputStream} and {@link javassist.util.proxy.ProxyObjectInputStream}
 * (which are subclasses of {@link java.io.ObjectOutputStream} and  {@link java.io.ObjectInputStream})
 * to serialize and deserialize, respectively, the proxy. These streams recognise javassist proxies and ensure
 * that they are serialized and deserialized without the need for the proxy class to implement special methods
 * such as writeReplace. Generation of the writeReplace method can be disabled globally by setting static field
 * {@link ProxyFactory#useWriteReplace} to false. Alternatively, it may be
 * configured per factory by calling instance method {@link ProxyFactory#setUseWriteReplace(boolean)}.
 *
 * @see MethodHandler
 * @since 3.1
 * @author Muga Nishizawa
 * @author Shigeru Chiba
 * @author Andrew Dinn
 */
public class ProxyFactory {
    private Class<?> superClass;
    private Class<?>[] interfaces;
    private MethodFilter methodFilter;
    private MethodHandler handler;  // retained for legacy usage
    private List<Map.Entry<String,Method>> signatureMethods;
    private boolean hasGetHandler;
    private byte[] signature;
    private String classname;
    private String basename;
    private String superName;
    private Class<?> thisClass;
    private String genericSignature;

    /**
     * per factory setting initialised from current setting for useCache but able to be reset before each create call
     */
    private boolean factoryUseCache;
    /**
     * per factory setting initialised from current setting for useWriteReplace but able to be reset before each create call
     */
    private boolean factoryWriteReplace;

    /**
     * <p>If true, only public/protected methods are forwarded to a proxy object.
     * The class for that proxy object is loaded by the {@code defineClass} method
     * in {@code java.lang.invoke.MethodHandles.Lookup}, which is available in
     * Java 9 or later.  This works even when {@code sun.misc.Unsafe} is not
     * available for some reasons (it is already deprecated in Java 9).</p>
     *
     * <p>To load a class, Javassist first tries to use {@code sun.misc.Unsafe} and,
     * if not available, it uses a {@code protected} method in {@code java.lang.ClassLoader}
     * via {@code PrivilegedAction}.  Since the latter approach is not available
     * any longer by default in Java 9 or later, the JVM argument
     * {@code --add-opens java.base/java.lang=ALL-UNNAMED} must be given to the JVM
     * when it is used (because of lack of {@code sun.misc.Unsafe}).
     * If this argument cannot be given to the JVM, {@code onlyPublicMethods} should
     * be set to {@code true}.  Javassist will try to load by using
     * {@code java.lang.invoke.MethodHandles.Lookup}.</p>
     *
     * <p>The default value is {@code false}.</p>
     *
     * @see DefineClassHelper#toClass(String, Class, ClassLoader, ProtectionDomain, byte[])
     * @since 3.22
     */
    public static boolean onlyPublicMethods = false;

    /**
     * If the value of this variable is not null, the class file of
     * the generated proxy class is written under the directory specified
     * by this variable.  For example, if the value is 
     * <code>"."</code>, then the class file is written under the current
     * directory.  This method is for debugging.
     *
     * <p>The default value is null.
     */
    public String writeDirectory;

    private static final Class<?> OBJECT_TYPE = Object.class;

    private static final String HOLDER = "_methods_";
    private static final String HOLDER_TYPE = "[Ljava/lang/reflect/Method;";
    private static final String FILTER_SIGNATURE_FIELD = "_filter_signature";
    private static final String FILTER_SIGNATURE_TYPE = "[B";
    private static final String HANDLER = "handler";
    private static final String NULL_INTERCEPTOR_HOLDER = "javassist.util.proxy.RuntimeSupport";
    private static final String DEFAULT_INTERCEPTOR = "default_interceptor";
    private static final String HANDLER_TYPE
        = 'L' + MethodHandler.class.getName().replace('.', '/') + ';';
    private static final String HANDLER_SETTER = "setHandler";
    private static final String HANDLER_SETTER_TYPE = "(" + HANDLER_TYPE + ")V";

    private static final String HANDLER_GETTER = "getHandler";
    private static final String HANDLER_GETTER_TYPE = "()" + HANDLER_TYPE;

    private static final String SERIAL_VERSION_UID_FIELD = "serialVersionUID";
    private static final String SERIAL_VERSION_UID_TYPE = "J";
    private static final long SERIAL_VERSION_UID_VALUE = -1L;

    /**
     * If true, a generated proxy class is cached and it will be reused
     * when generating the proxy class with the same properties is requested.
     * The default value is true.
     *
     * Note that this value merely specifies the initial setting employed by any newly created
     * proxy factory. The factory setting may be overwritten by calling factory instance method
     * {@link #setUseCache(boolean)}
     *
     * @since 3.4
     */
    public static volatile boolean useCache = true;

    /**
     * If true, a generated proxy class will implement method writeReplace enabling
     * serialization of its proxies to a conventional ObjectOutputStream. this (default)
     * setting retains the old javassist behaviour which has the advantage that it
     * retains compatibility with older  releases and requires no extra work on the part
     * of the client performing the serialization. However, it has the disadvantage that
     * state inherited from the superclasses of the proxy is lost during serialization.
     * if false then serialization/deserialization of the proxy instances will preserve
     * all fields. However, serialization must be performed via a {@link ProxyObjectOutputStream}
     * and deserialization must be via {@link ProxyObjectInputStream}. Any attempt to serialize
     * proxies whose class was created with useWriteReplace set to false via a normal
     * {@link java.io.ObjectOutputStream} will fail.
     *
     * Note that this value merely specifies the initial setting employed by any newly created
     * proxy factory. The factory setting may be overwritten by calling factory instance method
     * {@link #setUseWriteReplace(boolean)}
     *
     * @since 3.4
     */
    public static volatile boolean useWriteReplace = true;

    /*
     * methods allowing individual factory settings for factoryUseCache and factoryWriteReplace to be reset
     */

    /**
     * test whether this factory uses the proxy cache
     * @return true if this factory uses the proxy cache otherwise false
     */
    public boolean isUseCache()
    {
        return factoryUseCache;
    }

    /**
     * configure whether this factory should use the proxy cache
     * @param useCache true if this factory should use the proxy cache and false if it should not use the cache
     * @throws RuntimeException if a default interceptor has been set for the factory
     */
    public void setUseCache(boolean useCache)
    {
        // we cannot allow caching to be used if the factory is configured to install a default interceptor
        // field into generated classes
        if (handler != null && useCache) {
            throw new RuntimeException("caching cannot be enabled if the factory default interceptor has been set");
        }
        factoryUseCache = useCache;
    }

    /**
     * test whether this factory installs a writeReplace method in created classes
     * @return true if this factory installs a writeReplace method in created classes otherwise false
     */
    public boolean isUseWriteReplace()
    {
        return factoryWriteReplace;
    }

    /**
     * configure whether this factory should add a writeReplace method to created classes
     * @param useWriteReplace true if this factory should add a writeReplace method to created classes and false if it
     * should not add a writeReplace method
     */
    public void setUseWriteReplace(boolean useWriteReplace)
    {
        factoryWriteReplace = useWriteReplace;
    }

    private static Map<ClassLoader,Map<String,ProxyDetails>> proxyCache =
            new WeakHashMap<ClassLoader,Map<String,ProxyDetails>>();

    /**
     * determine if a class is a javassist proxy class
     * @param cl
     * @return true if the class is a javassist proxy class otherwise false
     */
    public static boolean isProxyClass(Class<?> cl)
    {
        // all proxies implement Proxy or ProxyObject. nothing else should. 
        return (Proxy.class.isAssignableFrom(cl));
    }

    /**
     * used to store details of a specific proxy class in the second tier of the proxy cache. this entry
     * will be located in a hashmap keyed by the unique identifying name of the proxy class. the hashmap is
     * located in a weak hashmap keyed by the classloader common to all proxy classes in the second tier map.
     */
    static class ProxyDetails {
        /**
         * the unique signature of any method filter whose behaviour will be met by this class. each bit in
         * the byte array is set if the filter redirects the corresponding super or interface method and clear
         * if it does not redirect it.
         */
        byte[] signature;
        /**
         * a hexadecimal string representation of the signature bit sequence. this string also forms part
         * of the proxy class name.
         */
        Reference<Class<?>> proxyClass;
        /**
         * a flag which is true this class employs writeReplace to perform serialization of its instances
         * and false if serialization must employ of a ProxyObjectOutputStream and ProxyObjectInputStream
         */
        boolean isUseWriteReplace;

        ProxyDetails(byte[] signature, Class<?> proxyClass, boolean isUseWriteReplace)
        {
            this.signature = signature;
            this.proxyClass = new WeakReference<Class<?>>(proxyClass);
            this.isUseWriteReplace = isUseWriteReplace;
        }
    }

    /**
     * Constructs a factory of proxy class.
     */
    public ProxyFactory() {
        superClass = null;
        interfaces = null;
        methodFilter = null;
        handler = null;
        signature = null;
        signatureMethods = null;
        hasGetHandler = false;
        thisClass = null;
        genericSignature = null;
        writeDirectory = null;
        factoryUseCache = useCache;
        factoryWriteReplace = useWriteReplace;
    }

    /**
     * Sets the super class of a proxy class.
     */
    public void setSuperclass(Class<?> clazz) {
        superClass = clazz;
        // force recompute of signature
        signature = null;
    }

    /**
     * Obtains the super class set by <code>setSuperclass()</code>.
     *
     * @since 3.4
     */
    public Class<?> getSuperclass() { return superClass; }

    /**
     * Sets the interfaces of a proxy class.
     */
    public void setInterfaces(Class<?>[] ifs) {
        interfaces = ifs;
        // force recompute of signature
        signature = null;
    }

    /**
     * Obtains the interfaces set by <code>setInterfaces</code>.
     *
     * @since 3.4
     */
    public Class<?>[] getInterfaces() { return interfaces; }

    /**
     * Sets a filter that selects the methods that will be controlled by a handler.
     */
    public void setFilter(MethodFilter mf) {
        methodFilter = mf;
        // force recompute of signature
        signature = null;
    }

    /**
     * Sets the generic signature for the proxy class.
     * <p>For example,
     *
     * <pre>class NullPtr&lt;T&gt; {
     *     T get() { return null; }
     * }
     * </pre>
     *
     * <p>The following code makes a proxy for <code>NullPtr&lt;String&gt;</code>:
     *
     * <pre>ProxyFactory factory = new ProxyFactory();
     * factory.setSuperclass(NullPtr.class);
     * factory.setGenericSignature("LNullPtr&lt;Ljava/lang/String;&gt;;");
     * NullPtr&lt;?&gt; np = (NullPtr&lt;?&gt;)factory.create(null, null);
     *java.lang.reflect.Type[] x = ((java.lang.reflect.ParameterizedType)np.getClass().getGenericSuperclass())
     *                                                                     .getActualTypeArguments();
     * // x[0] == String.class
     * </pre>
     * 
     * @param sig   a generic signature.
     * @see javassist.CtClass#setGenericSignature(String)
     * @since 3.26
     */
    public void setGenericSignature(String sig) {
        genericSignature = sig;
    }

    /**
     * Generates a proxy class using the current filter.
     * The module or package where a proxy class is created
     * has to be opened to this package or the Javassist module.
     *
     * @see #createClass(Lookup)
     */
    public Class<?> createClass() {
        if (signature == null) {
            computeSignature(methodFilter);
        }
        return createClass1(null);
    }

    /**
     * Generates a proxy class using the supplied filter.
     * The module or package where a proxy class is created
     * has to be opened to this package or the Javassist module.
     */
    public Class<?> createClass(MethodFilter filter) {
        computeSignature(filter);
        return createClass1(null);
    }

    /**
     * Generates a proxy class with a specific signature.
     * access is package local so ProxyObjectInputStream can use this
     * @param signature
     */
    Class<?> createClass(byte[] signature)
    {
        installSignature(signature);
        return createClass1(null);
    }

    /**
     * Generates a proxy class using the current filter.
     * It loads a class file by the given
     * {@code java.lang.invoke.MethodHandles.Lookup} object,
     * which can be obtained by {@code MethodHandles.lookup()} called from
     * somewhere in the package that the loaded class belongs to.
     *
     * @param lookup    used for loading the proxy class.
     *                  It needs an appropriate right to invoke {@code defineClass}
     *                  for the proxy class.
     * @since 3.24
     */
    public Class<?> createClass(Lookup lookup) {
        if (signature == null) {
            computeSignature(methodFilter);
        }
        return createClass1(lookup);
    }

    /**
     * Generates a proxy class using the supplied filter.
     *
     * @param lookup    used for loading the proxy class.
     *                  It needs an appropriate right to invoke {@code defineClass}
     *                  for the proxy class.
     * @param filter    the filter.
     * @see #createClass(Lookup)
     * @since 3.24
     */
    public Class<?> createClass(Lookup lookup, MethodFilter filter) {
        computeSignature(filter);
        return createClass1(lookup);
    }

    /**
     * Generates a proxy class with a specific signature.
     * access is package local so ProxyObjectInputStream can use this.
     *
     * @param lookup    used for loading the proxy class.
     *                  It needs an appropriate right to invoke {@code defineClass}
     *                  for the proxy class.
     * @param signature         the signature.
     */
    Class<?> createClass(Lookup lookup, byte[] signature)
    {
        installSignature(signature);
        return createClass1(lookup);
    }

    private Class<?> createClass1(Lookup lookup) {
        Class<?> result = thisClass;
        if (result == null) {
            ClassLoader cl = getClassLoader();
            synchronized (proxyCache) {
                if (factoryUseCache)
                    createClass2(cl, lookup);
                else
                    createClass3(cl, lookup);

                result = thisClass;
                // don't retain any unwanted references
                thisClass = null;
            }
        }

        return result;
    }

    private static char[] hexDigits =
            { '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public String getKey(Class<?> superClass, Class<?>[] interfaces, byte[] signature, boolean useWriteReplace)
    {
        StringBuilder sbuf = new StringBuilder();
        if (superClass != null){
            sbuf.append(superClass.getName());
        }
        sbuf.append(':');
        for (int i = 0; i < interfaces.length; i++) {
            sbuf.append(interfaces[i].getName());
            sbuf.append(':');
        }
        for (int i = 0; i < signature.length; i++) {
            byte b = signature[i];
            int lo = b & 0xf;
            int hi = (b >> 4) & 0xf;
            sbuf.append(hexDigits[lo]);
            sbuf.append(hexDigits[hi]);
        }
        if (useWriteReplace) {
            sbuf.append(":w");
        }

        return sbuf.toString();
    }

    private void createClass2(ClassLoader cl, Lookup lookup) {
        String key = getKey(superClass, interfaces, signature, factoryWriteReplace);
        /*
         * Excessive concurrency causes a large memory footprint and slows the
         * execution speed down (with JDK 1.5).  Thus, we use a jumbo lock for
         * reducing concrrency.
         */
        // synchronized (proxyCache) {
            Map<String,ProxyDetails> cacheForTheLoader = proxyCache.get(cl);
            ProxyDetails details;
            if (cacheForTheLoader == null) {
                cacheForTheLoader = new HashMap<String,ProxyDetails>();
                proxyCache.put(cl, cacheForTheLoader);
            }
            details = cacheForTheLoader.get(key);
            if (details != null) {
                Reference<Class<?>> reference = details.proxyClass;
                thisClass = reference.get();
                if (thisClass != null) {
                    return;
                }
            }
            createClass3(cl, lookup);
            details = new  ProxyDetails(signature, thisClass, factoryWriteReplace);
            cacheForTheLoader.put(key, details);
        // }
    }

    private void createClass3(ClassLoader cl, Lookup lookup) {
        // we need a new class so we need a new class name
        allocateClassName();

        try {
            ClassFile cf = make();
            if (writeDirectory != null)
                FactoryHelper.writeFile(cf, writeDirectory);

            if (lookup == null)
                thisClass = FactoryHelper.toClass(cf, getClassInTheSamePackage(), cl, getDomain());
            else
                thisClass = FactoryHelper.toClass(cf, lookup);

            setField(FILTER_SIGNATURE_FIELD, signature);
            // legacy behaviour : we only set the default interceptor static field if we are not using the cache
            if (!factoryUseCache) {
                setField(DEFAULT_INTERCEPTOR, handler);
            }
        }
        catch (CannotCompileException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    /**
     * Obtains a class belonging to the same package that the created
     * proxy class belongs to.  It is used to obtain an appropriate
     * {@code java.lang.invoke.MethodHandles.Lookup}.
     */
    private Class<?> getClassInTheSamePackage() {
        if (basename.startsWith(packageForJavaBase))       // maybe the super class is java.*
            return this.getClass();
        else if (superClass != null && superClass != OBJECT_TYPE)
            return superClass;
        else if (interfaces != null && interfaces.length > 0)
            return interfaces[0];
        else
            return this.getClass();     // maybe wrong?
    }

    private void setField(String fieldName, Object value) {
        if (thisClass != null && value != null)
            try {
                Field f = thisClass.getField(fieldName);
                SecurityActions.setAccessible(f, true);
                f.set(null, value);
                SecurityActions.setAccessible(f, false);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    static byte[] getFilterSignature(Class<?> clazz) {
        return (byte[])getField(clazz, FILTER_SIGNATURE_FIELD);
    }

    private static Object getField(Class<?> clazz, String fieldName) {
        try {
            Field f = clazz.getField(fieldName);
            f.setAccessible(true);
            Object value = f.get(null);
            f.setAccessible(false);
            return value;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Obtains the method handler of the given proxy object.
     * 
     * @param p     a proxy object.
     * @return the method handler.
     * @since 3.16
     */
    public static MethodHandler getHandler(Proxy p) {
        try {
            Field f = p.getClass().getDeclaredField(HANDLER);
            f.setAccessible(true);
            Object value = f.get(p);
            f.setAccessible(false);
            return (MethodHandler)value;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A provider of class loaders.
     *
     * @see #classLoaderProvider
     * @since 3.4
     */
    public static interface ClassLoaderProvider {
        /**
         * Returns a class loader.
         *
         * @param pf    a proxy factory that is going to obtain a class loader.
         */
        public ClassLoader get(ProxyFactory pf);
    }

    /**
     * A provider used by <code>createClass()</code> for obtaining
     * a class loader.
     * <code>get()</code> on this <code>ClassLoaderProvider</code> object
     * is called to obtain a class loader.
     *
     * <p>The value of this field can be updated for changing the default
     * implementation.
     *
     * <p>Example:
     * <pre>
     * ProxyFactory.classLoaderProvider = new ProxyFactory.ClassLoaderProvider() {
     *     public ClassLoader get(ProxyFactory pf) {
     *         return Thread.currentThread().getContextClassLoader();
     *     }
     * };
     * </pre>
     *
     * @since 3.4
     */
    public static ClassLoaderProvider classLoaderProvider =
        new ClassLoaderProvider() {
            @Override
            public ClassLoader get(ProxyFactory pf) {
                  return pf.getClassLoader0();
              }
        };

    protected ClassLoader getClassLoader() {
        return classLoaderProvider.get(this);
    }

    protected ClassLoader getClassLoader0() {
        ClassLoader loader = null;
        if (superClass != null && !superClass.getName().equals("java.lang.Object"))
            loader = superClass.getClassLoader();
        else if (interfaces != null && interfaces.length > 0)
            loader = interfaces[0].getClassLoader();
 
        if (loader == null) {
            loader = getClass().getClassLoader();
            // In case javassist is in the endorsed dir
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
                if (loader == null)
                    loader = ClassLoader.getSystemClassLoader();
            }
        }

        return loader;
    }

    protected ProtectionDomain getDomain() {
        Class<?> clazz;
        if (superClass != null && !superClass.getName().equals("java.lang.Object"))
            clazz = superClass;
        else if (interfaces != null && interfaces.length > 0)
            clazz = interfaces[0];
        else
            clazz = this.getClass();

        return clazz.getProtectionDomain();
    }

    /**
     * Creates a proxy class and returns an instance of that class.
     *
     * @param paramTypes    parameter types for a constructor.
     * @param args          arguments passed to a constructor.
     * @param mh            the method handler for the proxy class.
     * @since 3.4
     */
    public Object create(Class<?>[] paramTypes, Object[] args, MethodHandler mh)
        throws NoSuchMethodException, IllegalArgumentException,
               InstantiationException, IllegalAccessException, InvocationTargetException
    {
        Object obj = create(paramTypes, args);
        ((Proxy)obj).setHandler(mh);
        return obj;
    }

    /**
     * Creates a proxy class and returns an instance of that class.
     *
     * @param paramTypes    parameter types for a constructor.
     * @param args          arguments passed to a constructor.
     */
    public Object create(Class<?>[] paramTypes, Object[] args)
        throws NoSuchMethodException, IllegalArgumentException,
               InstantiationException, IllegalAccessException, InvocationTargetException
    {
        Class<?> c = createClass();
        Constructor<?> cons = c.getConstructor(paramTypes);
        return cons.newInstance(args);
    }

    /**
     * Sets the default invocation handler.  This invocation handler is shared
     * among all the instances of a proxy class unless another is explicitly
     * specified.
     * @deprecated since 3.12
     * use of this method is incompatible  with proxy class caching.
     * instead clients should call method {@link Proxy#setHandler(MethodHandler)} to set the handler
     * for each newly created  proxy instance.
     * calling this method will automatically disable caching of classes created by the proxy factory.
     */
    @Deprecated
    public void setHandler(MethodHandler mi) {
        // if we were using the cache and the handler is non-null then we must stop caching
        if (factoryUseCache && mi != null)  {
            factoryUseCache = false;
            // clear any currently held class so we don't try to reuse it or set its handler field
          thisClass  = null;
        }
        handler = mi;
        // this retains the behaviour of the old code which resets any class we were holding on to
        // this is probably not what is wanted
        setField(DEFAULT_INTERCEPTOR, handler);
    }

    /**
     * A unique class name generator.
     */
    public static interface UniqueName {
        /**
         * Returns a unique class name.
         *
         * @param classname     the super class name of the proxy class.
         */
        String get(String classname);
    }

    /**
     * A unique class name generator.
     * Replacing this generator changes the algorithm to generate a
     * unique name. The <code>get</code> method does not have to be
     * a <code>synchronized</code> method since the access to this field
     * is mutually exclusive and thus thread safe.
     */
    public static UniqueName nameGenerator = new UniqueName() {
        private final String sep = "_$$_jvst" + Integer.toHexString(this.hashCode() & 0xfff) + "_";
        private int counter = 0;

        @Override
        public String get(String classname) {
            return classname + sep + Integer.toHexString(counter++);
        }
    };

    private static String makeProxyName(String classname) {
        synchronized (nameGenerator) {
            return nameGenerator.get(classname);
        }
    }

    private ClassFile make() throws CannotCompileException {
        ClassFile cf = new ClassFile(false, classname, superName);
        cf.setAccessFlags(AccessFlag.PUBLIC);
        setInterfaces(cf, interfaces, hasGetHandler ? Proxy.class : ProxyObject.class);
        ConstPool pool = cf.getConstPool();

        // legacy: we only add the static field for the default interceptor if caching is disabled
        if  (!factoryUseCache) {
            FieldInfo finfo = new FieldInfo(pool, DEFAULT_INTERCEPTOR, HANDLER_TYPE);
            finfo.setAccessFlags(AccessFlag.PUBLIC | AccessFlag.STATIC);
            cf.addField(finfo);
        }

        // handler is per instance
        FieldInfo finfo2 = new FieldInfo(pool, HANDLER, HANDLER_TYPE);
        finfo2.setAccessFlags(AccessFlag.PRIVATE);
        cf.addField(finfo2);

        // filter signature is per class
        FieldInfo finfo3 = new FieldInfo(pool, FILTER_SIGNATURE_FIELD, FILTER_SIGNATURE_TYPE);
        finfo3.setAccessFlags(AccessFlag.PUBLIC | AccessFlag.STATIC);
        cf.addField(finfo3);

        // the proxy class serial uid must always be a fixed value
        FieldInfo finfo4 = new FieldInfo(pool, SERIAL_VERSION_UID_FIELD, SERIAL_VERSION_UID_TYPE);
        finfo4.setAccessFlags(AccessFlag.PUBLIC | AccessFlag.STATIC| AccessFlag.FINAL);
        cf.addField(finfo4);

        if (genericSignature != null) {
            SignatureAttribute sa = new SignatureAttribute(pool, genericSignature);
            cf.addAttribute(sa);
        }

        // HashMap allMethods = getMethods(superClass, interfaces);
        // int size = allMethods.size();
        makeConstructors(classname, cf, pool, classname);

        List<Find2MethodsArgs> forwarders = new ArrayList<Find2MethodsArgs>();
        int s = overrideMethods(cf, pool, classname, forwarders);
        addClassInitializer(cf, pool, classname, s, forwarders);
        addSetter(classname, cf, pool);
        if (!hasGetHandler)
            addGetter(classname, cf, pool);

        if (factoryWriteReplace) {
            try {
                cf.addMethod(makeWriteReplace(pool));
            }
            catch (DuplicateMemberException e) {
                // writeReplace() is already declared in the super class/interfaces.
            }
        }

        thisClass = null;
        return cf;
    }

    private void checkClassAndSuperName() {
        if (interfaces == null)
            interfaces = new Class[0];

        if (superClass == null) {
            superClass = OBJECT_TYPE;
            superName = superClass.getName();
            basename = interfaces.length == 0 ? superName
                                              : interfaces[0].getName();
        } else {
            superName = superClass.getName();
            basename = superName;
        }

        if (Modifier.isFinal(superClass.getModifiers()))
            throw new RuntimeException(superName + " is final");

        // Since java.base module is not opened, its proxy class should be
        // in a different (open) module.  Otherwise, it could not be created
        // by reflection.
        if (basename.startsWith("java.") || basename.startsWith("jdk.") || onlyPublicMethods)
            basename = packageForJavaBase + basename.replace('.', '_');
    }

    private static final String packageForJavaBase = "javassist.util.proxy.";

    private void allocateClassName() {
        classname = makeProxyName(basename);
    }

    private static Comparator<Map.Entry<String,Method>> sorter =
        new Comparator<Map.Entry<String,Method>>() {
            @Override
            public int compare(Map.Entry<String,Method> e1, Map.Entry<String,Method> e2) {
                return e1.getKey().compareTo(e2.getKey());
            }
        };

    private void makeSortedMethodList() {
        checkClassAndSuperName();

        hasGetHandler = false;      // getMethods() may set this to true.
        Map<String,Method> allMethods = getMethods(superClass, interfaces);
        signatureMethods = new ArrayList<Map.Entry<String,Method>>(allMethods.entrySet());
        Collections.sort(signatureMethods, sorter);
    }

    private void computeSignature(MethodFilter filter) // throws CannotCompileException
    {
        makeSortedMethodList();

        int l = signatureMethods.size();
        int maxBytes = ((l + 7) >> 3);
        signature = new byte[maxBytes];
        for (int idx = 0; idx < l; idx++)
        {
            Method m = signatureMethods.get(idx).getValue();
            int mod = m.getModifiers();
            if (!Modifier.isFinal(mod) && !Modifier.isStatic(mod)
                    && isVisible(mod, basename, m) && (filter == null || filter.isHandled(m))) {
                setBit(signature, idx);
            }
        }
    }

    private void installSignature(byte[] signature) // throws CannotCompileException
    {
        makeSortedMethodList();

        int l = signatureMethods.size();
        int maxBytes = ((l + 7) >> 3);
        if (signature.length != maxBytes) {
            throw new RuntimeException("invalid filter signature length for deserialized proxy class");
        }

        this.signature =  signature;
    }

    private boolean testBit(byte[] signature, int idx) {
        int byteIdx = idx >> 3;
        if (byteIdx > signature.length)
            return false;
        int bitIdx = idx & 0x7;
        int mask = 0x1 << bitIdx;
        int sigByte = signature[byteIdx];
        return ((sigByte & mask) != 0);
    }

    private void setBit(byte[] signature, int idx) {
        int byteIdx = idx >> 3;
        if (byteIdx < signature.length) {
            int bitIdx = idx & 0x7;
            int mask = 0x1 << bitIdx;
            int sigByte = signature[byteIdx];
            signature[byteIdx] = (byte)(sigByte | mask);
        }
    }

    private static void setInterfaces(ClassFile cf, Class<?>[] interfaces, Class<?> proxyClass) {
        String setterIntf = proxyClass.getName();
        String[] list;
        if (interfaces == null || interfaces.length == 0)
            list = new String[] { setterIntf };
        else {
            list = new String[interfaces.length + 1];
            for (int i = 0; i < interfaces.length; i++)
                list[i] = interfaces[i].getName();

            list[interfaces.length] = setterIntf;
        }

        cf.setInterfaces(list);
    }

    private static void addClassInitializer(ClassFile cf, ConstPool cp,
                String classname, int size, List<Find2MethodsArgs> forwarders)
        throws CannotCompileException
    {
        FieldInfo finfo = new FieldInfo(cp, HOLDER, HOLDER_TYPE);
        finfo.setAccessFlags(AccessFlag.PRIVATE | AccessFlag.STATIC);
        cf.addField(finfo);
        MethodInfo minfo = new MethodInfo(cp, "<clinit>", "()V");
        minfo.setAccessFlags(AccessFlag.STATIC);
        setThrows(minfo, cp, new Class<?>[] { ClassNotFoundException.class });

        Bytecode code = new Bytecode(cp, 0, 2);
        code.addIconst(size * 2);
        code.addAnewarray("java.lang.reflect.Method");
        final int varArray = 0;
        code.addAstore(varArray);

        // forName() must be called here.  Otherwise, the class might be
        // invisible.
        code.addLdc(classname);
        code.addInvokestatic("java.lang.Class",
                "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
        final int varClass = 1;
        code.addAstore(varClass);

        for (Find2MethodsArgs args:forwarders)
            callFind2Methods(code, args.methodName, args.delegatorName,
                             args.origIndex, args.descriptor, varClass, varArray);

        code.addAload(varArray);
        code.addPutstatic(classname, HOLDER, HOLDER_TYPE);

        code.addLconst(SERIAL_VERSION_UID_VALUE);
        code.addPutstatic(classname, SERIAL_VERSION_UID_FIELD, SERIAL_VERSION_UID_TYPE);
        code.addOpcode(Bytecode.RETURN);
        minfo.setCodeAttribute(code.toCodeAttribute());
        cf.addMethod(minfo);
    }

    /**
     * @param thisMethod        might be null.
     */
    private static void callFind2Methods(Bytecode code, String superMethod, String thisMethod,
                                         int index, String desc, int classVar, int arrayVar) {
        String findClass = RuntimeSupport.class.getName();
        String findDesc
            = "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;[Ljava/lang/reflect/Method;)V";

        code.addAload(classVar);
        code.addLdc(superMethod);
        if (thisMethod == null)
            code.addOpcode(Opcode.ACONST_NULL);
        else
            code.addLdc(thisMethod);

        code.addIconst(index);
        code.addLdc(desc);
        code.addAload(arrayVar);
        code.addInvokestatic(findClass, "find2Methods", findDesc);
    }

    private static void addSetter(String classname, ClassFile cf, ConstPool cp)
        throws CannotCompileException
    {
        MethodInfo minfo = new MethodInfo(cp, HANDLER_SETTER,
                                          HANDLER_SETTER_TYPE);
        minfo.setAccessFlags(AccessFlag.PUBLIC);
        Bytecode code = new Bytecode(cp, 2, 2);
        code.addAload(0);
        code.addAload(1);
        code.addPutfield(classname, HANDLER, HANDLER_TYPE);
        code.addOpcode(Bytecode.RETURN);
        minfo.setCodeAttribute(code.toCodeAttribute());
        cf.addMethod(minfo);
    }

    private static void addGetter(String classname, ClassFile cf, ConstPool cp)
        throws CannotCompileException
    {
        MethodInfo minfo = new MethodInfo(cp, HANDLER_GETTER,
                                          HANDLER_GETTER_TYPE);
        minfo.setAccessFlags(AccessFlag.PUBLIC);
        Bytecode code = new Bytecode(cp, 1, 1);
        code.addAload(0);
        code.addGetfield(classname, HANDLER, HANDLER_TYPE);
        code.addOpcode(Bytecode.ARETURN);
        minfo.setCodeAttribute(code.toCodeAttribute());
        cf.addMethod(minfo);
    }

    private int overrideMethods(ClassFile cf, ConstPool cp, String className, List<Find2MethodsArgs> forwarders)
        throws CannotCompileException
    {
        String prefix = makeUniqueName("_d", signatureMethods);
        Iterator<Map.Entry<String,Method>> it = signatureMethods.iterator();
        int index = 0;
        while (it.hasNext()) {
            Map.Entry<String,Method> e = it.next();
            if (ClassFile.MAJOR_VERSION < ClassFile.JAVA_5 || !isBridge(e.getValue()))
            	if (testBit(signature, index)) {
            		override(className, e.getValue(), prefix, index,
            				 keyToDesc(e.getKey(), e.getValue()), cf, cp, forwarders);
            	}

            index++;
        }

        return index;
    }

    private static boolean isBridge(Method m) {
    	return m.isBridge();
    }

    private void override(String thisClassname, Method meth, String prefix,
                          int index, String desc, ClassFile cf, ConstPool cp,
                          List<Find2MethodsArgs> forwarders)
        throws CannotCompileException
    {
        Class<?> declClass = meth.getDeclaringClass();
        String delegatorName = prefix + index + meth.getName();
        if (Modifier.isAbstract(meth.getModifiers()))
            delegatorName = null;
        else {
            MethodInfo delegator
                = makeDelegator(meth, desc, cp, declClass, delegatorName);
            // delegator is not a bridge method.  See Sec. 15.12.4.5 of JLS 3rd Ed.
            delegator.setAccessFlags(delegator.getAccessFlags() & ~AccessFlag.BRIDGE);
            cf.addMethod(delegator);
        }

        MethodInfo forwarder
            = makeForwarder(thisClassname, meth, desc, cp, declClass,
                            delegatorName, index, forwarders);
        cf.addMethod(forwarder);
    }

    private void makeConstructors(String thisClassName, ClassFile cf,
            ConstPool cp, String classname) throws CannotCompileException
    {
        Constructor<?>[] cons = SecurityActions.getDeclaredConstructors(superClass);
        // legacy: if we are not caching then we need to initialise the default handler
        boolean doHandlerInit = !factoryUseCache;
        for (int i = 0; i < cons.length; i++) {
            Constructor<?> c = cons[i];
            int mod = c.getModifiers();
            if (!Modifier.isFinal(mod) && !Modifier.isPrivate(mod)
                    && isVisible(mod, basename, c)) {
                MethodInfo m = makeConstructor(thisClassName, c, cp, superClass, doHandlerInit);
                cf.addMethod(m);
            }
        }
    }

    private static String makeUniqueName(String name, List<Map.Entry<String,Method>> sortedMethods) {
        if (makeUniqueName0(name, sortedMethods.iterator()))
            return name;

        for (int i = 100; i < 999; i++) {
            String s = name + i;
            if (makeUniqueName0(s, sortedMethods.iterator()))
                return s;
        }

        throw new RuntimeException("cannot make a unique method name");
    }

    private static boolean makeUniqueName0(String name, Iterator<Map.Entry<String,Method>> it) {
        while (it.hasNext())
            if (it.next().getKey().startsWith(name))
                return false;
        return true;
    }

    /**
     * Returns true if the method is visible from the package.
     *
     * @param mod       the modifiers of the method. 
     */
    private static boolean isVisible(int mod, String from, Member meth) {
        if ((mod & Modifier.PRIVATE) != 0)
            return false;
        else if ((mod & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0)
            return true;
        else {
            String p = getPackageName(from);
            String q = getPackageName(meth.getDeclaringClass().getName());
            if (p == null)
                return q == null;
            return p.equals(q);
        }
    }

    private static String getPackageName(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0)
            return null;
        return name.substring(0, i);
    }

    /* getMethods() may set hasGetHandler to true.
     */
    private Map<String,Method> getMethods(Class<?> superClass, Class<?>[] interfaceTypes) {
        Map<String,Method> hash = new HashMap<String,Method>();
        Set<Class<?>> set = new HashSet<Class<?>>();
        for (int i = 0; i < interfaceTypes.length; i++)
            getMethods(hash, interfaceTypes[i], set);

        getMethods(hash, superClass, set);
        return hash;
    }

    private void getMethods(Map<String,Method> hash, Class<?> clazz, Set<Class<?>> visitedClasses) {
        // This both speeds up scanning by avoiding duplicate interfaces and is needed to
        // ensure that superinterfaces are always scanned before subinterfaces.
        if (!visitedClasses.add(clazz))
            return;

        Class<?>[] ifs = clazz.getInterfaces();
        for (int i = 0; i < ifs.length; i++)
            getMethods(hash, ifs[i], visitedClasses);

        Class<?> parent = clazz.getSuperclass();
        if (parent != null)
            getMethods(hash, parent, visitedClasses);

        /* Java 5 or later allows covariant return types.
         * It also allows contra-variant parameter types
         * if a super class is a generics with concrete type arguments
         * such as Foo<String>.  So the method-overriding rule is complex.
         */
        Method[] methods = SecurityActions.getDeclaredMethods(clazz);
        for (int i = 0; i < methods.length; i++)
            if (!Modifier.isPrivate(methods[i].getModifiers())) {
                Method m = methods[i];
                String key = m.getName() + ':' + RuntimeSupport.makeDescriptor(m);  // see keyToDesc().
                if (key.startsWith(HANDLER_GETTER_KEY))
                    hasGetHandler = true;

                // JIRA JASSIST-85
                // put the method to the cache, retrieve previous definition (if any)
                Method oldMethod = hash.put(key, m);

                // JIRA JASSIST-244, 267
                // ignore a bridge method to a method declared in a non-public class.
                if (null != oldMethod && isBridge(m)
                    && !Modifier.isPublic(oldMethod.getDeclaringClass().getModifiers())
                    && !Modifier.isAbstract(oldMethod.getModifiers()) && !isDuplicated(i, methods))
                    hash.put(key, oldMethod);

                // check if visibility has been reduced 
                if (null != oldMethod && Modifier.isPublic(oldMethod.getModifiers())
                                      && !Modifier.isPublic(m.getModifiers())) { 
                    // we tried to overwrite a public definition with a non-public definition,
                    // use the old definition instead. 
                    hash.put(key, oldMethod); 
                }
            }
    }

    private static boolean isDuplicated(int index, Method[] methods) {
        String name = methods[index].getName();
        for (int i = 0; i < methods.length; i++)
            if (i != index)
                if (name.equals(methods[i].getName()) && areParametersSame(methods[index], methods[i]))
                    return true;

        return false;
    }
    
    private static boolean areParametersSame(Method method, Method targetMethod) {
        Class<?>[] methodTypes = method.getParameterTypes();
        Class<?>[] targetMethodTypes = targetMethod.getParameterTypes();
        if (methodTypes.length == targetMethodTypes.length) {
            for (int i = 0; i< methodTypes.length; i++) {
                if (methodTypes[i].getName().equals(targetMethodTypes[i].getName())) {
                    continue;
                } else {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    private static final String HANDLER_GETTER_KEY
    	= HANDLER_GETTER + ":()";

    private static String keyToDesc(String key, Method m) {
        return key.substring(key.indexOf(':') + 1);
    }

    private static MethodInfo makeConstructor(String thisClassName, Constructor<?> cons,
                                              ConstPool cp, Class<?> superClass, boolean doHandlerInit) {
        String desc = RuntimeSupport.makeDescriptor(cons.getParameterTypes(),
                                                    Void.TYPE);
        MethodInfo minfo = new MethodInfo(cp, "<init>", desc);
        minfo.setAccessFlags(Modifier.PUBLIC);      // cons.getModifiers() & ~Modifier.NATIVE
        setThrows(minfo, cp, cons.getExceptionTypes());
        Bytecode code = new Bytecode(cp, 0, 0);

        // legacy: if we are not using caching then we initialise the instance's handler
        // from the class's static default interceptor and skip the next few instructions if
        // it is non-null
        if (doHandlerInit) {
            code.addAload(0);
            code.addGetstatic(thisClassName, DEFAULT_INTERCEPTOR, HANDLER_TYPE);
            code.addPutfield(thisClassName, HANDLER, HANDLER_TYPE);
            code.addGetstatic(thisClassName, DEFAULT_INTERCEPTOR, HANDLER_TYPE);
            code.addOpcode(Opcode.IFNONNULL);
            code.addIndex(10);
        }
        // if caching is enabled then we don't have a handler to initialise so this else branch will install
        // the handler located in the static field of class RuntimeSupport.
        code.addAload(0);
        code.addGetstatic(NULL_INTERCEPTOR_HOLDER, DEFAULT_INTERCEPTOR, HANDLER_TYPE);
        code.addPutfield(thisClassName, HANDLER, HANDLER_TYPE);
        int pc = code.currentPc();

        code.addAload(0);
        int s = addLoadParameters(code, cons.getParameterTypes(), 1);
        code.addInvokespecial(superClass.getName(), "<init>", desc);
        code.addOpcode(Opcode.RETURN);
        code.setMaxLocals(s + 1);
        CodeAttribute ca = code.toCodeAttribute();
        minfo.setCodeAttribute(ca);

        StackMapTable.Writer writer = new StackMapTable.Writer(32);
        writer.sameFrame(pc);
        ca.setAttribute(writer.toStackMapTable(cp));
        return minfo;
    }

    private MethodInfo makeDelegator(Method meth, String desc,
                ConstPool cp, Class<?> declClass, String delegatorName) {
        MethodInfo delegator = new MethodInfo(cp, delegatorName, desc);
        delegator.setAccessFlags(Modifier.FINAL | Modifier.PUBLIC
                | (meth.getModifiers() & ~(Modifier.PRIVATE
                                           | Modifier.PROTECTED
                                           | Modifier.ABSTRACT
                                           | Modifier.NATIVE
                                           | Modifier.SYNCHRONIZED)));
        setThrows(delegator, cp, meth);
        Bytecode code = new Bytecode(cp, 0, 0);
        code.addAload(0);
        int s = addLoadParameters(code, meth.getParameterTypes(), 1);
        Class<?> targetClass = invokespecialTarget(declClass);
        code.addInvokespecial(targetClass.isInterface(), cp.addClassInfo(targetClass.getName()),
                              meth.getName(), desc);
        addReturn(code, meth.getReturnType());
        code.setMaxLocals(++s);
        delegator.setCodeAttribute(code.toCodeAttribute());
        return delegator;
    }

    /* Suppose that the receiver type is S, the invoked method
     * is declared in T, and U is the immediate super class of S
     * (or its interface).  If S <: U <: T (S <: T reads "S extends T"), 
     * the target type of invokespecial has to be not T but U.
     */
    private Class<?> invokespecialTarget(Class<?> declClass) {
        if (declClass.isInterface())
            for (Class<?> i: interfaces)
                if (declClass.isAssignableFrom(i))
                    return i;

        return superClass;
    }

    /**
     * @param delegatorName     null if the original method is abstract.
     */
    private static MethodInfo makeForwarder(String thisClassName,
                    Method meth, String desc, ConstPool cp,
                    Class<?> declClass, String delegatorName, int index,
                    List<Find2MethodsArgs> forwarders) {
        MethodInfo forwarder = new MethodInfo(cp, meth.getName(), desc);
        forwarder.setAccessFlags(Modifier.FINAL
                    | (meth.getModifiers() & ~(Modifier.ABSTRACT
                                               | Modifier.NATIVE
                                               | Modifier.SYNCHRONIZED)));
        setThrows(forwarder, cp, meth);
        int args = Descriptor.paramSize(desc);
        Bytecode code = new Bytecode(cp, 0, args + 2);
        /*
         * static {
         *   methods[index * 2]
         *     = RuntimeSupport.findSuperMethod(this, <overridden name>, <desc>);
         *   methods[index * 2 + 1]
         *     = RuntimeSupport.findMethod(this, <delegator name>, <desc>);
         *     or = null // the original method is abstract.
         * }
         *     :
         * return ($r)handler.invoke(this, methods[index * 2],
         *                methods[index * 2 + 1], $args);
         */
        int origIndex = index * 2;
        int delIndex = index * 2 + 1;
        int arrayVar = args + 1;
        code.addGetstatic(thisClassName, HOLDER, HOLDER_TYPE);
        code.addAstore(arrayVar);

        forwarders.add(new Find2MethodsArgs(meth.getName(), delegatorName, desc, origIndex));

        code.addAload(0);
        code.addGetfield(thisClassName, HANDLER, HANDLER_TYPE);
        code.addAload(0);

        code.addAload(arrayVar);
        code.addIconst(origIndex);
        code.addOpcode(Opcode.AALOAD);

        code.addAload(arrayVar);
        code.addIconst(delIndex);
        code.addOpcode(Opcode.AALOAD);

        makeParameterList(code, meth.getParameterTypes());
        code.addInvokeinterface(MethodHandler.class.getName(), "invoke",
            "(Ljava/lang/Object;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
            5);
        Class<?> retType = meth.getReturnType();
        addUnwrapper(code, retType);
        addReturn(code, retType);

        CodeAttribute ca = code.toCodeAttribute();
        forwarder.setCodeAttribute(ca);
        return forwarder;
    }

    static class Find2MethodsArgs {
        String methodName, delegatorName, descriptor;
        int origIndex;

        Find2MethodsArgs(String mname, String dname, String desc, int index) {
            methodName = mname;
            delegatorName = dname;
            descriptor = desc;
            origIndex = index;
        }
    }

    private static void setThrows(MethodInfo minfo, ConstPool cp, Method orig) {
        Class<?>[] exceptions = orig.getExceptionTypes();
        setThrows(minfo, cp, exceptions);
    }

    private static void setThrows(MethodInfo minfo, ConstPool cp,
                                  Class<?>[] exceptions) {
        if (exceptions.length == 0)
            return;

        String[] list = new String[exceptions.length];
        for (int i = 0; i < exceptions.length; i++)
            list[i] = exceptions[i].getName();

        ExceptionsAttribute ea = new ExceptionsAttribute(cp);
        ea.setExceptions(list);
        minfo.setExceptionsAttribute(ea);
    }

    private static int addLoadParameters(Bytecode code, Class<?>[] params,
                                         int offset) {
        int stacksize = 0;
        int n = params.length;
        for (int i = 0; i < n; ++i)
            stacksize += addLoad(code, stacksize + offset, params[i]);

        return stacksize;
    }

    private static int addLoad(Bytecode code, int n, Class<?> type) {
        if (type.isPrimitive()) {
            if (type == Long.TYPE) {
                code.addLload(n);
                return 2;
            }
            else if (type == Float.TYPE)
                code.addFload(n);
            else if (type == Double.TYPE) {
                code.addDload(n);
                return 2;
            }
            else
                code.addIload(n);
        }
        else
            code.addAload(n);

        return 1;
    }

    private static int addReturn(Bytecode code, Class<?> type) {
        if (type.isPrimitive()) {
            if (type == Long.TYPE) {
                code.addOpcode(Opcode.LRETURN);
                return 2;
            }
            else if (type == Float.TYPE)
                code.addOpcode(Opcode.FRETURN);
            else if (type == Double.TYPE) {
                code.addOpcode(Opcode.DRETURN);
                return 2;
            }
            else if (type == Void.TYPE) {
                code.addOpcode(Opcode.RETURN);
                return 0;
            }
            else
                code.addOpcode(Opcode.IRETURN);
        }
        else
            code.addOpcode(Opcode.ARETURN);

        return 1;
    }

    private static void makeParameterList(Bytecode code, Class<?>[] params) {
        int regno = 1;
        int n = params.length;
        code.addIconst(n);
        code.addAnewarray("java/lang/Object");
        for (int i = 0; i < n; i++) {
            code.addOpcode(Opcode.DUP);
            code.addIconst(i);
            Class<?> type = params[i];
            if (type.isPrimitive())
                regno = makeWrapper(code, type, regno);
            else {
                code.addAload(regno);
                regno++;
            }

            code.addOpcode(Opcode.AASTORE);
        }
    }

    private static int makeWrapper(Bytecode code, Class<?> type, int regno) {
        int index = FactoryHelper.typeIndex(type);
        String wrapper = FactoryHelper.wrapperTypes[index]; 
        code.addNew(wrapper);
        code.addOpcode(Opcode.DUP);
        addLoad(code, regno, type);
        code.addInvokespecial(wrapper, "<init>",
                              FactoryHelper.wrapperDesc[index]);
        return regno + FactoryHelper.dataSize[index];
    }

    private static void addUnwrapper(Bytecode code, Class<?> type) {
        if (type.isPrimitive()) {
            if (type == Void.TYPE)
                code.addOpcode(Opcode.POP);
            else {
                int index = FactoryHelper.typeIndex(type);
                String wrapper = FactoryHelper.wrapperTypes[index];
                code.addCheckcast(wrapper);
                code.addInvokevirtual(wrapper,
                                      FactoryHelper.unwarpMethods[index],
                                      FactoryHelper.unwrapDesc[index]);
            }
        }
        else       
            code.addCheckcast(type.getName());
    }

    private static MethodInfo makeWriteReplace(ConstPool cp) {
        MethodInfo minfo = new MethodInfo(cp, "writeReplace", "()Ljava/lang/Object;");
        String[] list = new String[1];
        list[0] = "java.io.ObjectStreamException";
        ExceptionsAttribute ea = new ExceptionsAttribute(cp);
        ea.setExceptions(list);
        minfo.setExceptionsAttribute(ea);
        Bytecode code = new Bytecode(cp, 0, 1);
        code.addAload(0);
        code.addInvokestatic("javassist.util.proxy.RuntimeSupport",
                             "makeSerializedProxy",
                             "(Ljava/lang/Object;)Ljavassist/util/proxy/SerializedProxy;");
        code.addOpcode(Opcode.ARETURN);
        minfo.setCodeAttribute(code.toCodeAttribute());
        return minfo;
    }
}
