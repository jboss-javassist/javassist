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

import java.util.Map;
import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.ClassPool;
import javassist.NotFoundException;

/**
 * A support class for dealing with descriptors.
 *
 * <p>See chapter 4.3 in "The Java Virtual Machine Specification (2nd ed.)"
 */
public class Descriptor {
    /**
     * Converts a class name into the internal representation used in
     * the JVM.
     *
     * <p>Note that <code>toJvmName(toJvmName(s))</code> is equivalent
     * to <code>toJvmName(s)</code>.
     */
    public static String toJvmName(String classname) {
	return classname.replace('.', '/');
    }

    /**
     * Converts a class name from the internal representation used in
     * the JVM to the normal one used in Java.
     */
    public static String toJavaName(String classname) {
	return classname.replace('/', '.');
    }

    /**
     * Returns the internal representation of the class name in the
     * JVM.
     */
    public static String toJvmName(CtClass clazz) {
	if (clazz.isArray())
	    return of(clazz);
	else
	    return toJvmName(clazz.getName());
    }

    /**
     * Substitutes a class name
     * in the given descriptor string.
     *
     * @param desc		descriptor string
     * @param oldname		replaced JVM class name
     * @param newname		substituted JVM class name
     *
     * @see Descriptor#toJvmName(String)
     */
    public static String rename(String desc,
				String oldname, String newname) {
	if (desc.indexOf(oldname) < 0)
	    return desc;

	StringBuffer newdesc = new StringBuffer();
	int head = 0;
	int i = 0;
	for (;;) {
	    int j = desc.indexOf('L', i);
	    if (j < 0)
		break;
	    else if (desc.startsWith(oldname, j + 1)
		     && desc.charAt(j + oldname.length() + 1) == ';') {
		newdesc.append(desc.substring(head, j));
		newdesc.append('L');
		newdesc.append(newname);
		newdesc.append(';');
		head = i = j + oldname.length() + 2;
	    }
	    else {
		i = desc.indexOf(';', j) + 1;
		if (i < 1)
		    break;	// ';' was not found.
	    }
	}

	if (head == 0)
	    return desc;
	else {
	    int len = desc.length();
	    if (head < len)
		newdesc.append(desc.substring(head, len));

	    return newdesc.toString();
	}
    }

    /**
     * Substitutes class names in the given descriptor string
     * according to the given <code>map</code>.
     *
     * @param map		a map between replaced and substituted
     *				JVM class names.
     *
     * @see Descriptor#toJvmName(String)
     */
    public static String rename(String desc, Map map) {
	if (map == null)
	    return desc;

	StringBuffer newdesc = new StringBuffer();
	int head = 0;
	int i = 0;
	for (;;) {
	    int j = desc.indexOf('L', i);
	    if (j < 0)
		break;

	    int k = desc.indexOf(';', j);
	    if (k < 0)
		break;

	    i = k + 1;
	    String name = desc.substring(j + 1, k);
	    String name2 = (String)map.get(name);
	    if (name2 != null) {
		newdesc.append(desc.substring(head, j));
		newdesc.append('L');
		newdesc.append(name2);
		newdesc.append(';');
		head = i;
	    }
	}

	if (head == 0)
	    return desc;
	else {
	    int len = desc.length();
	    if (head < len)
		newdesc.append(desc.substring(head, len));

	    return newdesc.toString();
	}
    }

    /**
     * Returns the descriptor representing the given type.
     */
    public static String of(CtClass type) {
	StringBuffer sbuf = new StringBuffer();
	toDescriptor(sbuf, type);
	return sbuf.toString();
    }

    private static void toDescriptor(StringBuffer desc, CtClass type) {
	if (type.isArray()) {
	    desc.append('[');
	    try {
		toDescriptor(desc, type.getComponentType());
	    }
	    catch (NotFoundException e) {
		desc.append('L');
		String name = type.getName();
		desc.append(toJvmName(name.substring(0, name.length() - 2)));
		desc.append(';');
	    }
	}
	else if (type.isPrimitive()) {
	    CtPrimitiveType pt = (CtPrimitiveType)type;
	    desc.append(pt.getDescriptor());
	}
	else {		// class type
	    desc.append('L');
	    desc.append(type.getName().replace('.', '/'));
	    desc.append(';');
	}
    }

