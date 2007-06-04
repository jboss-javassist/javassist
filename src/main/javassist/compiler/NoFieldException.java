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

package javassist.compiler;

import javassist.compiler.ast.ASTree;

public class NoFieldException extends CompileError {
    private String fieldName;
    private ASTree expr;

    /* NAME must be JVM-internal representation.
     */
    public NoFieldException(String name, ASTree e) {
        super("no such field: " + name);
        fieldName = name;
        expr = e;
    }

    /* The returned name should be JVM-internal representation.
     */
    public String getField() { return fieldName; }

    /* Returns the expression where this exception is thrown.
     */
    public ASTree getExpr() { return expr; }
}
