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
package javassist.web;

/**
 * Thrown when receiving an invalid HTTP request.
 */
public class BadHttpRequest extends Exception {
    private Exception e;

    public BadHttpRequest() { e = null; }

    public BadHttpRequest(Exception _e) { e = _e; }

    public String toString() {
        if (e == null)
            return super.toString();
        else
            return e.toString();
    }
}
