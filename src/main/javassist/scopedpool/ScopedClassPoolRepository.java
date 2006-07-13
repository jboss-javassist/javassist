/*
* Javassist, a Java-bytecode translator toolkit.
* Copyright (C) 2006 JBoss Inc., All Rights Reserved.
*
* The contents of this file are subject to the Mozilla Public License Version
* 1.1 (the "License"); you may not use this file except in compliance with
* the License.  Alternatively, the contents of this file may be used under
* the terms of the GNU Lesser General Public License Version 2.1 or later.
*
* Software distributed under the License is distributed on an "AS IS"  
basis,
* WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
* for the specific language governing rights and limitations under the
* License.
*/
package javassist.scopedpool;

import java.util.Map;

import javassist.ClassPool;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.2 $
 */
public interface ScopedClassPoolRepository
{
   void setClassPoolFactory(ScopedClassPoolFactory factory);
   
   ScopedClassPoolFactory getClassPoolFactory();

   /**
    * Get the prune.
    * 
    * @return the prune.
    */
   boolean isPrune();

   /**
    * Set the prune.
    * 
    * @param prune the prune.
    */
   void setPrune(boolean prune);
   
   /**
    * Create a scoped classpool
    * 
    * @param cl the classloader
    * @param src the original classpool
    * @return the classpool
    */
   ScopedClassPool createScopedClassPool(ClassLoader cl, ClassPool src);

   /**
    * Finds a scoped classpool registered under the passed in classloader
    * @param the classloader
    * @return the classpool
    */
   ClassPool findClassPool(ClassLoader cl);

   /**
    * Register a classloader
    * 
    * @param ucl the classloader
    * @return the classpool
    */
   ClassPool registerClassLoader(ClassLoader ucl);

   /**
    * Get the registered classloaders
    * 
    * @return the registered classloaders
    */
   Map getRegisteredCLs();

   /**
    * This method will check to see if a register classloader has been undeployed (as in JBoss)
    */
   void clearUnregisteredClassLoaders();

   /**
    * Unregisters a classpool and unregisters its classloader.
    * @ClassLoader the classloader the pool is stored under
    */
   void unregisterClassLoader(ClassLoader cl);
}