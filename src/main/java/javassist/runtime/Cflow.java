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

package javassist.runtime;

/**
 * A support class for implementing <code>$cflow</code>.
 * This support class is required at runtime
 * only if <code>$cflow</code> is used.
 *
 * @see javassist.CtBehavior#useCflow(String)
 */
public class Cflow extends ThreadLocal<Cflow.Depth> {
    protected static class Depth {
        private int depth;
        Depth() { depth = 0; }
        int value() { return depth; }
        void inc() { ++depth; }
        void dec() { --depth; }
    }

    @Override
    protected synchronized Depth initialValue() {
        return new Depth();
    }

    /**
     * Increments the counter.
     */
    public void enter() { get().inc(); }

    /**
     * Decrements the counter.
     */
    public void exit() { get().dec(); }

    /**
     * Returns the value of the counter.
     */
    public int value() { return get().value(); }
}
