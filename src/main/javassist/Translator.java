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
     * @param pool	the <code>ClassPool</code> that this translator
     *				is attached to.
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
     * @param pool	the <code>ClassPool</code> that this translator
     *				is attached to.
     * @param classname		a fully-qualified class name
     *
     * @see ClassPool#writeFile(String)
     * @see ClassPool#writeFile(String, String)
     * @see ClassPool#write(String)
     * @see ClassPool#write(String,DataOutputStream)
     */
    void onWrite(ClassPool pool, String classname)
	throws NotFoundException, CannotCompileException;
}
