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

/**
 * A <code>ByteArrayClassPath</code> contains bytes that is served as
 * a class file to a <code>ClassPool</code>.  It is useful to convert
 * a byte array to a <code>CtClass</code> object.
 *
 * <p>For example, if you want to convert a byte array <code>b</code>
 * into a <code>CtClass</code> object representing the class with a name
 * <code>classname</code>, then do as following:
 *
 * <ul><pre>
 * ClassPool cp = ClassPool.getDefault();
 * cp.insertClassPath(new ByteArrayClassPath(classname, b));
 * CtClass cc = cp.get(classname);
 * </pre></ul>
 *
 * <p>The <code>ClassPool</code> object <code>cp</code> uses the created
 * <code>ByteArrayClassPath</code> object as the source of the class file.
 * 
 * <p>A <code>ByteArrayClassPath</code> must be instantiated for every
 * class.  It contains only a single class file.
 *
 * @see javassist.ClassPath
 * @see ClassPool#insertClassPath(ClassPath)
 * @see ClassPool#appendClassPath(ClassPath)
 */
public class ByteArrayClassPath implements ClassPath {
    protected String classname;
    protected byte[] classfile;

    /*
     * Creates a <code>ByteArrayClassPath</code> containing the given
     * bytes.
     *
     * @param name              a fully qualified class name
     * @param classfile         the contents of a class file.
     */
    public ByteArrayClassPath(String name, byte[] classfile) {
        this.classname = name;
        this.classfile = classfile;
    }

    /**
     * Closes this class path.
     */
    public void close() {}

    public String toString() {
        return "byte[]:" + classname;
    }

    /**
     * Opens a class file.
     */
    public InputStream openClassfile(String classname) {
        if(this.classname.equals(classname))
            return new ByteArrayInputStream(classfile);
        else
            return null;
    }
}
