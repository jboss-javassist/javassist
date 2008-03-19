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

package javassist.runtime;

/**
 * A support class for implementing <code>$sig</code> and
 * <code>$type</code>.
 * This support class is required at runtime
 * only if <code>$sig</code> or <code>$type</code> is used.
 */
public class Desc {

    /**
     * Specifies how a <code>java.lang.Class</code> object is loaded.
     *
     * <p>If true, it is loaded by:
     * <ul><pre>Thread.currentThread().getContextClassLoader().loadClass()</pre></ul>
     * <p>If false, it is loaded by <code>Class.forName()</code>.
     * The default value is false.
     */
    public static boolean useContextClassLoader = false;

    private static Class getClassObject(String name)
        throws ClassNotFoundException
    {
        if (useContextClassLoader)
            return Thread.currentThread().getContextClassLoader()
                   .loadClass(name);
        else
            return Class.forName(name);
    }

    /**
     * Interprets the given class name.
     * It is used for implementing <code>$class</code>.
     */
    public static Class getClazz(String name) {
        try {
            return getClassObject(name);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "$class: internal error, could not find class '" + name 
                    + "' (Desc.useContextClassLoader: " 
                    + Boolean.toString(useContextClassLoader) + ")", e); 
        }
    }

    /**
     * Interprets the given type descriptor representing a method
     * signature.  It is used for implementing <code>$sig</code>.
     */
    public static Class[] getParams(String desc) {
        if (desc.charAt(0) != '(')
            throw new RuntimeException("$sig: internal error");

        return getType(desc, desc.length(), 1, 0);
    }

    /**
     * Interprets the given type descriptor.
     * It is used for implementing <code>$type</code>.
     */
    public static Class getType(String desc) {
        Class[] result = getType(desc, desc.length(), 0, 0);
        if (result == null || result.length != 1)
            throw new RuntimeException("$type: internal error");

        return result[0];
    }

    private static Class[] getType(String desc, int descLen,
                                   int start, int num) {
        Class clazz;
        if (start >= descLen)
            return new Class[num];

        char c = desc.charAt(start);
        switch (c) {
        case 'Z' :
            clazz = Boolean.TYPE;
            break;
        case 'C' :
            clazz = Character.TYPE;
            break;
        case 'B' :
            clazz = Byte.TYPE;
            break;
        case 'S' :
            clazz = Short.TYPE;
            break;
        case 'I' :
            clazz = Integer.TYPE;
            break;
        case 'J' :
            clazz = Long.TYPE;
            break;
        case 'F' :
            clazz = Float.TYPE;
            break;
        case 'D' :
            clazz = Double.TYPE;
            break;
        case 'V' :
            clazz = Void.TYPE;
            break;
        case 'L' :
        case '[' :
            return getClassType(desc, descLen, start, num);
        default :
            return new Class[num];
        }

        Class[] result = getType(desc, descLen, start + 1, num + 1);
        result[num] = clazz;
        return result;
    }

    private static Class[] getClassType(String desc, int descLen,
                                        int start, int num) {
        int end = start;
        while (desc.charAt(end) == '[')
            ++end;

        if (desc.charAt(end) == 'L') {
            end = desc.indexOf(';', end);
            if (end < 0)
                throw new IndexOutOfBoundsException("bad descriptor");
        }

        String cname;
        if (desc.charAt(start) == 'L')
            cname = desc.substring(start + 1, end);
        else
            cname = desc.substring(start, end + 1);

        Class[] result = getType(desc, descLen, end + 1, num + 1);
        try {
            result[num] = getClassObject(cname.replace('/', '.'));
        }
        catch (ClassNotFoundException e) {
            // "new RuntimeException(e)" is not available in JDK 1.3.
            throw new RuntimeException(e.getMessage());
        }

        return result;
    }
}
