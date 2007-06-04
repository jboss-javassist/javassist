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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javassist.ClassPool;
import javassist.LoaderClassPath;

/**
 * An implementation of <code>ScopedClassPoolRepository</code>.
 * It is an singleton.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.4 $
 */
public class ScopedClassPoolRepositoryImpl implements ScopedClassPoolRepository {
    /** The instance */
    private static final ScopedClassPoolRepositoryImpl instance = new ScopedClassPoolRepositoryImpl();

    /** Whether to prune */
    private boolean prune = true;

    /** Whether to prune when added to the classpool's cache */
    boolean pruneWhenCached;

    /** The registered classloaders */
    protected Map registeredCLs = Collections
            .synchronizedMap(new WeakHashMap());

    /** The default class pool */
    protected ClassPool classpool;

    /** The factory for creating class pools */
    protected ScopedClassPoolFactory factory = new ScopedClassPoolFactoryImpl();

    /**
     * Get the instance.
     * 
     * @return the instance.
     */
    public static ScopedClassPoolRepository getInstance() {
        return instance;
    }

    /**
     * Singleton.
     */
    private ScopedClassPoolRepositoryImpl() {
        classpool = ClassPool.getDefault();
        // FIXME This doesn't look correct
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        classpool.insertClassPath(new LoaderClassPath(cl));
    }

    /**
     * Returns the value of the prune attribute.
     * 
     * @return the prune.
     */
    public boolean isPrune() {
        return prune;
    }

    /**
     * Set the prune attribute.
     * 
     * @param prune     a new value.
     */
    public void setPrune(boolean prune) {
        this.prune = prune;
    }

    /**
     * Create a scoped classpool.
     * 
     * @param cl    the classloader.
     * @param src   the original classpool.
     * @return the classpool
     */
    public ScopedClassPool createScopedClassPool(ClassLoader cl, ClassPool src) {
        return factory.create(cl, src, this);
    }

    public ClassPool findClassPool(ClassLoader cl) {
        if (cl == null)
            return registerClassLoader(ClassLoader.getSystemClassLoader());

        return registerClassLoader(cl);
    }

    /**
     * Register a classloader.
     * 
     * @param ucl       the classloader.
     * @return the classpool
     */
    public ClassPool registerClassLoader(ClassLoader ucl) {
        synchronized (registeredCLs) {
            // FIXME: Probably want to take this method out later
            // so that AOP framework can be independent of JMX
            // This is in here so that we can remove a UCL from the ClassPool as
            // a
            // ClassPool.classpath
            if (registeredCLs.containsKey(ucl)) {
                return (ClassPool)registeredCLs.get(ucl);
            }
            ScopedClassPool pool = createScopedClassPool(ucl, classpool);
            registeredCLs.put(ucl, pool);
            return pool;
        }
    }

    /**
     * Get the registered classloaders.
     */
    public Map getRegisteredCLs() {
        clearUnregisteredClassLoaders();
        return registeredCLs;
    }

    /**
     * This method will check to see if a register classloader has been
     * undeployed (as in JBoss)
     */
    public void clearUnregisteredClassLoaders() {
        ArrayList toUnregister = null;
        synchronized (registeredCLs) {
            Iterator it = registeredCLs.values().iterator();
            while (it.hasNext()) {
                ScopedClassPool pool = (ScopedClassPool)it.next();
                if (pool.isUnloadedClassLoader()) {
                    it.remove();
                    ClassLoader cl = pool.getClassLoader();
                    if (cl != null) {
                        if (toUnregister == null) {
                            toUnregister = new ArrayList();
                        }
                        toUnregister.add(cl);
                    }
                }
            }
            if (toUnregister != null) {
                for (int i = 0; i < toUnregister.size(); i++) {
                    unregisterClassLoader((ClassLoader)toUnregister.get(i));
                }
            }
        }
    }

    public void unregisterClassLoader(ClassLoader cl) {
        synchronized (registeredCLs) {
            ScopedClassPool pool = (ScopedClassPool)registeredCLs.remove(cl);
            if (pool != null)
                pool.close();
        }
    }

    public void insertDelegate(ScopedClassPoolRepository delegate) {
        // Noop - this is the end
    }

    public void setClassPoolFactory(ScopedClassPoolFactory factory) {
        this.factory = factory;
    }

    public ScopedClassPoolFactory getClassPoolFactory() {
        return factory;
    }
}
