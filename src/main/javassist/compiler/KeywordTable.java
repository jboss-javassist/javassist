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

public final class KeywordTable extends java.util.HashMap {
    public KeywordTable() { super(); }

    public int lookup(String name) {
        Object found = get(name);
        if (found == null)
            return -1;
        else
            return ((Integer)found).intValue();
    }

    public void append(String name, int t) {
        put(name, new Integer(t));
    }
}
