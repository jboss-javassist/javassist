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
 * An interface to access a metaobject and a class metaobject.
 * This interface is implicitly implemented by the reflective
 * class.
 */
public interface Metalevel {
    /**
     * Obtains the class metaobject associated with this object.
     */
    ClassMetaobject _getClass();

    /**
     * Obtains the metaobject associated with this object.
     */
    Metaobject _getMetaobject();

    /**
     * Changes the metaobject associated with this object.
     */
    void _setMetaobject(Metaobject m);
}
