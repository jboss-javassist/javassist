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
