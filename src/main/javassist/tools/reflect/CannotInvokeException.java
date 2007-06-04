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

import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalAccessException;

/**
 * Thrown when method invocation using the reflection API has thrown
 * an exception.
 *
 * @see javassist.tools.reflect.Metaobject#trapMethodcall(int, Object[])
 * @see javassist.tools.reflect.ClassMetaobject#trapMethodcall(int, Object[])
 * @see javassist.tools.reflect.ClassMetaobject#invoke(Object, int, Object[])
 */
public class CannotInvokeException extends RuntimeException {

    private Throwable err = null;

    /**
     * Returns the cause of this exception.  It may return null.
     */
    public Throwable getReason() { return err; }

    /**
     * Constructs a CannotInvokeException with an error message.
     */
    public CannotInvokeException(String reason) {
        super(reason);
    }

    /**
     * Constructs a CannotInvokeException with an InvocationTargetException.
     */
    public CannotInvokeException(InvocationTargetException e) {
        super("by " + e.getTargetException().toString());
        err = e.getTargetException();
    }

    /**
     * Constructs a CannotInvokeException with an IllegalAccessException.
     */
    public CannotInvokeException(IllegalAccessException e) {
        super("by " + e.toString());
        err = e;
    }

    /**
     * Constructs a CannotInvokeException with an ClassNotFoundException.
     */
    public CannotInvokeException(ClassNotFoundException e) {
        super("by " + e.toString());
        err = e;
    }
}
