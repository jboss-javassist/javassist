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

/**
 * An observer of <code>ClassPool</code>.
 * The users can define a class implementing this
 * interface and attach an instance of that class to a
 * <code>ClassPool</code> object so that it can translate a class file
 * when the class file is loaded into the JVM, for example.
 *
 * @see ClassPool#ClassPool(ClassPool,Translator)
 * @see ClassPool#getDefault(Translator)
 */
public interface Translator {
    /**
     * Is invoked by a <code>ClassPool</code> for initialization
     * when the object is attached to a <code>ClassPool</code> object.
     *
     * @param pool      the <code>ClassPool</code> that this translator
     *                          is attached to.
     *
     * @see ClassPool#ClassPool(ClassPool,Translator)
     * @see ClassPool#getDefault(Translator)
     */
    void start(ClassPool pool)
        throws NotFoundException, CannotCompileException;

    /**
     * Is invoked by a <code>ClassPool</code> for notifying that
     * a class is written out to an output stream.
     *
     * <p>If CtClass.frozen() is true, that is, if the class has been
     * already modified and written, then onWrite() is not invoked.
     *
     * @param pool      the <code>ClassPool</code> that this translator
     *                          is attached to.
     * @param classname         a fully-qualified class name
     *
     * @see ClassPool#writeFile(String)
     * @see ClassPool#writeFile(String, String)
     * @see ClassPool#write(String)
     * @see ClassPool#write(String,DataOutputStream)
     */
    void onWrite(ClassPool pool, String classname)
        throws NotFoundException, CannotCompileException;
}
