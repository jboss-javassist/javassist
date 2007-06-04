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

package javassist.tools.reflect;

import javassist.CannotCompileException;

/**
 * Thrown by <code>makeReflective()</code> in <code>Reflection</code>
 * when there is an attempt to reflect
 * a class that is either an interface or a subclass of
 * either ClassMetaobject or Metaobject.
 *
 * @author Brett Randall
 * @see javassist.tools.reflect.Reflection#makeReflective(CtClass,CtClass,CtClass)
 * @see javassist.CannotCompileException
 */
public class CannotReflectException extends CannotCompileException {
    public CannotReflectException(String msg) {
        super(msg);
    }
}
