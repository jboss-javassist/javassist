/*
 * This file is part of the Javassist toolkit.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * either http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is Javassist.
 *
 * The Initial Developer of the Original Code is Shigeru Chiba.  Portions
 * created by Shigeru Chiba are Copyright (C) 1999-2003 Shigeru Chiba.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * The development of this software is supported in part by the PRESTO
 * program (Sakigake Kenkyu 21) of Japan Science and Technology Corporation.
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
     * @param name		a fully qualified class name
     * @param classfile		the contents of a class file.
     */
    public ByteArrayClassPath(String name, byte[] classfile) {
	this.classname = name;
	this.classfile = classfile;
    }

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
