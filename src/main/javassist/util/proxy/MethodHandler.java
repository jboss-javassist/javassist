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

package javassist.util.proxy;

import java.lang.reflect.Method;

/**
 * The interface implemented by the invocation handler of a proxy
 * instance.
 *
 * @see ProxyFactory#setHandler(MethodHandler)
 */
public interface MethodHandler {
    /**
     * Is called when a method is invoked on a proxy instance associated
     * with this handler.  This method must process that method invocation.
     *
     * @param self          the proxy instance.
     * @param thisMethod    the overridden method declared in the super
     *                      class or interface.
     * @param proceed       the forwarder method for invoking the overridden 
     *                      method.  It is null if the overridden mehtod is
     *                      abstract or declared in the interface.
     * @param args          an array of objects containing the values of
     *                      the arguments passed in the method invocation
     *                      on the proxy instance.  If a parameter type is
     *                      a primitive type, the type of the array element
     *                      is a wrapper class.
     * @return              the resulting value of the method invocation.
     *
     * @throws Throwable    if the method invocation fails.
     */
    Object invoke(Object self, Method thisMethod, Method proceed,
                  Object[] args) throws Throwable;
}
