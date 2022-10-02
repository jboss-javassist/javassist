/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.tools.web;

/**
 * Thrown when receiving an invalid HTTP request.
 */
public class BadHttpRequest extends Exception {
    /** default serialVersionUID */
    private static final long serialVersionUID = 1L;
    private Exception e;

    public BadHttpRequest() { e = null; }

    public BadHttpRequest(Exception _e) { e = _e; }

    @Override
    public String toString() {
        if (e == null)
            return super.toString();
        return e.toString();
    }
}
