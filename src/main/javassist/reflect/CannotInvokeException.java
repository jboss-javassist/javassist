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
package javassist.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalAccessException;

/**
 * Thrown when method invocation using the reflection API has thrown
 * an exception.
 *
 * @see javassist.reflect.Metaobject#trapMethodcall(int, Object[])
 * @see javassist.reflect.ClassMetaobject#trapMethodcall(int, Object[])
 * @see javassist.reflect.ClassMetaobject#invoke(Object, int, Object[])
 */
public class CannotInvokeException extends RuntimeException {
    /**
     * @serial
     */
    private Throwable err = null;

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
