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
package javassist.reflect;

/**
 * A template used for defining a reflective class.
 */
public class Sample {
    private Metaobject _metaobject;
    private static ClassMetaobject _classobject;

    public Object trap(Object[] args, int identifier) throws Throwable {
        Metaobject mobj;
        mobj = _metaobject;
        if (mobj == null)
            return ClassMetaobject.invoke(this, identifier, args);
        else
            return mobj.trapMethodcall(identifier, args);
    }

    public static Object trapStatic(Object[] args, int identifier)
        throws Throwable
    {
        return _classobject.trapMethodcall(identifier, args);
    }

    public static Object trapRead(Object[] args, String name) {
        if (args[0] == null)
            return _classobject.trapFieldRead(name);
        else
            return ((Metalevel)args[0])._getMetaobject().trapFieldRead(name);
    }

    public static Object trapWrite(Object[] args, String name) {
        Metalevel base = (Metalevel)args[0];
        if (base == null)
            _classobject.trapFieldWrite(name, args[1]);
        else
            base._getMetaobject().trapFieldWrite(name, args[1]);

        return null;
    }
}
