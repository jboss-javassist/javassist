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

import java.io.InputStream;

/**
 * <code>ClassPath</code> is an interface implemented by objects
 * representing a class search path.
 * <code>ClassPool</code> uses those objects for reading class files.
 *
 * <code>The users can define a class implementing this interface so that
 * a class file is obtained from a non-standard source.
 *
 * @see ClassPool#insertClassPath(ClassPath)
 * @see ClassPool#appendClassPath(ClassPath)
 * @see ClassPool#removeClassPath(ClassPath)
 */
public interface ClassPath {
    /**
     * Opens a class file.
     *
     * <p>This method can return null if the specified class file is not
     * found.  If null is returned, the next search path is examined.
     * However, if an error happens, this method must throw an exception 
     * so that the search is terminated.
     *
     * <p>This method should not modify the contents of the class file.
     *
     * @param classname         a fully-qualified class name
     * @return          the input stream for reading a class file
     */
    InputStream openClassfile(String classname) throws NotFoundException;

    /**
     * This method is invoked when the <code>ClassPath</code> object is
     * detached from the search path.  It will be an empty method in most of
     * classes.
     */
    void close();
}
