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

import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.Iterator;
import java.util.Map;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

/**
 * A scoped class pool.
 * 
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.8 $
 */
public class ScopedClassPool extends ClassPool {
    protected ScopedClassPoolRepository repository;

    protected WeakReference classLoader;

    protected LoaderClassPath classPath;

    protected SoftValueHashMap softcache = new SoftValueHashMap();
    
    boolean isBootstrapCl = true;

    static {
        ClassPool.doPruning = false;
        ClassPool.releaseUnmodifiedClassFile = false;
    }

    /**
     * Create a new ScopedClassPool.
     * 
     * @param cl
     *            the classloader
     * @param src
     *            the original class pool
     * @param repository
     *            the repository
     *@deprecated
     */
    protected ScopedClassPool(ClassLoader cl, ClassPool src,
            ScopedClassPoolRepository repository) {
       this(cl, src, repository, false);
    }
    
    /**
     * Create a new ScopedClassPool.
     * 
     * @param cl
     *            the classloader
     * @param src
     *            the original class pool
     * @param repository
     *            the repository
     * @param isTemp
     *            Whether this is a temporary pool used to resolve references
     */
    protected ScopedClassPool(ClassLoader cl, ClassPool src, ScopedClassPoolRepository repository, boolean isTemp)
    {
       super(src);
       this.repository = repository;
       this.classLoader = new WeakReference(cl);
       if (cl != null) {
           classPath = new LoaderClassPath(cl);
           this.insertClassPath(classPath);
       }
       childFirstLookup = true;
       if (!isTemp && cl == null)
       {
          isBootstrapCl = true;
       }
    }

    /**
     * Get the class loader
     * 
     * @return the class loader
     */
    public ClassLoader getClassLoader() {
       ClassLoader cl = getClassLoader0();
       if (cl == null && !isBootstrapCl)
       {
          throw new IllegalStateException(
                  "ClassLoader has been garbage collected");
       }
       return cl;
    }

    protected ClassLoader getClassLoader0() {
       return (ClassLoader)classLoader.get();
    }

    /**
     * Close the class pool
     */
    public void close() {
        this.removeClassPath(classPath);
        classPath.close();
        classes.clear();
        softcache.clear();
    }

    /**
     * Flush a class
     * 
     * @param classname
     *            the class to flush
     */
    public synchronized void flushClass(String classname) {
        classes.remove(classname);
        softcache.remove(classname);
    }

    /**
     * Soften a class
     * 
     * @param clazz
     *            the class
     */
    public synchronized void soften(CtClass clazz) {
        if (repository.isPrune())
            clazz.prune();
        classes.remove(clazz.getName());
        softcache.put(clazz.getName(), clazz);
    }

    /**
     * Whether the classloader is loader
     * 
     * @return false always
     */
    public boolean isUnloadedClassLoader() {
        return false;
    }

    /**
     * Get the cached class
     * 
     * @param classname
     *            the class name
     * @return the class
     */
    protected CtClass getCached(String classname) {
        CtClass clazz = getCachedLocally(classname);
        if (clazz == null) {
            boolean isLocal = false;

            ClassLoader dcl = getClassLoader0();
            if (dcl != null) {
                final int lastIndex = classname.lastIndexOf('$');
                String classResourceName = null;
                if (lastIndex < 0) {
                    classResourceName = classname.replaceAll("[\\.]", "/")
                            + ".class";
                }
                else {
                    classResourceName = classname.substring(0, lastIndex)
                            .replaceAll("[\\.]", "/")
                            + classname.substring(lastIndex) + ".class";
                }

                isLocal = dcl.getResource(classResourceName) != null;
            }

            if (!isLocal) {
                Map registeredCLs = repository.getRegisteredCLs();
                synchronized (registeredCLs) {
                    Iterator it = registeredCLs.values().iterator();
                    while (it.hasNext()) {
                        ScopedClassPool pool = (ScopedClassPool)it.next();
                        if (pool.isUnloadedClassLoader()) {
                            repository.unregisterClassLoader(pool
                                    .getClassLoader());
                            continue;
                        }

                        clazz = pool.getCachedLocally(classname);
                        if (clazz != null) {
                            return clazz;
                        }
                    }
                }
            }
        }
        // *NOTE* NEED TO TEST WHEN SUPERCLASS IS IN ANOTHER UCL!!!!!!
        return clazz;
    }

    /**
     * Cache a class
     * 
     * @param classname
     *            the class name
     * @param c
     *            the ctClass
     * @param dynamic
     *            whether the class is dynamically generated
     */
    protected void cacheCtClass(String classname, CtClass c, boolean dynamic) {
        if (dynamic) {
            super.cacheCtClass(classname, c, dynamic);
        }
        else {
            if (repository.isPrune())
                c.prune();
            softcache.put(classname, c);
        }
    }

    /**
     * Lock a class into the cache
     * 
     * @param c
     *            the class
     */
    public void lockInCache(CtClass c) {
        super.cacheCtClass(c.getName(), c, false);
    }

    /**
     * Whether the class is cached in this pooled
     * 
     * @param classname
     *            the class name
     * @return the cached class
     */
    protected CtClass getCachedLocally(String classname) {
        CtClass cached = (CtClass)classes.get(classname);
        if (cached != null)
            return cached;
        synchronized (softcache) {
            return (CtClass)softcache.get(classname);
        }
    }

    /**
     * Get any local copy of the class
     * 
     * @param classname
     *            the class name
     * @return the class
     * @throws NotFoundException
     *             when the class is not found
     */
    public synchronized CtClass getLocally(String classname)
            throws NotFoundException {
        softcache.remove(classname);
        CtClass clazz = (CtClass)classes.get(classname);
        if (clazz == null) {
            clazz = createCtClass(classname, true);
            if (clazz == null)
                throw new NotFoundException(classname);
            super.cacheCtClass(classname, clazz, false);
        }

        return clazz;
    }

    /**
     * Convert a javassist class to a java class
     * 
     * @param ct
     *            the javassist class
     * @param loader
     *            the loader
     * @throws CannotCompileException
     *             for any error
     */
    public Class toClass(CtClass ct, ClassLoader loader, ProtectionDomain domain)
            throws CannotCompileException {
        // We need to pass up the classloader stored in this pool, as the
        // default implementation uses the Thread context cl.
        // In the case of JSP's in Tomcat,
        // org.apache.jasper.servlet.JasperLoader will be stored here, while
        // it's parent
        // org.jboss.web.tomcat.tc5.WebCtxLoader$ENCLoader is used as the Thread
        // context cl. The invocation class needs to
        // be generated in the JasperLoader classloader since in the case of
        // method invocations, the package name will be
        // the same as for the class generated from the jsp, i.e.
        // org.apache.jsp. For classes belonging to org.apache.jsp,
        // JasperLoader does NOT delegate to its parent if it cannot find them.
        lockInCache(ct);
        return super.toClass(ct, getClassLoader0(), domain);
    }
}
