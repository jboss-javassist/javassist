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

package javassist.tools.reflect;

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
