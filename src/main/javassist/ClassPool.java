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

package javassist;

import java.io.*;
import java.util.Hashtable;

/**
 * A driver class for controlling bytecode editing with Javassist.
 * It manages where a class file is obtained and how it is modified.
 *
 * <p>A <code>ClassPool</code> object can be regarded as a container
 * of <code>CtClass</code> objects.  It reads class files on demand
 * from various
 * sources represented by <code>ClassPath</code> and create
 * <code>CtClass</code> objects representing those class files.
 * The source may be another <code>ClassPool</code>.  If so,
 * <code>write()</code> is called on the source <code>ClassPool</code>
 * for obtaining a class file.
 *
 * <p>A <code>CtClass</code>
 * object contained in a <code>ClassPool</code> is written to an
 * output stream (or a file) if <code>write()</code>
 * (or <code>writeFile()</code>) is called on the 
 * <code>ClassPool</code>.
 * <code>write()</code> is typically called by a class loader,
 * which obtains the bytecode image to be loaded.
 *
 * <p>The users can modify <code>CtClass</code> objects
 * before those objects are written out.
 * To obtain a reference
 * to a <code>CtClass</code> object contained in a
 * <code>ClassPool</code>, <code>get()</code> should be
 * called on the <code>ClassPool</code>.  If a <code>CtClass</code>
 * object is modified, then the modification is reflected on the resulting
 * class file returned by <code>write()</code> in <code>ClassPool</code>.
 *
 * <p>In summary,
 *
 * <ul>
 * <li><code>get()</code> returns a reference to a <code>CtClass</code>
 *     object contained in a <code>ClassPool</code>.
 *
 * <li><code>write()</code> translates a <code>CtClass</code>
 * object contained in a <code>ClassPool</code> into a class file
 * and writes it to an output stream.
 * </ul>
 *
 * <p>The users can add a listener object receiving an event from a
 * <code>ClassPool</code>.  An event occurs when a listener is
 * added to a <code>ClassPool</code> and when <code>write()</code>
 * is called on a <code>ClassPool</code>.  The listener class
 * must implement <code>Translator</code>.  A typical listener object
 * is used for modifying a <code>CtClass</code> object <i>on demand</i>
 * when it is written to an output stream.
 *
 * <p>The implementation of this class is thread-safe.
 *
 * @see javassist.CtClass
 * @see javassist.ClassPath
 * @see javassist.Translator
 */
public class ClassPool {
    /* If this field is null, then the object must be an instance of
     * ClassPoolTail.
     */
    protected ClassPool source;

    protected Translator translator;

    protected Hashtable classes;        // should be synchronous

   /**
    * Provide a hook so that subclasses can do their own
    * caching of classes
    *
    * @see #removeCached(String)
    */
    protected CtClass getCached(String classname)
    {
        return (CtClass)classes.get(classname); 
    }

   /**
    * Provide a hook so that subclasses can do their own
    * caching of classes
    *
    * @see #getCached(String)
    */
    protected void removeCached(String classname)
    {
        classes.remove(classname);
    }

    /**
     * Creates a class pool.
     *
     * @param src       the source of class files.  If it is null,
     *                  the class search path is initially null.
     * @see javassist.ClassPool#getDefault()
     */
    public ClassPool(ClassPool src) {
        this(src, null);
    }

    /**
     * Creates a class pool.
     *
     * @param src       the source of class files.  If it is null,
     *                  the class search path is initially null.
     * @param trans     the translator linked to this class pool.
     *                  It may be null.
     * @see javassist.ClassPool#getDefault()
     */
    public ClassPool(ClassPool src, Translator trans)
        throws RuntimeException
    {
        classes = new Hashtable();
        CtClass[] pt = CtClass.primitiveTypes;
        for (int i = 0; i < pt.length; ++i)
            classes.put(pt[i].getName(), pt[i]);

        if (src != null)
            source = src;
        else
            source = new ClassPoolTail();

        translator = trans;
        if (trans != null)
            try {
                trans.start(this);
            }
            catch (Exception e) {
                throw new RuntimeException(
                    "Translator.start() throws an exception: "
                    + e.toString());
            }
    }

