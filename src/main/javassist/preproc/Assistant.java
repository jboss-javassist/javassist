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

package javassist.preproc;

import javassist.CtClass;
import javassist.CannotCompileException;
import javassist.ClassPool;

/**
 * This is an interface for objects invoked by the
 * Javassist preprocessor when the preprocessor encounters an annotated
 * import declaration.
 *
 * @see javassist.preproc.Compiler
 */
public interface Assistant {
    /**
     * Is called when the Javassist preprocessor encounters an
     * import declaration annotated with the "by" keyword.
     *
     * <p>The original import declaration is replaced with new import
     * declarations of classes returned by this method.  For example,
     * the following implementation does not change the original
     * declaration:
     *
     * <ul><pre>
     * public CtClass[] assist(ClassPool cp, String importname, String[] args) {
     *     return new CtClass[] { cp.get(importname) };
     * }
     * </pre></uL>
     *
     * @param cp		class pool
     * @param importname	the class imported by the declaration
     * @param args		the parameters specified by the annotation
     * @return			the classes imported in the java source
     *				program produced by the preprocessor.
     */
    public CtClass[] assist(ClassPool cp, String importname,
			    String[] args) throws CannotCompileException;
}
