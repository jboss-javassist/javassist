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

package javassist;

import java.io.*;
import java.util.Hashtable;
import java.util.Vector;
import java.security.ProtectionDomain;

/**
 * The class loader for Javassist.
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
 *     ClassPool cp = ClassPool.getDefault();
 *     Loader cl = new Loader(cp);
 *     cl.addTranslator(cp, myTrans);
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
 * object before the JVM loads it.
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
 * <p>If only a particular class must be modified when it is loaded,
 * the startup program can be simpler; <code>MyTranslator</code> is
 * unnecessary.  For example, if only a class <code>test.Rectangle</code>
 * is modified, the <code>main()</code> method above will be the following:
 *
 * <ul><pre>
 * ClassPool cp = ClassPool.getDefault();
 * Loader cl = new Loader(cp);
 * CtClass ct = cp.get("test.Rectangle");
 * ct.setSuperclass(cp.get("test.Point"));
 * cl.run("MyApp", args);</pre></ul>
 *
 * <p>This program changes the super class of the <code>test.Rectangle</code>
 * class.
 *
 * <p><b>Note 1:</b>
 *
 * <p>This class loader does not allow the users to intercept the loading
 * of <code>java.*</code> and <code>javax.*</code> classes (and
 * <code>sun.*</code>, <code>org.xml.*</code>, ...) unless
 * <code>Loader.doDelegation</code> is <code>false</code>.  This is because
 * the JVM prohibits a user class loader from loading a system class.
 * Also see Note 2.
 * If this behavior is not appropriate, a subclass of <code>Loader</code>
 * must be defined and <code>loadClassByDelegation()</code> must be overridden.
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
    private Hashtable notDefinedHere; // must be atomic.
    private Vector notDefinedPackages; // must be atomic.
    private ClassPool source;
    private Translator translator;
    private ProtectionDomain domain; 

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
        translator = null;
        domain = null;
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
     * Sets the protection domain for the classes handled by this class
     * loader.  Without registering an appropriate protection domain,
     * the program loaded by this loader will not work with a security
     * manager or a signed jar file.
     */
    public void setDomain(ProtectionDomain d) {
        domain = d;
    }

    /**
     * Sets the soruce <code>ClassPool</code>.
     */
    public void setClassPool(ClassPool cp) {
        source = cp;
    }

    /**
     * Adds a translator, which is called whenever a class is loaded.
     *
     * @param cp        the <code>ClassPool</code> object for obtaining
     *                  a class file.
     * @param t         a translator.
     * @throws NotFoundException        if <code>t.start()</code> throws an exception.
     * @throws CannotCompileException   if <code>t.start()</code> throws an exception.
     */
    public void addTranslator(ClassPool cp, Translator t)
        throws NotFoundException, CannotCompileException {
        source = cp;
        translator = t;
        t.start(cp);
    }

    /**
     * Loads a class with an instance of <code>Loader</code>
     * and calls <code>main()</code> of that class.
     *
     * <p>This method calls <code>run()</code>.
     *
     * @param args              command line parameters.
     * <ul>
     * <code>args[0]</code> is the class name to be loaded.
     * <br><code>args[1..n]</code> are parameters passed
     *                      to the target <code>main()</code>.
     * </ul>
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
     * @param args              command line parameters.
     * <ul>
     * <code>args[0]</code> is the class name to be loaded.
     * <br><code>args[1..n]</code> are parameters passed
     *                      to the target <code>main()</code>.
     * </ul>
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
            c.getDeclaredMethod("main", new Class[] { String[].class }).invoke(
                null,
                new Object[] { args });
        }
        catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * Requests the class loader to load a class.
     */
    protected Class loadClass(String name, boolean resolve)
        throws ClassFormatError, ClassNotFoundException {
        name = name.intern();
        synchronized (name) {
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
    }

    /**
     * Finds the specified class using <code>ClassPath</code>.
     * If the source throws an exception, this returns null.
     *
     * <p>This method can be overridden by a subclass of
     * <code>Loader</code>.  Note that the overridden method must not throw
     * an exception when it just fails to find a class file.
     *
     * @return      null if the specified class could not be found.
     * @throws ClassNotFoundException   if an exception is thrown while
     *                                  obtaining a class file.
     */
    protected Class findClass(String name) throws ClassNotFoundException {
        byte[] classfile;
        try {
            if (source != null) {
                if (translator != null)
                    translator.onLoad(source, name);

                try {
                    classfile = source.get(name).toBytecode();
                }
                catch (NotFoundException e) {
                    return null;
                }
            }
            else {
                String jarname = "/" + name.replace('.', '/') + ".class";
                InputStream in = this.getClass().getResourceAsStream(jarname);
                if (in == null)
                    return null;

                classfile = ClassPoolTail.readStream(in);
            }
        }
        catch (Exception e) {
            throw new ClassNotFoundException(
                "caught an exception while obtaining a class file for "
                + name, e);
        }

        int i = name.lastIndexOf('.');
        if (i != -1) {
            String pname = name.substring(0, i);
            if (getPackage(pname) == null)
                try {
                    definePackage(
                        pname, null, null, null, null, null, null, null);
                }
                catch (IllegalArgumentException e) {
                    // ignore.  maybe the package object for the same
                    // name has been created just right away.
                }
        }

        if (domain == null)
            return defineClass(name, classfile, 0, classfile.length);
        else
            return defineClass(name, classfile, 0, classfile.length, domain);
    }

    protected Class loadClassByDelegation(String name)
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
            if (name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("sun.")
                || name.startsWith("com.sun.")
                || name.startsWith("org.w3c.")
                || name.startsWith("org.xml.")
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

    protected Class delegateToParent(String classname)
        throws ClassNotFoundException
    {
        ClassLoader cl = getParent();
        if (cl != null)
            return cl.loadClass(classname);
        else
            return findSystemClass(classname);
    }

    protected Package getPackage(String name) {
        return super.getPackage(name);
    }
    /*
        // Package p = super.getPackage(name);
        Package p = null;
        if (p == null)
            return definePackage(name, null, null, null,
                                 null, null, null, null);
        else
            return p;
    }
    */
}
