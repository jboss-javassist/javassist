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
