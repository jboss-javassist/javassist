/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package javassist;

import java.io.*;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The class loader for Javassist (JDK 1.2 or later only).
 *
 * <p>This is a sample class loader using <code>ClassPool</code>.
 * Unlike a regular class loader, this class loader obtains bytecode
 * from a <code>ClassPool</code>.
 *
 * <p>Note that Javassist can be used without this class loader; programmers
 * can define their own versions of class loader.  They can run
 * a program even without any user-defined class loader if that program
 * is statically translated with Javassist.
 * This class loader is just provided as a utility class.
 *
 * <p>Suppose that an instance of <code>MyTranslator</code> implementing
 * the interface <code>Translator</code> is responsible for modifying
 * class files.
 * The startup program of an application using <code>MyTranslator</code>
 * should be something like this:
 *
 * <ul><pre>
 * import javassist.*;
 *
 * public class Main {
 *   public static void main(String[] args) throws Throwable {
 *     MyTranslator myTrans = new MyTranslator();
 *     ClassPool cp = ClassPool.getDefault(myTrans);
 *     Loader cl = new Loader(cp);
 *     cl.run("MyApp", args);
 *   }
 * }
 * </pre></ul>
 *
 * <p>Class <code>MyApp</code> is the main program of the application.
 *
 * <p>This program should be executed as follows:
 *
 * <ul><pre>
 * % java Main <i>arg1</i> <i>arg2</i>...
 * </pre></ul>
 *
 * <p>It modifies the class <code>MyApp</code> with a <code>MyTranslator</code>
 * object before loading it into the JVM.
 * Then it calls <code>main()</code> in <code>MyApp</code> with arguments
 * <i>arg1</i>, <i>arg2</i>, ...
 *
 * <p>This program execution is equivalent to:
 *
 * <ul><pre>
 * % java MyApp <i>arg1</i> <i>arg2</i>...
 * </pre></ul>
 *
 * <p>except that classes are translated by <code>MyTranslator</code>
 * at load time.
 *
 * <p>If only a particular class is modified when it is loaded,
 * a program like the following can be used:
 *
 * <ul><pre>ClassPool cp = ClassPool.getDefault();
 * Loader cl = new Loader(cp);
 *
 * CtClass ct = cp.get("test.Rectangle");
 * ct.setSuperclass(loader.get("test.Point"));
 *
 * Class c = cl.loadClass("test.Rectangle");
 * Object rect = c.newInstance();</pre></ul>
 *
 * <p>This program modifies the super class of the <code>Rectangle</code>
 * class and loads it into the JVM with a class loader <code>cl</code>.
 *
 * <p><b>Note 1:</b>
 *
 * <p>This class loader does not allow the users to intercept the loading
 * of <code>java.*</code> and <code>javax.*</code> classes unless
 * <code>Loader.doDelegation</code> is <code>false</code>.  Also see
 * Note 2.
 *
 * <p><b>Note 2:</b>
 *
 * <p>If classes are loaded with different class loaders, they belong to
 * separate name spaces.  If class <code>C</code> is loaded by a class
 * loader <code>CL</code>, all classes that the class <code>C</code>
 * refers to are also loaded by <code>CL</code>.  However, if <code>CL</code>
 * delegates the loading of the class <code>C</code> to <code>CL'</code>,
 * then those classes that the class <code>C</code> refers to
 * are loaded by a parent class loader <code>CL'</code>
 * instead of <code>CL</code>.
 *
 * <p>If an object of class <code>C</code> is assigned
 * to a variable of class <code>C</code> belonging to a different name
 * space, then a <code>ClassCastException</code> is thrown.
 *
 * <p>Because of the fact above, this loader delegates only the loading of
 * <code>javassist.Loader</code>
 * and classes included in package <code>java.*</code> and
 * <code>javax.*</code> to the parent class
 * loader.  Other classes are directly loaded by this loader.
 *
 * <p>For example, suppose that <code>java.lang.String</code> would be loaded
 * by this loader while <code>java.io.File</code> is loaded by the parent
 * class loader.  If the constructor of <code>java.io.File</code> is called
 * with an instance of <code>java.lang.String</code>, then it may throw
 * an exception since it accepts an instance of only the
 * <code>java.lang.String</code> loaded by the parent class loader.
 *
 * @see javassist.ClassPool
 * @see javassist.Translator
 */
public class Loader extends ClassLoader {
    private Hashtable notDefinedHere;   // must be atomic.
    private Vector notDefinedPackages;  // must be atomic.
    private ClassPool source;

    /**
     * Specifies the algorithm of class loading.
     *
     * <p>This class loader uses the parent class loader for
     * <code>java.*</code> and <code>javax.*</code> classes.
     * If this variable <code>doDelegation</code>
     * is <code>false</code>, this class loader does not delegate those
     * classes to the parent class loader.
     *
     * <p>The default value is <code>true</code>.
     */
    public boolean doDelegation = true;

