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

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Abstract root of ClassPool and ClassPoolTail.
 */
abstract class AbsClassPool {
    public abstract String toString();

    public abstract void recordInvalidClassName(String name);

    abstract byte[] readSource(String classname)
        throws NotFoundException, IOException, CannotCompileException;

    abstract boolean write0(String classname, DataOutputStream out,
                            boolean callback)
        throws NotFoundException, CannotCompileException, IOException;

    abstract URL find(String classname);

    public abstract ClassPath appendSystemPath();
    public abstract ClassPath insertClassPath(ClassPath cp);
    public abstract ClassPath appendClassPath(ClassPath cp);
    public abstract ClassPath insertClassPath(String pathname)
        throws NotFoundException;
    public abstract ClassPath appendClassPath(String pathname)
        throws NotFoundException;
    public abstract void removeClassPath(ClassPath cp);
}
