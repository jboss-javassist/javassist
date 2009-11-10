/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2009 Shigeru Chiba. All Rights Reserved.
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

package javassist.bytecode.annotation;

/**
 * Thrown if the linkage fails.
 * It keeps the name of the class that caused this error. 
 */
public class NoSuchClassError extends Error {
    private String className;

    /**
     * Constructs an exception.
     */
    public NoSuchClassError(String className, Error cause) {
        super(cause.toString(), cause);
        this.className = className;
    }

    /**
     * Returns the name of the class not found. 
     */
    public String getClassName() {
        return className;
    }
}
