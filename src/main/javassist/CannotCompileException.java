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

import javassist.compiler.CompileError;

/**
 * Thrown when bytecode transformation has failed.
 */
public class CannotCompileException extends Exception {
    private String message;

    public String getReason() {
	if (message != null)
	    return message;
	else
	    return this.toString();
    }

    /**
     * Constructs a CannotCompileException with a message.
     */
    public CannotCompileException(String msg) {
	super(msg);
	message = msg;
    }

    /**
     * Constructs a CannotCompileException with an <code>Exception</code>.
     */
    public CannotCompileException(Exception e) {
	super("by " + e.toString());
	message = null;
    }

    /**
     * Constructs a CannotCompileException with a
     * <code>NotFoundException</code>.
     */
    public CannotCompileException(NotFoundException e) {
	this("cannot find " + e.getMessage());
    }

    /**
     * Constructs a CannotCompileException with an <code>CompileError</code>.
     */
    public CannotCompileException(CompileError e) {
	super("[source error] " + e.getMessage());
	message = null;
    }

    /**
     * Constructs a CannotCompileException
     * with a <code>ClassNotFoundException</code>.
     */
    public CannotCompileException(ClassNotFoundException e, String name) {
	this("cannot find " + name);
    }

    /**
     * Constructs a CannotCompileException with a ClassFormatError.
     */
    public CannotCompileException(ClassFormatError e, String name) {
	this("invalid class format: " + name);
    }
}
