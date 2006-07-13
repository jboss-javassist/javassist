/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package javassist.scopedpool;

import java.util.Map;

import javassist.ClassPool;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
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