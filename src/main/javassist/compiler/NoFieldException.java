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
