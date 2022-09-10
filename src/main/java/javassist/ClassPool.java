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

package javassist;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javassist.bytecode.ClassFile;
import javassist.bytecode.Descriptor;
import javassist.util.proxy.DefinePackageHelper;

/**
 * A container of <code>CtClass</code> objects.
 * A <code>CtClass</code> object must be obtained from this object.
 * If <code>get()</code> is called on this object,
 * it searches various sources represented by <code>ClassPath</code>
 * to find a class file and then it creates a <code>CtClass</code> object
 * representing that class file.  The created object is returned to the
 * caller.
 *
 * <p><b>Memory consumption memo:</b>
 *
 * <p><code>ClassPool</code> objects hold all the <code>CtClass</code>es
 * that have been created so that the consistency among modified classes
 * can be guaranteed.  Thus if a large number of <code>CtClass</code>es
 * are processed, the <code>ClassPool</code> will consume a huge amount
 * of memory.  To avoid this, a <code>ClassPool</code> object
 * should be recreated, for example, every hundred classes processed.
 * Note that <code>getDefault()</code> is a singleton factory.
 * Otherwise, <code>detach()</code> in <code>CtClass</code> should be used
 * to avoid huge memory consumption.
 *
 * <p><b><code>ClassPool</code> hierarchy:</b>
 *
 * <p><code>ClassPool</code>s can make a parent-child hierarchy as
 * <code>java.lang.ClassLoader</code>s.  If a <code>ClassPool</code> has
 * a parent pool, <code>get()</code> first asks the parent pool to find
 * a class file.  Only if the parent could not find the class file,
 * <code>get()</code> searches the <code>ClassPath</code>s of
 * the child <code>ClassPool</code>.  This search order is reversed if
 * <code>ClassPath.childFirstLookup</code> is <code>true</code>.
 *
 * @see javassist.CtClass
 * @see javassist.ClassPath
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ClassPool {

    /**
     * Determines the search order.
     *
     * <p>If this field is true, <code>get()</code> first searches the
     * class path associated to this <code>ClassPool</code> and then
     * the class path associated with the parent <code>ClassPool</code>.
     * Otherwise, the class path associated with the parent is searched
     * first.
     *
     * <p>The default value is false.
     */
    public boolean childFirstLookup = false;

    /**
     * Turning the automatic pruning on/off.
     *
     * <p>If this field is true, <code>CtClass</code> objects are
     * automatically pruned by default when <code>toBytecode()</code> etc.
     * are called.  The automatic pruning can be turned on/off individually
     * for each <code>CtClass</code> object.
     *
     * <p>The initial value is false.
     *
     * @see CtClass#prune()
     * @see CtClass#stopPruning(boolean)
     * @see CtClass#detach()
     */
    public static boolean doPruning = false;

    private int compressCount;
    private static final int COMPRESS_THRESHOLD = 100;

    /* releaseUnmodifiedClassFile was introduced for avoiding a bug
       of JBoss AOP.  So the value should be true except for JBoss AOP.
     */

    /**
     * If true, unmodified and not-recently-used class files are
     * periodically released for saving memory.
     *
     * <p>The initial value is true.
     */
    public static boolean releaseUnmodifiedClassFile = true;

    /**
     * If true, the contents of a jar file are cached after the jar
     * file is opened.
     *
     * <p>The initial value is true.
     */
    public static boolean cacheOpenedJarFile = true;    // see ClassPoolTail.JarClassPath#openClassfile(String)

    protected ClassPoolTail source;
    protected ClassPool parent;
    protected Hashtable classes;        // should be synchronous

    /**
     * Table of registered cflow variables.
     */
    private Hashtable cflow = null;     // should be synchronous.

    private static final int INIT_HASH_SIZE = 191;

    private ArrayList importedPackages;

    /**
     * Creates a root class pool.  No parent class pool is specified.
     */
    public ClassPool() {
        this(null);
    }

    /**
     * Creates a root class pool.  If <code>useDefaultPath</code> is
     * true, <code>appendSystemPath()</code> is called.  Otherwise,
     * this constructor is equivalent to the constructor taking no
     * parameter.
     *
     * @param useDefaultPath    true if the system search path is
     *                          appended.
     */
    public ClassPool(boolean useDefaultPath) {
        this(null);
        if (useDefaultPath)
            appendSystemPath();
    }

    /**
     * Creates a class pool.
     *
     * @param parent    the parent of this class pool.  If this is a root
     *                  class pool, this parameter must be <code>null</code>.
     * @see javassist.ClassPool#getDefault()
     */
    public ClassPool(ClassPool parent) {
        this.classes = new Hashtable(INIT_HASH_SIZE);
        this.source = new ClassPoolTail();
        this.parent = parent;
        if (parent == null) {
            CtClass[] pt = CtClass.primitiveTypes;
            for (int i = 0; i < pt.length; ++i)
                classes.put(pt[i].getName(), pt[i]);
        }

        this.cflow = null;
        this.compressCount = 0;
        clearImportedPackages();
    }

    /**
     * Returns the default class pool.
     * The returned object is always identical since this method is
     * a singleton factory.
     *
     * <p>The default class pool searches the system search path,
     * which usually includes the platform library, extension
     * libraries, and the search path specified by the
     * <code>-classpath</code> option or the <code>CLASSPATH</code>
     * environment variable.
     *
     * <p>When this method is called for the first time, the default
     * class pool is created with the following code snippet:
     *
     * <pre>ClassPool cp = new ClassPool();
     * cp.appendSystemPath();
     * </pre>
     *
     * <p>If the default class pool cannot find any class files,
     * try <code>ClassClassPath</code>, <code>ModuleClassPath</code>,
     * or <code>LoaderClassPath</code>.
     *
     * @see ClassClassPath
     * @see LoaderClassPath
     */
    public static synchronized ClassPool getDefault() {
        if (defaultPool == null) {
            defaultPool = new ClassPool(null);
            defaultPool.appendSystemPath();
        }

        return defaultPool;
    }

    private static ClassPool defaultPool = null;

    /**
     * Provide a hook so that subclasses can do their own
     * caching of classes.
     *
     * @see #cacheCtClass(String,CtClass,boolean)
     * @see #removeCached(String)
     */
    protected CtClass getCached(String classname) {
        return (CtClass)classes.get(classname);
    }

    /**
     * Provides a hook so that subclasses can do their own
     * caching of classes.
     *
     * @see #getCached(String)
     * @see #removeCached(String)
     */
    protected void cacheCtClass(String classname, CtClass c, boolean dynamic) {
        classes.put(classname, c);
    }

    /**
     * Provide a hook so that subclasses can do their own
     * caching of classes.
     *
     * @see #getCached(String)
     * @see #cacheCtClass(String,CtClass,boolean)
     */
    protected CtClass removeCached(String classname) {
        return (CtClass)classes.remove(classname);
    }

    /**
     * Returns the class search path.
     */
    public String toString() {
        return source.toString();
    }

    /**
     * This method is periodically invoked so that memory
     * footprint will be minimized.
     */
    void compress() {
        if (compressCount++ > COMPRESS_THRESHOLD) {
            compressCount = 0;
            Enumeration e = classes.elements();
            while (e.hasMoreElements())
                ((CtClass)e.nextElement()).compress();
        }
    }

    /**
     * Record a package name so that the Javassist compiler searches
     * the package to resolve a class name.
     * Don't record the <code>java.lang</code> package, which has
     * been implicitly recorded by default.
     *
     * <p>Since version 3.14, <code>packageName</code> can be a
     * fully-qualified class name.
     *
     * <p>Note that <code>get()</code> in <code>ClassPool</code> does
     * not search the recorded package.  Only the compiler searches it.
     *
     * @param packageName       the package name.
     *         It must not include the last '.' (dot).
     *         For example, "java.util" is valid but "java.util." is wrong.
     * @since 3.1
     */
    public void importPackage(String packageName) {
        importedPackages.add(packageName);
    }

    /**
     * Clear all the package names recorded by <code>importPackage()</code>.
     * The <code>java.lang</code> package is not removed.
     *
     * @see #importPackage(String)
     * @since 3.1
     */
    public void clearImportedPackages() {
        importedPackages = new ArrayList();
        importedPackages.add("java.lang");
    }

    /**
     * Returns all the package names recorded by <code>importPackage()</code>. 
     *
     * @see #importPackage(String)
     * @since 3.1
     */
    public Iterator<String> getImportedPackages() {
        return importedPackages.iterator();
    }

    /**
     * Records a class name that never exists.
     * For example, a package name can be recorded by this method.
     * This would improve execution performance
     * since <code>get()</code> quickly throw an exception
     * without searching the class path at all
     * if the given name is an invalid name recorded by this method.
     * Note that searching the class path takes relatively long time.
     *
     * <p>The current implementation of this method performs nothing.
     *
     * @param name          an invalid class name (separeted by dots).
     * @deprecated
     */
    public void recordInvalidClassName(String name) {
        // source.recordInvalidClassName(name);
    }

    /**
     * Records the <code>$cflow</code> variable for the field specified
     * by <code>cname</code> and <code>fname</code>.
     *
     * @param name      variable name
     * @param cname     class name
     * @param fname     field name
     */
    void recordCflow(String name, String cname, String fname) {
        if (cflow == null)
            cflow = new Hashtable();

        cflow.put(name, new Object[] { cname, fname });
    }

    /**
     * Undocumented method.  Do not use; internal-use only.
     *
     * @param name      the name of <code>$cflow</code> variable
     */
    public Object[] lookupCflow(String name) {
        if (cflow == null)
            cflow = new Hashtable();

        return (Object[])cflow.get(name);
    }

    /**
     * Reads a class file and constructs a <code>CtClass</code>
     * object with a new name.
     * This method is useful if you want to generate a new class as a copy
     * of another class (except the class name).  For example,
     *
     * <pre>
     * getAndRename("Point", "Pair")
     * </pre>
     *
     * returns a <code>CtClass</code> object representing <code>Pair</code>
     * class.  The definition of <code>Pair</code> is the same as that of
     * <code>Point</code> class except the class name since <code>Pair</code>
     * is defined by reading <code>Point.class</code>.
     *
     * @param orgName   the original (fully-qualified) class name
     * @param newName   the new class name
     */
    public CtClass getAndRename(String orgName, String newName)
        throws NotFoundException
    {
        CtClass clazz = get0(orgName, false);
        if (clazz == null)
            throw new NotFoundException(orgName);

        if (clazz instanceof CtClassType)
            ((CtClassType)clazz).setClassPool(this);

        clazz.setName(newName);         // indirectly calls
                                        // classNameChanged() in this class
        return clazz;
    }

    /*
     * This method is invoked by CtClassType.setName().  It removes a
     * CtClass object from the hash table and inserts it with the new
     * name.  Don't delegate to the parent.
     */
    synchronized void classNameChanged(String oldname, CtClass clazz) {
        CtClass c = (CtClass)getCached(oldname);
        if (c == clazz)             // must check this equation.
            removeCached(oldname);  // see getAndRename().

        String newName = clazz.getName();
        checkNotFrozen(newName);
        cacheCtClass(newName, clazz, false);
    }

    /**
     * Reads a class file from the source and returns a reference
     * to the <code>CtClass</code>
     * object representing that class file.  If that class file has been
     * already read, this method returns a reference to the
     * <code>CtClass</code> created when that class file was read at the
     * first time.
     *
     * <p>If <code>classname</code> ends with "[]", then this method
     * returns a <code>CtClass</code> object for that array type.
     *
     * <p>To obtain an inner class, use "$" instead of "." for separating
     * the enclosing class name and the inner class name.
     *
     * @param classname         a fully-qualified class name.
     */
    public CtClass get(String classname) throws NotFoundException {
        CtClass clazz;
        if (classname == null)
            clazz = null;
        else
            clazz = get0(classname, true);

        if (clazz == null)
            throw new NotFoundException(classname);
        else {
            clazz.incGetCounter();
            return clazz;
        }
    }

    /**
     * Reads a class file from the source and returns a reference
     * to the <code>CtClass</code>
     * object representing that class file.
     * This method is equivalent to <code>get</code> except
     * that it returns <code>null</code> when a class file is
     * not found and it never throws an exception.
     *
     * @param classname     a fully-qualified class name.
     * @return a <code>CtClass</code> object or <code>null</code>.
     * @see #get(String)
     * @see #find(String)
     * @since 3.13
     */
    public CtClass getOrNull(String classname) {
        CtClass clazz = null;
        if (classname == null)
            clazz = null;
        else
            try {
                /* ClassPool.get0() never throws an exception
                   but its subclass may implement get0 that
                   may throw an exception.
                */
                clazz = get0(classname, true);
            }
            catch (NotFoundException e){}

        if (clazz != null)
            clazz.incGetCounter();

        return clazz;
    }

    /**
     * Returns a <code>CtClass</code> object with the given name.
     * This is almost equivalent to <code>get(String)</code> except
     * that classname can be an array-type "descriptor" (an encoded
     * type name) such as <code>[Ljava/lang/Object;</code>.
     *
     * <p>Using this method is not recommended; this method should be 
     * used only to obtain the <code>CtClass</code> object
     * with a name returned from <code>getClassInfo</code> in
     * <code>javassist.bytecode.ClassPool</code>.  <code>getClassInfo</code>
     * returns a fully-qualified class name but, if the class is an array
     * type, it returns a descriptor.
     *
     * @param classname         a fully-qualified class name or a descriptor
     *                          representing an array type.
     * @see #get(String)
     * @see javassist.bytecode.ConstPool#getClassInfo(int)
     * @see javassist.bytecode.Descriptor#toCtClass(String, ClassPool)
     * @since 3.8.1
     */
    public CtClass getCtClass(String classname) throws NotFoundException {
        if (classname.charAt(0) == '[')
            return Descriptor.toCtClass(classname, this);
        else
            return get(classname);
    }

    /**
     * @param useCache      false if the cached CtClass must be ignored.
     * @return null     if the class could not be found.
     */
    protected synchronized CtClass get0(String classname, boolean useCache)
        throws NotFoundException
    {
        CtClass clazz = null;
        if (useCache) {
            clazz = getCached(classname);
            if (clazz != null)
                return clazz;
        }

        if (!childFirstLookup && parent != null) {
            clazz = parent.get0(classname, useCache);
            if (clazz != null)
                return clazz;
        }

        clazz = createCtClass(classname, useCache);
        if (clazz != null) {
            // clazz.getName() != classname if classname is "[L<name>;".
            if (useCache)
                cacheCtClass(clazz.getName(), clazz, false);

            return clazz;
        }

        if (childFirstLookup && parent != null)
            clazz = parent.get0(classname, useCache);

        return clazz;
    }

    /**
     * Creates a CtClass object representing the specified class.
     * It first examines whether or not the corresponding class
     * file exists.  If yes, it creates a CtClass object.
     *
     * @return null if the class file could not be found.
     */
    protected CtClass createCtClass(String classname, boolean useCache) {
        // accept "[L<class name>;" as a class name. 
        if (classname.charAt(0) == '[')
            classname = Descriptor.toClassName(classname);

        if (classname.endsWith("[]")) {
            String base = classname.substring(0, classname.indexOf('['));
            if ((!useCache || getCached(base) == null) && find(base) == null)
                return null;
            else
                return new CtArray(classname, this);
        }
        else
            if (find(classname) == null)
                return null;
            else
                return new CtClassType(classname, this);
    }

    /**
     * Searches the class path to obtain the URL of the class file
     * specified by classname.  It is also used to determine whether
     * the class file exists.
     *
     * @param classname     a fully-qualified class name.
     * @return null if the class file could not be found.
     * @see CtClass#getURL()
     */
    public URL find(String classname) {
        return source.find(classname);
    }

    /*
     * Is invoked by CtClassType.setName() and methods in this class.
     * This method throws an exception if the class is already frozen or
     * if this class pool cannot edit the class since it is in a parent
     * class pool.
     *
     * @see checkNotExists(String)
     */
    void checkNotFrozen(String classname) throws RuntimeException {
        CtClass clazz = getCached(classname);
        if (clazz == null) {
            if (!childFirstLookup && parent != null) {
                try {
                    clazz = parent.get0(classname, true);
                }
                catch (NotFoundException e) {}
                if (clazz != null)
                    throw new RuntimeException(classname
                            + " is in a parent ClassPool.  Use the parent.");
            }
        }
        else
            if (clazz.isFrozen())
                throw new RuntimeException(classname
                                        + ": frozen class (cannot edit)");
    }

    /*
     * This method returns null if this or its parent class pool does
     * not contain a CtClass object with the class name.
     *
     * @see checkNotFrozen(String)
     */
    CtClass checkNotExists(String classname) {
        CtClass clazz = getCached(classname);
        if (clazz == null)
            if (!childFirstLookup && parent != null) {
                try {
                    clazz = parent.get0(classname, true);
                }
                catch (NotFoundException e) {}
            }

        return clazz;
    }

    /* for CtClassType.getClassFile2().  Don't delegate to the parent.
     */
    InputStream openClassfile(String classname) throws NotFoundException {
        return source.openClassfile(classname);
    }

    void writeClassfile(String classname, OutputStream out)
        throws NotFoundException, IOException, CannotCompileException
    {
        source.writeClassfile(classname, out);
    }

    /**
     * Reads class files from the source and returns an array of
     * <code>CtClass</code>
     * objects representing those class files.
     *
     * <p>If an element of <code>classnames</code> ends with "[]",
     * then this method
     * returns a <code>CtClass</code> object for that array type.
     *
     * @param classnames        an array of fully-qualified class name.
     */
    public CtClass[] get(String[] classnames) throws NotFoundException {
        if (classnames == null)
            return new CtClass[0];

        int num = classnames.length;
        CtClass[] result = new CtClass[num];
        for (int i = 0; i < num; ++i)
            result[i] = get(classnames[i]);

        return result;
    }

    /**
     * Reads a class file and obtains a compile-time method.
     *
     * @param classname         the class name
     * @param methodname        the method name
     * @see CtClass#getDeclaredMethod(String)
     */
    public CtMethod getMethod(String classname, String methodname)
        throws NotFoundException
    {
        CtClass c = get(classname);
        return c.getDeclaredMethod(methodname);
    }

    /**
     * Creates a new class (or interface) from the given class file.
     * If there already exists a class with the same name, the new class
     * overwrites that previous class.
     *
     * <p>This method is used for creating a <code>CtClass</code> object
     * directly from a class file.  The qualified class name is obtained
     * from the class file; you do not have to explicitly give the name.
     *
     * @param classfile class file.
     * @throws RuntimeException if there is a frozen class with the
     *                          the same name.
     * @see #makeClassIfNew(InputStream)
     * @see javassist.ByteArrayClassPath
     */
    public CtClass makeClass(InputStream classfile)
        throws IOException, RuntimeException
    {
        return makeClass(classfile, true);
    }

    /**
     * Creates a new class (or interface) from the given class file.
     * If there already exists a class with the same name, the new class
     * overwrites that previous class.
     *
     * <p>This method is used for creating a <code>CtClass</code> object
     * directly from a class file.  The qualified class name is obtained
     * from the class file; you do not have to explicitly give the name.
     *
     * @param classfile class file.
     * @param ifNotFrozen       throws a RuntimeException if this parameter is true
     *                          and there is a frozen class with the same name.
     * @see javassist.ByteArrayClassPath
     */
    public CtClass makeClass(InputStream classfile, boolean ifNotFrozen)
        throws IOException, RuntimeException
    {
        compress();
        classfile = new BufferedInputStream(classfile);
        CtClass clazz = new CtClassType(classfile, this);
        clazz.checkModify();
        String classname = clazz.getName();
        if (ifNotFrozen)
            checkNotFrozen(classname);

        cacheCtClass(classname, clazz, true);
        return clazz;
    }

    /**
     * Creates a new class (or interface) from the given class file.
     * If there already exists a class with the same name, the new class
     * overwrites that previous class.
     *
     * <p>This method is used for creating a <code>CtClass</code> object
     * directly from a class file.  The qualified class name is obtained
     * from the class file; you do not have to explicitly give the name.
     *
     * @param classfile         class file.
     * @throws RuntimeException if there is a frozen class with the
     *                          the same name.
     * @since 3.20
     */
    public CtClass makeClass(ClassFile classfile)
        throws RuntimeException
    {
        return makeClass(classfile, true);
    }

    /**
     * Creates a new class (or interface) from the given class file.
     * If there already exists a class with the same name, the new class
     * overwrites that previous class.
     *
     * <p>This method is used for creating a <code>CtClass</code> object
     * directly from a class file.  The qualified class name is obtained
     * from the class file; you do not have to explicitly give the name.
     *
     * @param classfile     class file.
     * @param ifNotFrozen       throws a RuntimeException if this parameter is true
     *                          and there is a frozen class with the same name.
     * @since 3.20
     */
    public CtClass makeClass(ClassFile classfile, boolean ifNotFrozen)
        throws RuntimeException
    {
        compress();
        CtClass clazz = new CtClassType(classfile, this);
        clazz.checkModify();
        String classname = clazz.getName();
        if (ifNotFrozen)
            checkNotFrozen(classname);

        cacheCtClass(classname, clazz, true);
        return clazz;
    }

    /**
     * Creates a new class (or interface) from the given class file.
     * If there already exists a class with the same name, this method
     * returns the existing class; a new class is never created from
     * the given class file.
     *
     * <p>This method is used for creating a <code>CtClass</code> object
     * directly from a class file.  The qualified class name is obtained
     * from the class file; you do not have to explicitly give the name.
     *
     * @param classfile             the class file.
     * @see #makeClass(InputStream)
     * @see javassist.ByteArrayClassPath
     * @since 3.9
     */
    public CtClass makeClassIfNew(InputStream classfile)
        throws IOException, RuntimeException
    {
        compress();
        classfile = new BufferedInputStream(classfile);
        CtClass clazz = new CtClassType(classfile, this);
        clazz.checkModify();
        String classname = clazz.getName();
        CtClass found = checkNotExists(classname);
        if (found != null)
            return found;
        else {
            cacheCtClass(classname, clazz, true);
            return clazz;
        }
    }

    /**
     * Creates a new public class.
     * If there already exists a class with the same name, the new class
     * overwrites that previous class.
     *
     * <p>If no constructor is explicitly added to the created new
     * class, Javassist generates constructors and adds it when
     * the class file is generated.  It generates a new constructor
     * for each constructor of the super class.  The new constructor
     * takes the same set of parameters and invokes the
     * corresponding constructor of the super class.  All the received
     * parameters are passed to it.
     *
     * @param classname                 a fully-qualified class name.
     * @throws RuntimeException         if the existing class is frozen.
     */
    public CtClass makeClass(String classname) throws RuntimeException {
        return makeClass(classname, null);
    }

    /**
     * Creates a new public class.
     * If there already exists a class/interface with the same name,
     * the new class overwrites that previous class.
     *
     * <p>If no constructor is explicitly added to the created new
     * class, Javassist generates constructors and adds it when
     * the class file is generated.  It generates a new constructor
     * for each constructor of the super class.  The new constructor
     * takes the same set of parameters and invokes the
     * corresponding constructor of the super class.  All the received
     * parameters are passed to it.
     *
     * @param classname  a fully-qualified class name.
     * @param superclass the super class.
     * @throws RuntimeException if the existing class is frozen.
     */
    public synchronized CtClass makeClass(String classname, CtClass superclass)
        throws RuntimeException
    {
        checkNotFrozen(classname);
        CtClass clazz = new CtNewClass(classname, this, false, superclass);
        cacheCtClass(classname, clazz, true);
        return clazz;
    }

    /**
     * Creates a new public nested class.
     * This method is called by {@link CtClassType#makeNestedClass()}.
     *
     * @param classname     a fully-qualified class name.
     * @return      the nested class.
     */
    synchronized CtClass makeNestedClass(String classname) {
        checkNotFrozen(classname);
        CtClass clazz = new CtNewClass(classname, this, false, null);
        cacheCtClass(classname, clazz, true);
        return clazz;
    }

    /**
     * Creates a new public interface.
     * If there already exists a class/interface with the same name,
     * the new interface overwrites that previous one.
     *
     * @param name          a fully-qualified interface name.
     * @throws RuntimeException if the existing interface is frozen.
     */
    public CtClass makeInterface(String name) throws RuntimeException {
        return makeInterface(name, null);
    }

    /**
     * Creates a new public interface.
     * If there already exists a class/interface with the same name,
     * the new interface overwrites that previous one.
     *
     * @param name       a fully-qualified interface name.
     * @param superclass the super interface.
     * @throws RuntimeException if the existing interface is frozen.
     */
    public synchronized CtClass makeInterface(String name, CtClass superclass)
        throws RuntimeException
    {
        checkNotFrozen(name);
        CtClass clazz = new CtNewClass(name, this, true, superclass);
        cacheCtClass(name, clazz, true);
        return clazz;
    }

    /**
     * Creates a new annotation.
     * If there already exists a class/interface with the same name,
     * the new interface overwrites that previous one.
     *
     * @param name      a fully-qualified interface name.
     *                  Or null if the annotation has no super interface. 
     * @throws RuntimeException if the existing interface is frozen.
     * @since 3.19
     */
    public CtClass makeAnnotation(String name) throws RuntimeException {
        try {
            CtClass cc = makeInterface(name, get("java.lang.annotation.Annotation"));
            cc.setModifiers(cc.getModifiers() | Modifier.ANNOTATION);
            return cc;
        }
        catch (NotFoundException e) {
            // should never happen.
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Appends the system search path to the end of the
     * search path.  The system search path
     * usually includes the platform library, extension
     * libraries, and the search path specified by the
     * <code>-classpath</code> option or the <code>CLASSPATH</code>
     * environment variable.
     *
     * @return the appended class path.
     */
    public ClassPath appendSystemPath() {
        return source.appendSystemPath();
    }

    /**
     * Insert a <code>ClassPath</code> object at the head of the
     * search path.
     *
     * @return the inserted class path.
     * @see javassist.ClassPath
     * @see javassist.URLClassPath
     * @see javassist.ByteArrayClassPath
     */
    public ClassPath insertClassPath(ClassPath cp) {
        return source.insertClassPath(cp);
    }

    /**
     * Appends a <code>ClassPath</code> object to the end of the
     * search path.
     *
     * @return the appended class path.
     * @see javassist.ClassPath
     * @see javassist.URLClassPath
     * @see javassist.ByteArrayClassPath
     */
    public ClassPath appendClassPath(ClassPath cp) {
        return source.appendClassPath(cp);
    }

    /**
     * Inserts a directory or a jar (or zip) file at the head of the
     * search path.
     *
     * @param pathname      the path name of the directory or jar file.
     *                      It must not end with a path separator ("/").
     *                      If the path name ends with "/*", then all the
     *                      jar files matching the path name are inserted.
     *
     * @return the inserted class path.
     * @throws NotFoundException    if the jar file is not found.
     */
    public ClassPath insertClassPath(String pathname)
        throws NotFoundException
    {
        return source.insertClassPath(pathname);
    }

    /**
     * Appends a directory or a jar (or zip) file to the end of the
     * search path.
     *
     * @param pathname the path name of the directory or jar file.
     *                 It must not end with a path separator ("/").
     *                      If the path name ends with "/*", then all the
     *                      jar files matching the path name are appended.
     *
     * @return the appended class path.
     * @throws NotFoundException if the jar file is not found.
     */
    public ClassPath appendClassPath(String pathname)
        throws NotFoundException
    {
        return source.appendClassPath(pathname);
    }

    /**
     * Detatches the <code>ClassPath</code> object from the search path.
     * The detached <code>ClassPath</code> object cannot be added
     * to the path again.
     */
    public void removeClassPath(ClassPath cp) {
        source.removeClassPath(cp);
    }

    /**
     * Appends directories and jar files for search.
     *
     * <p>The elements of the given path list must be separated by colons
     * in Unix or semi-colons in Windows.
     *
     * @param pathlist      a (semi)colon-separated list of
     *                      the path names of directories and jar files.
     *                      The directory name must not end with a path
     *                      separator ("/").
     * @throws NotFoundException if a jar file is not found.
     */
    public void appendPathList(String pathlist) throws NotFoundException {
        char sep = File.pathSeparatorChar;
        int i = 0;
        for (;;) {
            int j = pathlist.indexOf(sep, i);
            if (j < 0) {
                appendClassPath(pathlist.substring(i));
                break;
            }
            else {
                appendClassPath(pathlist.substring(i, j));
                i = j + 1;
            }
        }
    }

    /**
     * Converts the given class to a <code>java.lang.Class</code> object.
     * Once this method is called, further modifications are not
     * allowed any more.
     * To load the class, this method uses the context class loader
     * of the current thread.  It is obtained by calling
     * <code>getClassLoader()</code>.  
     * 
     * <p>This behavior can be changed by subclassing the pool and changing
     * the <code>getClassLoader()</code> method.
     * If the program is running on some application
     * server, the context class loader might be inappropriate to load the
     * class.</p>
     *
     * <p>This method is provided for convenience.  If you need more
     * complex functionality, you should write your own class loader.
     *
     * <p><b>Warining:</b>
     * This method should not be used in Java 11 or later.
     * Use {@link #toClass(CtClass,Class)}.
     * </p>
     *
     * <p><b>Warining:</b>
     * A Class object returned by this method may not
     * work with a security manager or a signed jar file because a
     * protection domain is not specified.</p>
     *
     * @see #toClass(CtClass,Class)
     * @see #toClass(CtClass,Class,java.lang.ClassLoader,ProtectionDomain)
     * @see #getClassLoader()
     */
    public Class toClass(CtClass clazz) throws CannotCompileException {
        // Some subclasses of ClassPool may override toClass(CtClass,ClassLoader).
        // So we should call that method instead of toClass(.., ProtectionDomain).
        return toClass(clazz, getClassLoader()); 
    }

    /**
     * Get the classloader for <code>toClass()</code>, <code>getAnnotations()</code> in
     * <code>CtClass</code>, etc.
     * 
     * <p>The default is the context class loader.
     * 
     * @return the classloader for the pool
     * @see #toClass(CtClass)
     * @see CtClass#getAnnotations()
     */
    public ClassLoader getClassLoader() {
        return getContextClassLoader();
    }
    
    /**
     * Obtains a class loader that seems appropriate to look up a class
     * by name. 
     */
    static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Converts the class to a <code>java.lang.Class</code> object.
     * Do not override this method any more at a subclass because
     * {@link #toClass(CtClass)} will never calls this method.
     *
     * <p><b>Warining:</b> A Class object returned by this method may not
     * work with a security manager or a signed jar file because a
     * protection domain is not specified.
     *
     * @deprecated      Replaced by {@link #toClass(CtClass,Class,ClassLoader,ProtectionDomain)}.
     * A subclass of <code>ClassPool</code> that has been
     * overriding this method should be modified.  It should override
     * {@link #toClass(CtClass,Class,ClassLoader,ProtectionDomain)}.
     */
    public Class toClass(CtClass ct, ClassLoader loader)
        throws CannotCompileException
    {
        return toClass(ct, null, loader, null);
    }

    /**
     * Converts the class to a <code>java.lang.Class</code> object.
     * Once this method is called, further modifications are not allowed
     * any more.
     *
     * <p>The class file represented by the given <code>CtClass</code> is
     * loaded by the given class loader to construct a
     * <code>java.lang.Class</code> object.  Since a private method
     * on the class loader is invoked through the reflection API,
     * the caller must have permissions to do that.</p>
     *
     * <p>An easy way to obtain <code>ProtectionDomain</code> object is
     * to call <code>getProtectionDomain()</code>
     * in <code>java.lang.Class</code>.  It returns the domain that the
     * class belongs to.
     *
     * <p>This method is provided for convenience.  If you need more
     * complex functionality, you should write your own class loader.</p>
     *
     * @param ct            the class converted into {@code java.lang.Class}.
     * @param loader        the class loader used to load this class.
     *                      For example, the loader returned by
     *                      <code>getClassLoader()</code> can be used
     *                      for this parameter.
     * @param domain        the protection domain for the class.
     *                      If it is null, the default domain created
     *                      by <code>java.lang.ClassLoader</code> is used.
     *
     * @see #getClassLoader()
     * @since 3.3
     * @deprecated      Replaced by {@link #toClass(CtClass,Class,ClassLoader,ProtectionDomain)}.
     */
    public Class toClass(CtClass ct, ClassLoader loader, ProtectionDomain domain)
        throws CannotCompileException
    {
        return toClass(ct, null, loader, domain);
    }

    /**
     * Converts the class to a <code>java.lang.Class</code> object.
     * Once this method is called, further modifications are not allowed
     * any more.
     *
     * <p>This method is available in Java 9 or later.
     * It loads the class
     * by using {@code java.lang.invoke.MethodHandles} with {@code neighbor}.
     * </p>
     *
     * @param ct            the class converted into {@code java.lang.Class}.
     * @param neighbor      a class belonging to the same package that
     *                      the converted class belongs to.
     * @since 3.24
     */
    public Class<?> toClass(CtClass ct, Class<?> neighbor)
        throws CannotCompileException
    {
        try {
            return javassist.util.proxy.DefineClassHelper.toClass(neighbor,
                                                            ct.toBytecode());
        }
        catch (IOException e) {
            throw new CannotCompileException(e);
        }
    }

    /**
     * Converts the class to a <code>java.lang.Class</code> object.
     * Once this method is called, further modifications are not allowed
     * any more.
     *
     * <p>This method is available in Java 9 or later.
     * It loads the class
     * by using the given {@code java.lang.invoke.MethodHandles.Lookup}.
     * </p>
     *
     * @param ct            the class converted into {@code java.lang.Class}.
     * @since 3.24
     */
    public Class<?> toClass(CtClass ct,
                            java.lang.invoke.MethodHandles.Lookup lookup)
        throws CannotCompileException
    {
        try {
            return javassist.util.proxy.DefineClassHelper.toClass(lookup,
                                                            ct.toBytecode());
        }
        catch (IOException e) {
            throw new CannotCompileException(e);
        }
    }

    /**
     * Converts the class to a <code>java.lang.Class</code> object.
     * Once this method is called, further modifications are not allowed
     * any more.
     *
     * <p>When the JVM is Java 11 or later, this method loads the class
     * by using {@code java.lang.invoke.MethodHandles} with {@code neighbor}.
     * The other arguments {@code loader} and {@code domain} are not used;
     * so they can be null.
     * </p>
     *
     * <p>Otherwise, or when {@code neighbor} is null,
     * the class file represented by the given <code>CtClass</code> is
     * loaded by the given class loader to construct a
     * <code>java.lang.Class</code> object.  Since a private method
     * on the class loader is invoked through the reflection API,
     * the caller must have permissions to do that.
     *
     * <p>An easy way to obtain <code>ProtectionDomain</code> object is
     * to call <code>getProtectionDomain()</code>
     * in <code>java.lang.Class</code>.  It returns the domain that the
     * class belongs to.
     *
     * <p>If your program is for only Java 9 or later, don't use this method.
     * Use {@link #toClass(CtClass,Class)} or
     * {@link #toClass(CtClass,java.lang.invoke.MethodHandles.Lookup)}.
     * </p>
     *
     * @param ct            the class converted into {@code java.lang.Class}.
     * @param neighbor      a class belonging to the same package that
     *                      the converted class belongs to.
     *                      It can be null.
     * @param loader        the class loader used to load this class.
     *                      For example, the loader returned by
     *                      <code>getClassLoader()</code> can be used
     *                      for this parameter.
     * @param domain        the protection domain for the class.
     *                      If it is null, the default domain created
     *                      by <code>java.lang.ClassLoader</code> is used.
     *
     * @see #getClassLoader()
     * @since 3.24
     */
    public Class toClass(CtClass ct, Class<?> neighbor, ClassLoader loader,
                         ProtectionDomain domain)
            throws CannotCompileException
    {
        try {
            return javassist.util.proxy.DefineClassHelper.toClass(ct.getName(),
                    neighbor, loader, domain, ct.toBytecode());
        }
        catch (IOException e) {
            throw new CannotCompileException(e);
        }
    }

    /**
     * Defines a new package.  If the package is already defined, this method
     * performs nothing.
     *
     * <p>You do not necessarily need to
     * call this method.  If this method is called, then  
     * <code>getPackage()</code> on the <code>Class</code> object returned 
     * by <code>toClass()</code> will return a non-null object.</p>
     *
     * <p>The jigsaw module introduced by Java 9 has broken this method.</p>
     *
     * @param loader        the class loader passed to <code>toClass()</code> or
     *                      the default one obtained by <code>getClassLoader()</code>.
     * @param name          the package name.
     * @see #getClassLoader()
     * @see #toClass(CtClass)
     * @see CtClass#toClass()
     * @since 3.16
     * @deprecated
     */
    public void makePackage(ClassLoader loader, String name)
        throws CannotCompileException
    {
        DefinePackageHelper.definePackage(name, loader);
    }

}
