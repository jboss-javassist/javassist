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
package javassist.bytecode;

final class LongVector {
    private int num;
    private Object[] objects;
    private LongVector next;

    public LongVector(int initialSize) {
        num = 0;
        objects = new Object[initialSize];
        next = null;
    }

    public void addElement(Object obj) {
        LongVector p = this;
        while (p.next != null)
            p = p.next;

        if (p.num < p.objects.length)
            p.objects[p.num++] = obj;
        else {
            LongVector q = p.next = new LongVector(p.objects.length);
            q.objects[q.num++] = obj;
        }
    }

    public int size() {
        LongVector p = this;
        int s = 0;
        while (p != null) {
            s += p.num;
            p = p.next;
        }

        return s;
    }

    public Object elementAt(int i) {
        LongVector p = this;
        while (p != null)
            if (i < p.num)
                return p.objects[i];
            else {
                i -= p.num;
                p = p.next;
            }

        return null;
    }

/*
    public static void main(String [] args) {
        LongVector v = new LongVector(4);
        int i;
        for (i = 0; i < 128; ++i)
            v.addElement(new Integer(i));

        System.out.println(v.size());
        for (i = 0; i < v.size(); ++i) {
            System.out.print(v.elementAt(i));
            System.out.print(", ");
        }
    }
*/
}
