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

import java.io.InputStream;
import java.net.URL;

/**
 * <code>ClassPath</code> is an interface implemented by objects
 * representing a class search path.
 * <code>ClassPool</code> uses those objects for reading class files.
 *
 * <p>The users can define a class implementing this interface so that
 * a class file is obtained from a non-standard source.
 *
 * @see ClassPool#insertClassPath(ClassPath)
 * @see ClassPool#appendClassPath(ClassPath)
 * @see ClassPool#removeClassPath(ClassPath)
 */
public interface ClassPath {
    /**
     * Opens a class file.
     * This method may be called just to examine whether the class file
     * exists as well as to read the contents of the file.
     *
     * <p>This method can return null if the specified class file is not
     * found.  If null is returned, the next search path is examined.
     * However, if an error happens, this method must throw an exception 
     * so that the search will be terminated.
     *
     * <p>This method should not modify the contents of the class file.
     *
     * @param classname         a fully-qualified class name
     * @return          the input stream for reading a class file
     * @see javassist.Translator
     */
    InputStream openClassfile(String classname) throws NotFoundException;

    /**
     * Returns the uniform resource locator (URL) of the class file
     * with the specified name.
     *
     * @param classname         a fully-qualified class name.
     * @return null if the specified class file could not be found.
     */
    URL find(String classname);

    /**
     * This method is invoked when the <code>ClassPath</code> object is
     * detached from the search path.  It will be an empty method in most of
     * classes.
     */
    void close();
}