    protected ClassPool() {
        source = null;
        classes = null;
        translator = null;
    }

    /**
     * Returns the default class pool.
     * The returned object is always identical.
     *
     * <p>The default class pool searches the system search path,
     * which usually includes the platform library, extension
     * libraries, and the search path specified by the
     * <code>-classpath</code> option or the <code>CLASSPATH</code>
     * environment variable.
     *
     * @param t         null or the translator linked to the class pool.
     */
    public static synchronized ClassPool getDefault(Translator t) {
        if (defaultPool == null) {
            ClassPoolTail tail = new ClassPoolTail();
            tail.appendSystemPath();
            defaultPool = new ClassPool(tail, t);
        }
        else if (defaultPool.translator != t)
            throw new RuntimeException(
                "has been created with a different translator");

        return defaultPool;
    }

    private static ClassPool defaultPool = null;

    /**
     * Returns the default class pool.
     * The returned object is always identical.
     *
     * <p>This returns the result of <code>getDefault(null)</code>.
     *
     * @see #getDefault(Translator)
     */
    public static ClassPool getDefault() {
        return getDefault(null);
    }

    /**
     * Returns the class search path.
     */
    public String toString() {
        return source.toString();
    }

    /**
     * Records a name that never exists.  For example, a package name
     * can be recorded by this method.
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
     * Returns the <code>Translator</code> object associated with
     * this <code>ClassPool</code>.
     */
    public Translator getTranslator() { return translator; }

    /**
     * Table of registered cflow variables.
     */
    private Hashtable cflow = null;     // should be synchronous.

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
     * Writes a class file specified with <code>classname</code>
     * in the current directory.
     * It never calls <code>onWrite()</code> on a translator.
     * It is provided for debugging.
     *
     * @param classname         the name of the class written on a local disk.
     */
    public void debugWriteFile(String classname)
        throws NotFoundException, CannotCompileException, IOException
    {
        debugWriteFile(classname, ".");
    }

    /**
     * Writes a class file specified with <code>classname</code>.
     * It never calls <code>onWrite()</code> on a translator.
     * It is provided for debugging.
     *
     * @param classname         the name of the class written on a local disk.
     * @param directoryName     it must end without a directory separator.
     */
    public void debugWriteFile(String classname, String directoryName)
        throws NotFoundException, CannotCompileException, IOException
    {
        writeFile(classname, directoryName, false);
    }

    /* void writeFile(CtClass) should not be defined since writeFile()
     * may be called on the class pool that does not contain the given
     * CtClass object.
     */

    /**
     * Writes a class file specified with <code>classname</code>
     * in the current directory.
     * It calls <code>onWrite()</code> on a translator.
     *
     * @param classname         the name of the class written on a local disk.
     */
    public void writeFile(String classname)
        throws NotFoundException, CannotCompileException, IOException
    {
        writeFile(classname, ".");
    }

    /**
     * Writes a class file specified with <code>classname</code>
     * on a local disk.
     * It calls <code>onWrite()</code> on a translator.
     *
     * @param classname         the name of the class written on a local disk.
     * @param directoryName     it must end without a directory separator.
     */
    public void writeFile(String classname, String directoryName)
        throws NotFoundException, CannotCompileException, IOException
    {
        writeFile(classname, directoryName, true);
    }

    private void writeFile(String classname, String directoryName,
                           boolean callback)
        throws NotFoundException, CannotCompileException, IOException
    {
        String filename = directoryName + File.separatorChar
            + classname.replace('.', File.separatorChar) + ".class";
        int pos = filename.lastIndexOf(File.separatorChar);
        if (pos > 0) {
            String dir = filename.substring(0, pos);
            if (!dir.equals("."))
                new File(dir).mkdirs();
        }

        DataOutputStream out
            = new DataOutputStream(new BufferedOutputStream(
                                new DelayedFileOutputStream(filename)));
        write(classname, out, callback);
        out.close();
    }

