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
 * @see javassist.URLClassPath
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
     * @param classname		a fully-qualified class name
     * @return		the input stream for reading a class file
     */
    InputStream openClassfile(String classname) throws NotFoundException;
}