    /**
     * Returns the descriptor representing a constructor receiving
     * the given parameter types.
     *
     * @param paramTypes	parameter types
     */
    public static String ofConstructor(CtClass[] paramTypes) {
	return ofMethod(CtClass.voidType, paramTypes);
    }

    /**
     * Returns the descriptor representing a method that receives
     * the given parameter types and returns the given type.
     *
     * @param returnType	return type
     * @param paramTypes	parameter types
     */
    public static String ofMethod(CtClass returnType, CtClass[] paramTypes) {
	StringBuffer desc = new StringBuffer();
	desc.append('(');
	if (paramTypes != null) {
	    int n = paramTypes.length;
	    for (int i = 0; i < n; ++i)
		toDescriptor(desc, paramTypes[i]);
	}

	desc.append(')');
	if (returnType != null)
	    toDescriptor(desc, returnType);

	return desc.toString();
    }

    /**
     * Returns the descriptor representing a list of parameter types.
     * For example, if the given parameter types are two <code>int</code>,
     * then this method returns <code>"(II)"</code>.
     *
     * @param paramTypes	parameter types
     */
    public static String ofParameters(CtClass[] paramTypes) {
	return ofMethod(null, paramTypes);
    }

    /**
     * Appends a parameter type to the parameter list represented
     * by the given descriptor.
     *
     * <p><code>classname</code> must not be an array type.
     *
     * @param classname		parameter type (not primitive type)
     * @param desc		descriptor
     */
    public static String appendParameter(String classname,
					 String desc) {
	int i = desc.indexOf(')');
	if (i < 0)
	    return desc;
	else {
	    StringBuffer newdesc = new StringBuffer();
	    newdesc.append(desc.substring(0, i));
	    newdesc.append('L');
	    newdesc.append(classname.replace('.', '/'));
	    newdesc.append(';');
	    newdesc.append(desc.substring(i));
	    return newdesc.toString();
	}
    }

    /**
     * Inserts a parameter type at the beginning of the parameter
     * list represented
     * by the given descriptor.
     *
     * <p><code>classname</code> must not be an array type.
     *
     * @param classname		parameter type (not primitive type)
     * @param desc		descriptor
     */
    public static String insertParameter(String classname,
					 String desc) {
	if (desc.charAt(0) != '(')
	    return desc;
	else
	    return "(L" + classname.replace('.', '/') + ';'
		+ desc.substring(1);
    }

    /**
     * Changes the return type included in the given descriptor.
     *
     * <p><code>classname</code> must not be an array type.
     *
     * @param classname		return type
     * @param desc		descriptor
     */
    public static String changeReturnType(String classname, String desc) {
	int i = desc.indexOf(')');
	if (i < 0)
	    return desc;
	else {
	    StringBuffer newdesc = new StringBuffer();
	    newdesc.append(desc.substring(0, i + 1));
	    newdesc.append('L');
	    newdesc.append(classname.replace('.', '/'));
	    newdesc.append(';');
	    return newdesc.toString();
	}
    }

    /**
     * Returns the <code>CtClass</code> objects representing the parameter
     * types specified by the given descriptor.
     *
     * @param desc	descriptor
     * @param cp	the class pool used for obtaining
     *			a <code>CtClass</code> object.
     */
    public static CtClass[] getParameterTypes(String desc, ClassPool cp)
	throws NotFoundException
    {
	if (desc.charAt(0) != '(')
	    return null;
	else {
	    int num = numOfParameters(desc);
	    CtClass[] args = new CtClass[num];
	    int n = 0;
	    int i = 1;
	    do {
		i = toCtClass(cp, desc, i, args, n++);
	    } while(i > 0);
	    return args;
	}
    }

    /**
     * Returns the <code>CtClass</code> object representing the return
     * type specified by the given descriptor.
     *
     * @param desc	descriptor
     * @param cp	the class pool used for obtaining
     *			a <code>CtClass</code> object.
     */
    public static CtClass getReturnType(String desc, ClassPool cp)
	throws NotFoundException
    {
	int i = desc.indexOf(')');
	if (i < 0)
	    return null;
	else {
	    CtClass[] type = new CtClass[1];
	    toCtClass(cp, desc, i + 1, type, 0);
	    return type[0];
	}
    }

