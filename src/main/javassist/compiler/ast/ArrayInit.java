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
 * Array initializer such as <code>{ 1, 2, 3 }</code>.
 */
public class ArrayInit extends ASTList {
    /** default serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an object.
     * @param firstElement      maybe null when the initializer is <code>{}</code> (empty).
     */
    public ArrayInit(ASTree firstElement) {
        super(firstElement);
    }

    /**
     * Gets the number of the elements.  Don't call {@link #length()}.
     *
     * @return the number of the elements.
     */
    public int size() {
        int s = length();
        if (s == 1 && head() == null)
            return 0;
        else
            return s;
    }

    @Override
    public void accept(Visitor v) throws CompileError { v.atArrayInit(this); }

    @Override
    public String getTag() { return "array"; }
}
