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

/**
 * An observer of <code>Loader</code>.
 * The users can define a class implementing this
 * interface and attach an instance of that class to a
 * <code>Loader</code> object so that it can translate a class file
 * when the class file is loaded into the JVM.
 *
 * @see Loader#addTranslator(ClassPool, Translator)
 */
public interface Translator {
    /**
     * Is invoked by a <code>Loader</code> for initialization
     * when the object is attached to the <code>Loader</code> object.
     * This method can be used for getting (for caching) some
     * <code>CtClass</code> objects that will be accessed
     * in <code>onLoad()</code> in <code>Translator</code>.
     *
     * @param pool      the <code>ClassPool</code> that this translator
     *                          should use.
     * @see Loader
     * @throws NotFoundException    if a <code>CtClass</code> cannot be found.
     * @throws CannotCompileException   if the initialization by this method
     *                                  fails.
     */
    void start(ClassPool pool)
        throws NotFoundException, CannotCompileException;

    /**
     * Is invoked by a <code>Loader</code> for notifying that
     * a class is loaded.  The <code>Loader</code> calls
     *
     * <ul><pre>
     * pool.get(classname).toBytecode()</pre></ul>
     *
     * to read the class file after <code>onLoad()</code> returns.
     *
     * <p><code>classname</code> may be the name of a class
     * that has not been created yet.
     * If so, <code>onLoad()</code> must create that class so that
     * the <code>Loader</code> can read it after <code>onLoad()</code>
     * returns.
     *
     * @param pool      the <code>ClassPool</code> that this translator
     *                          should use.
     * @param classname     the name of the class being loaded.
     * @see Loader
     * @throws NotFoundException    if a <code>CtClass</code> cannot be found.
     * @throws CannotCompileException   if the code transformation
     *                                  by this method fails.
     */
    void onLoad(ClassPool pool, String classname)
        throws NotFoundException, CannotCompileException;
}
