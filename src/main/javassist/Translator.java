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

/**
 * An observer of <code>Loader</code>.
 * The users can define a class implementing this
 * interface and attach an instance of that class to a
 * <code>Loader</code> object so that it can translate a class file
 * when the class file is loaded into the JVM.
 *
 * @see Loader
 */
public interface Translator {
    /**
     * Is invoked by a <code>Loader</code> for initialization
     * when the object is attached to the <code>Loader</code> object.
     *
     * @param pool      the <code>ClassPool</code> that this translator
     *                          should use.
     * @see Loader
     */
    void start(ClassPool pool)
        throws NotFoundException, CannotCompileException;

    /**
     * Is invoked by a <code>Loader</code> for notifying that
     * a class is loaded.
     *
     * @param pool      the <code>ClassPool</code> that this translator
     *                          should use.
     * @param clazz     the class that is being loaded.
     * @see Loader
     */
    void onWrite(ClassPool pool, CtClass clazz)
        throws NotFoundException, CannotCompileException;
}
