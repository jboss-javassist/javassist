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

package javassist.runtime;

/**
 * A support class for implementing <code>$cflow</code>.
 * This support class is required at runtime
 * only if <code>$cflow</code> is used.
 *
 * @see javassist.CtBehavior#useCflow(String)
 */
public class Cflow extends ThreadLocal {
    private static class Depth {
	private int depth;
	Depth() { depth = 0; }
	int get() { return depth; }
	void inc() { ++depth; }
	void dec() { --depth; }
    }

    protected synchronized Object initialValue() {
	return new Depth();
    }

    /**
     * Increments the counter.
     */
    public void enter() { ((Depth)get()).inc(); }

    /**
     * Decrements the counter.
     */
    public void exit() { ((Depth)get()).dec(); }

    /**
     * Returns the value of the counter.
     */
    public int value() { return ((Depth)get()).get(); }
}
