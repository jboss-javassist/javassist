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
     * @param cp                class pool
     * @param importname        the class imported by the declaration
     * @param args              the parameters specified by the annotation
     * @return                  the classes imported in the java source
     *                          program produced by the preprocessor.
     */
    public CtClass[] assist(ClassPool cp, String importname,
                            String[] args) throws CannotCompileException;
}