    /**
     * Returns the number of the prameters included in the given
     * descriptor.
     *
     * @param desc	descriptor
     */
    public static int numOfParameters(String desc) {
	int n = 0;
	int i = 1;
	for (;;) {
	    char c = desc.charAt(i);
	    if (c == ')')
		break;

	    while (c == '[')
		c = desc.charAt(++i);

	    if (c == 'L') {
		i = desc.indexOf(';', i) + 1;
		if (i <= 0)
		    throw new IndexOutOfBoundsException("bad descriptor");
	    }
	    else
		++i;

	    ++n;
	}

	return n;
    }

    /**
     * Returns a <code>CtClass</code> object representing the type
     * specified by the given descriptor.
     *
     * <p>This method works even if the package-class separator is
     * not <code>/</code> but <code>.</code> (period).  For example,
     * it accepts <code>Ljava.lang.Object;</code>
     * as well as <code>Ljava/lang/Object;</code>.
     *
     * @param desc	descriptor
     * @param cp	the class pool used for obtaining
     *			a <code>CtClass</code> object.
     */
    public static CtClass toCtClass(String desc, ClassPool cp)
	throws NotFoundException
    {
	CtClass[] clazz = new CtClass[1];
	int res = toCtClass(cp, desc, 0, clazz, 0);
	if (res >= 0)
	    return clazz[0];
	else {
	    // maybe, you forgot to surround the class name with
	    // L and ;.  It violates the protocol, but I'm tolerant...
	    return cp.get(desc.replace('/', '.'));
	}
    }

    private static int toCtClass(ClassPool cp, String desc, int i,
				 CtClass[] args, int n)
	throws NotFoundException
    {
	int i2;
	String name;

	int arrayDim = 0;
	char c = desc.charAt(i);
	while (c == '[') {
	    ++arrayDim;
	    c = desc.charAt(++i);
	}

	if (c == 'L') {
	    i2 = desc.indexOf(';', ++i);
	    name = desc.substring(i, i2++).replace('/', '.');
	}
	else {
	    CtClass type = toPrimitiveClass(c);
	    if (type == null)
		return -1;	// error

	    i2 = i + 1;
	    if (arrayDim == 0) {
		args[n] = type;
		return i2;	// neither an array type or a class type
	    }
	    else
		name = type.getName();
	}

	if (arrayDim > 0) {
	    StringBuffer sbuf = new StringBuffer(name);
	    while (arrayDim-- > 0)
		sbuf.append("[]");

	    name = sbuf.toString();
	}

	args[n] = cp.get(name);
	return i2;
    }

    private static CtClass toPrimitiveClass(char c) {
	CtClass type = null;
	switch (c) {
	case 'Z' :
	    type = CtClass.booleanType;
	    break;
	case 'C' :
	    type = CtClass.charType;
	    break;
	case 'B' :
	    type = CtClass.byteType;
	    break;
	case 'S' :
	    type = CtClass.shortType;
	    break;
	case 'I' :
	    type = CtClass.intType;
	    break;
	case 'J' :
	    type = CtClass.longType;
	    break;
	case 'F' :
	    type = CtClass.floatType;
	    break;
	case 'D' :
	    type = CtClass.doubleType;
	    break;
	case 'V' :
	    type = CtClass.voidType;
	    break;
	}

	return type;
    }

    /**
     * Computes the data size specified by the given descriptor.
     * For example, if the descriptor is "D", this method returns 2.
     *
     * <p>If the descriptor represents a method type, this method returns
     * (the size of the returned value) - (the sum of the data sizes
     * of all the parameters).  For example, if the descriptor is
     * "(I)D", then this method returns 1 (= 2 - 1).
     *
     * @param desc	descriptor
     */
    public static int dataSize(String desc) {
	int n = 0;
	char c = desc.charAt(0);
	if (c == '(') {
	    int i = 1;
	    for (;;) {
		c = desc.charAt(i);
		if (c == ')') {
		    c = desc.charAt(i + 1);
		    break;
		}

		boolean array = false;
		while (c == '[') {
		    array = true;
		    c = desc.charAt(++i);
		}

		if (c == 'L') {
		    i = desc.indexOf(';', i) + 1;
		    if (i <= 0)
			throw new IndexOutOfBoundsException("bad descriptor");
		}
		else
		    ++i;

		if (!array && (c == 'J' || c == 'D'))
		    n -= 2;
		else
		    --n;
	    }
	}

	if (c == 'J' || c == 'D')
	    n += 2;
	else if (c != 'V')
	    ++n;

	return n;
    }
}
