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

    private CtClass[] interfaces = null;

    public int getModifiers() {
        int mod = Modifier.FINAL;
        try {
            mod |= getComponentType().getModifiers()
                   & (Modifier.PROTECTED | Modifier.PUBLIC | Modifier.PRIVATE);
        }
        catch (NotFoundException e) {}
        return mod;
    }

    public CtClass[] getInterfaces() throws NotFoundException {
        if (interfaces == null) {
            Class[] intfs = Object[].class.getInterfaces();
            // java.lang.Cloneable and java.io.Serializable.
            // If the JVM is CLDC, intfs is empty.
            interfaces = new CtClass[intfs.length];
            for (int i = 0; i < intfs.length; i++)
                interfaces[i] = pool.get(intfs[i].getName());
        }

        return interfaces;
    }

    public boolean subtypeOf(CtClass clazz) throws NotFoundException {
        if (super.subtypeOf(clazz))
            return true;

        String cname = clazz.getName();
        if (cname.equals(javaLangObject))
            return true;

        CtClass[] intfs = getInterfaces();
        for (int i = 0; i < intfs.length; i++)
            if (intfs[i].subtypeOf(clazz))
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
