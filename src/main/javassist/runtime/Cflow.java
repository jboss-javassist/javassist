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