    /**
     * Creates a new class loader.
     */
    public Loader() {
        this(null);
    }

    /**
     * Creates a new class loader.
     *
     * @param cp        the source of class files.
     */
    public Loader(ClassPool cp) {
        init(cp);
    }

    /**
     * Creates a new class loader
     * using the specified parent class loader for delegation.
     *
     * @param parent    the parent class loader.
     * @param cp        the source of class files.
     */
    public Loader(ClassLoader parent, ClassPool cp) {
        super(parent);
        init(cp);
    }

    private void init(ClassPool cp) {
        notDefinedHere = new Hashtable();
        notDefinedPackages = new Vector();
        source = cp;
        delegateLoadingOf("javassist.Loader");
    }

    /**
     * Records a class so that the loading of that class is delegated
     * to the parent class loader.
     *
     * <p>If the given class name ends with <code>.</code> (dot), then
     * that name is interpreted as a package name.  All the classes
     * in that package and the sub packages are delegated.
     */
    public void delegateLoadingOf(String classname) {
        if (classname.endsWith("."))
            notDefinedPackages.addElement(classname);
        else
            notDefinedHere.put(classname, this);
    }

    /**
     * Sets the soruce <code>ClassPool</code>.
     */
    public void setClassPool(ClassPool cp) {
        source = cp;
    }

    /**
     * Loads a class with an instance of <code>Loader</code>
     * and calls <code>main()</code> of that class.
     *
     * <p>This method calls <code>run()</code>.
     *
     * @param args[0]           class name to be loaded.
     * @param args[1-n]         parameters passed to <code>main()</code>.
     *
     * @see javassist.Loader#run(String[])
     */
    public static void main(String[] args) throws Throwable {
        Loader cl = new Loader();
        cl.run(args);
    }

    /**
     * Loads a class and calls <code>main()</code> in that class.
     *
     * @param args[0]           the name of the loaded class.
     * @param args[1-n]         parameters passed to <code>main()</code>.
     */
    public void run(String[] args) throws Throwable {
        int n = args.length - 1;
        if (n >= 0) {
            String[] args2 = new String[n];
            for (int i = 0; i < n; ++i)
                args2[i] = args[i + 1];

            run(args[0], args2);
        }
    }

    /**
     * Loads a class and calls <code>main()</code> in that class.
     *
     * @param classname         the loaded class.
     * @param args              parameters passed to <code>main()</code>.
     */
    public void run(String classname, String[] args) throws Throwable {
        Class c = loadClass(classname);
        try {
            c.getDeclaredMethod("main", new Class[] { String[].class })
                .invoke(null, new Object[] { args });
        }
        catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * Requests the class loader to load a class.
     */
    protected Class loadClass(String name, boolean resolve)
        throws ClassFormatError, ClassNotFoundException
    {
        Class c = findLoadedClass(name);
        if (c == null)
            c = loadClassByDelegation(name);

        if (c == null)
            c = findClass(name);

        if (c == null)
            c = delegateToParent(name);

        if (resolve)
            resolveClass(c);

        return c;
    }

    /**
     * Finds the specified class using <code>ClassPath</code>.
     * If the source throws an exception, this returns null.
     *
     * <p>This method can be overridden by a subclass of
     * <code>Loader</code>.
     */
    protected Class findClass(String name) {
        byte[] classfile;
        try {
            if (source != null)
                classfile = source.write(name);
            else {
                String jarname = "/" + name.replace('.', '/') + ".class";
                InputStream in = this.getClass().getResourceAsStream(jarname);

                classfile = ClassPoolTail.readStream(in);
            }
        }
        catch (Exception e) {
            return null;
        }

        return defineClass(name, classfile, 0, classfile.length);
    }

    private Class loadClassByDelegation(String name)
        throws ClassNotFoundException
    {
        /* The swing components must be loaded by a system
         * class loader.
         * javax.swing.UIManager loads a (concrete) subclass
         * of LookAndFeel by a system class loader and cast
         * an instance of the class to LookAndFeel for
         * (maybe) a security reason.  To avoid failure of
         * type conversion, LookAndFeel must not be loaded
         * by this class loader.
         */

        Class c = null;
        if (doDelegation)
            if (name.startsWith("java.") || name.startsWith("javax.")
                || name.startsWith("sun.") || name.startsWith("com.sun.")
                || notDelegated(name))
                c = delegateToParent(name);

        return c;
    }

    private boolean notDelegated(String name) {
        if (notDefinedHere.get(name) != null)
            return true;

        int n = notDefinedPackages.size();
        for (int i = 0; i < n; ++i)
            if (name.startsWith((String)notDefinedPackages.elementAt(i)))
                return true;

        return false;
    }

    private Class delegateToParent(String classname)
        throws ClassNotFoundException
    {
        ClassLoader cl = getParent();
        if (cl != null)
            return cl.loadClass(classname);
        else
            return findSystemClass(classname);
    }
}
