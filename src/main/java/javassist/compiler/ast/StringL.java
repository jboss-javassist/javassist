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

package javassist.compiler.ast;

import javassist.compiler.CompileError;

/**
 * String literal.
 */
public class StringL extends ASTree {
    /** default serialVersionUID */
    private static final long serialVersionUID = 1L;
    protected String text;

    public StringL(String t) {
        text = t;
    }

    public String get() { return text; }

    @Override
    public String toString() { return "\"" + text + "\""; }

    @Override
    public void accept(Visitor v) throws CompileError { v.atStringL(this); }
}
