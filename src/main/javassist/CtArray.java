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
package javassist;

/**
 * Array types.
 */
final class CtArray extends CtClass {
    protected ClassPool pool;

    // the name of array type ends with "[]".
    CtArray(String name, ClassPool cp) {
        super(name);
        pool = cp;
    }

    public ClassPool getClassPool() {
        return pool;
    }

    public boolean isArray() {
        return true;
    }

    public boolean subtypeOf(CtClass clazz) throws NotFoundException {
        if (super.subtypeOf(clazz))
            return true;

        String cname = clazz.getName();
        if (cname.equals(javaLangObject)
            || cname.equals("java.lang.Cloneable"))
            return true;

        return clazz.isArray()
            && getComponentType().subtypeOf(clazz.getComponentType());
    }

    public CtClass getComponentType() throws NotFoundException {
        String name = getName();
        return pool.get(name.substring(0, name.length() - 2));
    }

    public CtClass getSuperclass() throws NotFoundException {
        return pool.get(javaLangObject);
    }

    public CtMethod[] getMethods() {
        try {
            return getSuperclass().getMethods();
        }
        catch (NotFoundException e) {
            return super.getMethods();
        }
    }

    public CtMethod getMethod(String name, String desc)
        throws NotFoundException
    {
        return getSuperclass().getMethod(name, desc);
    }

    public CtConstructor[] getConstructors() {
        try {
            return getSuperclass().getConstructors();
        }
        catch (NotFoundException e) {
            return super.getConstructors();
        }
    }
}