    static class DelayedFileOutputStream extends OutputStream {
        private FileOutputStream file;
        private String filename;

        DelayedFileOutputStream(String name) {
            file = null;
            filename = name;
        }

        private void init() throws IOException {
            if (file == null)
                file = new FileOutputStream(filename);
        }

        public void write(int b) throws IOException {
            init();
            file.write(b);
        }

        public void write(byte[] b) throws IOException {
            init();
            file.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            init();
            file.write(b, off, len);

        }

        public void flush() throws IOException {
            init();
            file.flush();
        }

        public void close() throws IOException {
            init();
            file.close();
        }
    }

    /**
     * A simple class loader used by <code>writeAsClass()</code>
     * in <code>ClassPool</code>.
     * This class loader is provided for convenience.  If you need more
     * complex functionality, you should write your own class loader.
     *
     * @see ClassPool#writeAsClass(String)
     * @see CtClass#toClass()
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

    private static SimpleLoader classLoader = new SimpleLoader();

    /**
     * Returns a <code>java.lang.Class</code> object that has been loaded
     * by <code>writeAsClass()</code>.  That object cannot be
     * obtained by <code>java.lang.Class.forName()</code> because it has
     * been loaded by an internal class loader of Javassist.
     *
     * @see #writeAsClass(String)
     * @see javassist.CtClass#toClass()
     */
    public static Class forName(String name) throws ClassNotFoundException {
        return classLoader.loadClass(name);
    }

    /**
     * Returns a <code>java.lang.Class</code> object.
     * It calls <code>write()</code> to obtain a class file and then
     * loads the obtained class file into the JVM.  The returned
     * <code>Class</code> object represents the loaded class.
     *
     * <p>This method is provided for convenience.  If you need more
     * complex functionality, you should write your own class loader.
     *
     * <p>To load a class file, this method uses an internal class loader,
     * which is an instance of <code>ClassPool.SimpleLoader</code>.
     * Thus, that class file is not loaded by the system class loader,
     * which should have loaded this <code>ClassPool</code> class.
     * The internal class loader
     * loads only the classes explicitly specified by this method
     * <code>writeAsClass()</code>.  The other classes are loaded
     * by the parent class loader (the sytem class loader) by delegation.
     *
     * <p>For example,
     *
     * <ul><pre>class Line { Point p1, p2; }</pre></ul>
     *
     * <p>If the class <code>Line</code> is loaded by the internal class
     * loader and the class <code>Point</code> has not been loaded yet,
     * then the class <code>Point</code> that the class <code>Line</code>
     * refers to is loaded by the parent class loader.  There is no
     * chance of modifying the definition of <code>Point</code> with
     * Javassist.
     *
     * <p>The internal class loader is shared among all the instances
     * of <code>ClassPool</code>.
     *
     * @param classname         a fully-qualified class name.
     *
     * @see #forName(String)
     * @see javassist.CtClass#toClass()
     * @see javassist.Loader
     */
    public Class writeAsClass(String classname)
        throws NotFoundException, IOException, CannotCompileException
    {
        try {
            return classLoader.loadClass(classname, write(classname));
        }
        catch (ClassFormatError e) {
            throw new CannotCompileException(e, classname);
        }
    }

    /**
     * Returns a byte array representing the class file.
     * It calls <code>onWrite()</code> on a translator.
     *
     * @param classname         a fully-qualified class name.
     */
    public byte[] write(String classname)
        throws NotFoundException, IOException, CannotCompileException
    {
        ByteArrayOutputStream barray = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(barray);
        try {
            write(classname, out, true);
        }
        finally {
            out.close();
        }

        return barray.toByteArray();
    }

