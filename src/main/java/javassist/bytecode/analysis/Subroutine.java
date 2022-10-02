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
package javassist.bytecode.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a nested method subroutine (marked by JSR and RET).
 *
 * @author Jason T. Greene
 */
public class Subroutine {
    //private Set callers = new HashSet();
    private List<Integer> callers = new ArrayList<Integer>();
    private Set<Integer> access = new HashSet<Integer>();
    private int start;

    public Subroutine(int start, int caller) {
        this.start = start;
        callers.add(caller);
    }

    public void addCaller(int caller) {
        callers.add(caller);
    }

    public int start() {
        return start;
    }

    public void access(int index) {
        access.add(index);
    }

    public boolean isAccessed(int index) {
        return access.contains(index);
    }

    public Collection<Integer> accessed() {
        return access;
    }

    public Collection<Integer> callers() {
        return callers;
    }

    @Override
    public String toString() {
        return "start = " + start + " callers = " + callers.toString();
    }
}
