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

import java.util.HashMap;
import javassist.compiler.ast.Declarator;

public final class SymbolTable extends HashMap {
    private SymbolTable parent;

    public SymbolTable() { this(null); }

    public SymbolTable(SymbolTable p) {
        super();
        parent = p;
    }

    public SymbolTable getParent() { return parent; }

    public Declarator lookup(String name) {
        Declarator found = (Declarator)get(name);
        if (found == null && parent != null)
            return parent.lookup(name);
        else
            return found;
    }

    public void append(String name, Declarator value) {
        put(name, value);
    }
}
