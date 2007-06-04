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

package javassist.scopedpool;

import java.util.Map;

import javassist.ClassPool;

/**
 * An interface to <code>ScopedClassPoolRepositoryImpl</code>.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.4 $
 */
public interface ScopedClassPoolRepository {
    /**
     * Records a factory.
     */
    void setClassPoolFactory(ScopedClassPoolFactory factory);

    /**
     * Obtains the recorded factory.
     */
    ScopedClassPoolFactory getClassPoolFactory();

    /**
     * Returns whether or not the class pool is pruned.
     * 
     * @return the prune.
     */
    boolean isPrune();

    /**
     * Sets the prune flag.
     * 
     * @param prune     a new value.
     */
    void setPrune(boolean prune);

    /**
     * Create a scoped classpool.
     * 
     * @param cl    the classloader.
     * @param src   the original classpool.
     * @return the classpool.
     */
    ScopedClassPool createScopedClassPool(ClassLoader cl, ClassPool src);

    /**
     * Finds a scoped classpool registered under the passed in classloader.
     * 
     * @param cl    the classloader.
     * @return the classpool.
     */
    ClassPool findClassPool(ClassLoader cl);

    /**
     * Register a classloader.
     * 
     * @param ucl   the classloader.
     * @return the classpool.
     */
    ClassPool registerClassLoader(ClassLoader ucl);

    /**
     * Get the registered classloaders.
     * 
     * @return the registered classloaders.
     */
    Map getRegisteredCLs();

    /**
     * This method will check to see if a register classloader has been
     * undeployed (as in JBoss).
     */
    void clearUnregisteredClassLoaders();

    /**
     * Unregisters a classpool and unregisters its classloader.
     * 
     * @param cl    the classloader the pool is stored under.
     */
    void unregisterClassLoader(ClassLoader cl);
}