    /**
     * Writes a class file specified by <code>classname</code>
     * to a given output stream.
     * It calls <code>onWrite()</code> on a translator.
     *
     * <p>This method does not close the output stream in the end.
     *
     * @param classname         a fully-qualified class name.
     * @param out               an output stream
     */
    public void write(String classname, DataOutputStream out)
        throws NotFoundException, CannotCompileException, IOException
    {
        write(classname, out, true);
    }

    private void write(String classname, DataOutputStream out,
                       boolean callback)
        throws NotFoundException, CannotCompileException, IOException
    {
        CtClass clazz = (CtClass)getCached(classname);
        if (callback && translator != null
                                && (clazz == null || !clazz.isFrozen())) {
            translator.onWrite(this, classname);
            // The CtClass object might be overwritten.
            clazz = (CtClass)getCached(classname);
        }

        if (clazz == null || !clazz.isModified()) {
            if (clazz != null)
                clazz.freeze();

            source.write(classname, out);
        }
        else
            clazz.toBytecode(out);
    }

    /* for CtClassType.getClassFile2()
     */
    byte[] readSource(String classname)
        throws NotFoundException, IOException, CannotCompileException
    {
        return source.write(classname);
    }

    /*
     * Is invoked by CtClassType.setName().
     */
    synchronized void classNameChanged(String oldname, CtClass clazz) {
        CtClass c = (CtClass)getCached(oldname);
        if (c == clazz)         // must check this equation
            removeCached(oldname);

        String newName = clazz.getName();
        checkNotFrozen(newName, "the class with the new name is frozen.");
        classes.put(newName, clazz);
    }

    /*
     * Is invoked by CtClassType.setName() and methods in this class.
     */
    void checkNotFrozen(String classname, String errmsg)
        throws RuntimeException
    {
        CtClass c = getCached(classname);
        if (c != null && c.isFrozen())
            throw new RuntimeException(errmsg);
    }

    /**
     * Reads a class file and constructs a <code>CtClass</code>
     * object with a new name.
     * This method is useful if that class file has been already
     * loaded and the resulting class is frozen.
     *
     * @param orgName   the original (fully-qualified) class name
     * @param newName   the new class name
     */
    public CtClass getAndRename(String orgName, String newName)
        throws NotFoundException
    {
        CtClass clazz = get0(orgName);
        clazz.setName(newName);         // indirectly calls
                                        // classNameChanged() in this class
        return clazz;
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
    public synchronized CtClass get(String classname)
        throws NotFoundException
    {
        CtClass clazz = getCached(classname);
        if (clazz == null) {
            clazz = get0(classname);
            classes.put(classname, clazz);
        }

        return clazz;
    }

    protected CtClass get0(String classname) throws NotFoundException {
        if (classname.endsWith("[]"))
            return new CtArray(classname, this);
        else {
            checkClassName(classname);
            return new CtClassType(classname, this);
        }
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
     * Creates a new class from the given class file.
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
     */
    public CtClass makeClass(InputStream classfile)
        throws IOException, RuntimeException
    {
        CtClass clazz = new CtClassType(classfile, this);
        clazz.checkModify();
        String classname = clazz.getName();
        checkNotFrozen(classname,
                       "there is a frozen class with the same name.");
        classes.put(classname, clazz);
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
        checkNotFrozen(classname,
                       "the class with the given name is frozen.");
        CtClass clazz = new CtNewClass(classname, this, false, superclass);
        classes.put(classname, clazz);
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
        checkNotFrozen(name,
                       "the interface with the given name is frozen.");
        CtClass clazz = new CtNewClass(name, this, true, superclass);
        classes.put(name, clazz);
        return clazz;
    }

    /**
     * Throws an exception if the class with the specified name does not
     * exist.
     */
    void checkClassName(String classname)
        throws NotFoundException
    {
        source.checkClassName(classname);
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
    public synchronized void removeClassPath(ClassPath cp) {
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

