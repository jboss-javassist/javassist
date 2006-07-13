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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javassist.ClassPool;
import javassist.LoaderClassPath;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ScopedClassPoolRepositoryImpl implements ScopedClassPoolRepository
{
   /** The instance */
   private static final ScopedClassPoolRepositoryImpl instance = new ScopedClassPoolRepositoryImpl();

   /** Whether to prune */
   private boolean prune = true;

   /** Whether to prune when added to the classpool's cache */
   boolean pruneWhenCached;

   /** The registered classloaders */
   protected Map registeredCLs = Collections.synchronizedMap(new WeakHashMap());
   
   /** The default class pool */
   protected ClassPool classpool;
   
   /** The factory for creating class pools */
   protected ScopedClassPoolFactory factory = new ScopedClassPoolFactoryImpl();

   /**
    * Get the instance
    * 
    * @return the instance
    */
   public static ScopedClassPoolRepository getInstance()
   {
      return instance;
   }

   /**
    * Singleton
    */
   private ScopedClassPoolRepositoryImpl()
   {
      classpool = ClassPool.getDefault();
      // FIXME This doesn't look correct 
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      classpool.insertClassPath(new LoaderClassPath(cl));
   }
   
   /**
    * Get the prune.
    * 
    * @return the prune.
    */
   public boolean isPrune()
   {
      return prune;
   }

   /**
    * Set the prune.
    * 
    * @param prune the prune.
    */
   public void setPrune(boolean prune)
   {
      this.prune = prune;
   }

   /**
    * Create a scoped classpool
    * 
    * @param cl the classloader
    * @param src the original classpool
    * @return the classpool
    */
   public ScopedClassPool createScopedClassPool(ClassLoader cl, ClassPool src)
   {
      return factory.create(cl, src, this);
   }

   public ClassPool findClassPool(ClassLoader cl)
   {
      if (cl == null)
         return registerClassLoader(ClassLoader.getSystemClassLoader());
      return registerClassLoader(cl);
   }

   /**
    * Register a classloader
    * 
    * @param ucl the classloader
    * @return the classpool
    */
   public ClassPool registerClassLoader(ClassLoader ucl)
   {
      synchronized (registeredCLs)
      {
         // FIXME: Probably want to take this method out later
         // so that AOP framework can be independent of JMX
         // This is in here so that we can remove a UCL from the ClassPool as a
         // ClassPool.classpath
         if (registeredCLs.containsKey(ucl))
         {
            return (ClassPool) registeredCLs.get(ucl);
         }
         ScopedClassPool pool = createScopedClassPool(ucl, classpool);
         registeredCLs.put(ucl, pool);
         return pool;
      }
   }

   /**
    * Get the registered classloaders
    * 
    * @return the registered classloaders
    */
   public Map getRegisteredCLs()
   {
      clearUnregisteredClassLoaders();
      return registeredCLs;
   }

   /**
    * This method will check to see if a register classloader has been undeployed (as in JBoss)
    */
   public void clearUnregisteredClassLoaders()
   {
      ArrayList toUnregister = null;
      synchronized (registeredCLs)
      {
         Iterator it = registeredCLs.values().iterator();
         while (it.hasNext())
         {
            ScopedClassPool pool = (ScopedClassPool) it.next();
            if (pool.isUnloadedClassLoader())
            {
               it.remove();
               ClassLoader cl = pool.getClassLoader();
               if (cl != null)
               {
                  if (toUnregister == null)
                  {
                     toUnregister = new ArrayList();
                  }
                  toUnregister.add(cl);
               }
            }
         }
         if (toUnregister != null)
         {
            for (int i = 0; i < toUnregister.size(); i++)
            {
               unregisterClassLoader((ClassLoader) toUnregister.get(i));
            }
         }
      }
   }

   public void unregisterClassLoader(ClassLoader cl)
   {
      synchronized (registeredCLs)
      {
         ScopedClassPool pool = (ScopedClassPool) registeredCLs.remove(cl);
         if (pool != null) pool.close();
      }
   }

   public void insertDelegate(ScopedClassPoolRepository delegate)
   {
      //Noop - this is the end
   }

   public void setClassPoolFactory(ScopedClassPoolFactory factory)
   {
      this.factory = factory;
   }
   
   public ScopedClassPoolFactory getClassPoolFactory()
   {
      return factory;
   }
}
