/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2004 Shigeru Chiba. All Rights Reserved.
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

package javassist;

import java.io.*;
import java.net.URL;
import java.util.Hashtable;

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

    protected ClassPoolTail source;
    protected ClassPool parent;
    protected Hashtable classes;        // should be synchronous

    /**
     * Table of registered cflow variables.
     */
    private Hashtable cflow = null;     // should be synchronous.

    /**
     * Creates a root class pool.  No parent class pool is specified.
     *
     */
    public ClassPool() {
        this(null);
    }

    /**
     * Creates a class pool.
     *
     * @param parent    the parent of this class pool.  If this is a root
     *                  class pool, this parameter must be <code>null</code>.
     * @see javassist.ClassPool#getDefault()
     */
    public ClassPool(ClassPool parent) {
        this.classes = new Hashtable();
        this.source = new ClassPoolTail();
        this.parent = parent;
        if (parent == null) {
            CtClass[] pt = CtClass.primitiveTypes;
            for (int i = 0; i < pt.length; ++i)
                classes.put(pt[i].getName(), pt[i]);
        }

        this.cflow = null;
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
     * <ul><code>ClassPool cp = new ClassPool();
     * cp.appendSystemPath();
     * </code></ul>
     *
     * <p>If the default class pool cannot find any class files,
     * try <code>ClassClassPath</code> and <code>LoaderClassPath</code>.
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
     * @see #cacheCtClass(String,CtClass)
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
     * @see #removeCached(String,CtClass)
     */
    protected void cacheCtClass(String classname, CtClass c) {
        classes.put(classname, c);
    }

    /**
     * Provide a hook so that subclasses can do their own
     * caching of classes.
     *
     * @see #getCached(String)
     * @see #cacheCtClass(String,CtClass)
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
     * Records a name that never exists.
     * For example, a package name can be recorded by this method.
     * This would improve execution performance
     * since <code>get()</code> does not search the class path at all
     * if the given name is an invalid name recorded by this method.
     * Note that searching the class path takes relatively long time.
     *
     * @param name          a class name (separeted by dot).
     */
    public void recordInvalidClassName(String name) {
        source.recordInvalidClassName(name);
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
     * A simple class loader used by <code>toClass()</code>
     * in <code>CtClass</code>.
     * This class loader is provided for convenience.  If you need more
     * complex functionality, you should write your own class loader.
     *
     * @see CtClass#forName(String)
     * @see CtClass#toClass()
     * @see ClassPool#toClass(CtClass)
     * @see ClassPool#forName(String)
     */
    public static class SimpleLoader extends ClassLoader {
        /**
         * Loads a class.
         *
         * @param name		the fully qualified class name.
         * @param classfile	the class file.
         * @throws ClassFormatError	if the class file is wrong.
         */
        public Class loadClass(String name, byte[] classfile)
            throws ClassFormatError
        {
            Class c = defineClass(name, classfile, 0, classfile.length);
            resolveClass(c);
            return c;
        }
    };

    private static SimpleLoader classLoader = null;

    /**
     * Returns a <code>java.lang.Class</code> object that has been loaded
     * by <code>toClass()</code> in <code>CtClass</code>
     * or <code>ClassPool</code>.  Such an object cannot be
     * obtained by <code>java.lang.Class.forName()</code> because it has
     * been loaded by an internal class loader of Javassist.
     *
     * @param name	the fully-qualified class name.
     * @see #toClass(CtClass)
     */
    public Class forName(String name) throws ClassNotFoundException {
        if (classLoader == null)
            classLoader = new SimpleLoader();

        return classLoader.loadClass(name);
    }

   /**
    * Loads the class represented by a <code>CtClass</code> object.
    * <code>toClass()</code> in <code>CtClass</code> calls this method
    * to load a class.
    *
    * <p>This method can be overridden to change the class loader.
    * In the default implementation,
    * the class loader is a <code>SimpleLoader</code>.
    * If this method is overridden, <code>forName()</code> must be also
    * overridden for extending the behavior.
    *
    * @param cc     the loaded <code>CtClass</code>.
    * @return   the <code>java.lang.Class</code> object representing
    *           the loaded class.
    * @see #forName(String)
    */
    public Class toClass(CtClass cc)
        throws NotFoundException, CannotCompileException, IOException
    {
        if (cc.getClassPool() != this)
           throw new CannotCompileException(
                 "not contained in this class pool: " + cc.getName());

        try {
            if (classLoader == null)
                classLoader = new SimpleLoader();

            return classLoader.loadClass(cc.getName(), cc.toBytecode());
        }
        catch (ClassFormatError e) {
            throw new CannotCompileException(e, cc.getName());
        }
    }

   /**
     * Reads a class file and constructs a <code>CtClass</code>
     * object with a new name.
     * This method is useful if you want to generate a new class as a copy
     * of another class (except the class name).  For example,
     *
     * <ul><pre>
     * getAndRename("Point", "Pair")
     * </pre></ul>
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
        cacheCtClass(newName, clazz);
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
        else
            return clazz;
    }

    /**
     * @param useCache  false if the cached CtClass must be ignored.
     * @param searchParent  false if the parent class pool is not searched.
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
            if (useCache)
                cacheCtClass(classname, clazz);

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
                throw new RuntimeException(classname +
                                       ": frozen class (cannot edit)");
    }

    /* for CtClassType.getClassFile2().  Don't delegate to the parent.
     */
    InputStream openClassfile(String classname)
        throws NotFoundException
    {
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
     *
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
     * @param classfile         class file.
     * @exception RuntimeException      if there is a frozen class with the
     *                                  the same name.
     * @see javassist.ByteArrayClassPath
     */
    public CtClass makeClass(InputStream classfile)
        throws IOException, RuntimeException
    {
        classfile = new BufferedInputStream(classfile);
        CtClass clazz = new CtClassType(classfile, this);
        clazz.checkModify();
        String classname = clazz.getName();
        checkNotFrozen(classname);
        cacheCtClass(classname, clazz);
        return clazz;
    }

    /**
     * Creates a new public class.
     * If there already exists a class with the same name, the new class
     * overwrites that previous class.
     *
     * @param classname         a fully-qualified class name.
     * @exception RuntimeException      if the existing class is frozen.
     */
    public CtClass makeClass(String classname) throws RuntimeException {
        return makeClass(classname, null);
    }

    /**
     * Creates a new public class.
     * If there already exists a class/interface with the same name,
     * the new class overwrites that previous class.
     *
     * @param classname         a fully-qualified class name.
     * @param superclass        the super class.
     * @exception RuntimeException      if the existing class is frozen.
     */
    public synchronized CtClass makeClass(String classname, CtClass superclass)
        throws RuntimeException
    {
        checkNotFrozen(classname);
        CtClass clazz = new CtNewClass(classname, this, false, superclass);
        cacheCtClass(classname, clazz);
        return clazz;
    }

    /**
     * Creates a new nested class.
     * This method is called by CtClassType.makeNestedClass().
     *
     * @param classname     a fully-qualified class name.
     * @return      the nested class.
     */
    synchronized CtClass makeNestedClass(String classname) {
        checkNotFrozen(classname);
        CtClass clazz = new CtNewNestedClass(classname, this, false, null);
        cacheCtClass(classname, clazz);
        return clazz;
    }

    /**
     * Creates a new public interface.
     * If there already exists a class/interface with the same name,
     * the new interface overwrites that previous one.
     *
     * @param name              a fully-qualified interface name.
     * @exception RuntimeException      if the existing interface is frozen.
     */
    public CtClass makeInterface(String name) throws RuntimeException {
        return makeInterface(name, null);
    }

    /**
     * Creates a new public interface.
     * If there already exists a class/interface with the same name,
     * the new interface overwrites that previous one.
     *
     * @param name              a fully-qualified interface name.
     * @param superclass        the super interface.
     * @exception RuntimeException      if the existing interface is frozen.
     */
    public synchronized CtClass makeInterface(String name, CtClass superclass)
        throws RuntimeException
    {
        checkNotFrozen(name);
        CtClass clazz = new CtNewClass(name, this, true, superclass);
        cacheCtClass(name, clazz);
        return clazz;
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
     *
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
     *
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
     * @param pathname  the path name of the directory or jar file.
     *                  It must not end with a path separator ("/").
     * @return          the inserted class path.
     * @exception NotFoundException     if the jar file is not found.
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
     * @param pathname  the path name of the directory or jar file.
     *                  It must not end with a path separator ("/").
     * @return          the appended class path.
     * @exception NotFoundException     if the jar file is not found.
     */
    public ClassPath appendClassPath(String pathname)
        throws NotFoundException
    {
        return source.appendClassPath(pathname);
    }

    /**
     * Detatches the <code>ClassPath</code> object from the search path.
     * The detached <code>ClassPath</code> object cannot be added
     * to the pathagain.
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
     * @param pathlist          a (semi)colon-separated list of
     *                          the path names of directories and jar files.
     *                          The directory name must not end with a path
     *                          separator ("/").
     *
     * @exception NotFoundException     if a jar file is not found.
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
}

