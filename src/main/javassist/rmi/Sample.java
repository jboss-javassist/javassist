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

package javassist.rmi;

/**
 * A template used for defining a proxy class.
 * The class file of this class is read by the <code>StubGenerator</code>
 * class.
 */
public class Sample {
    private ObjectImporter importer;
    private int objectId;

    public Object forward(Object[] args, int identifier) {
	return importer.call(objectId, identifier, args);
    }

    public static Object forwardStatic(Object[] args, int identifier)
	throws RemoteException
    {
	throw new RemoteException("cannot call a static method.");
    }
}
