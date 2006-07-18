/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2006 Shigeru Chiba. All Rights Reserved.
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

package javassist.bytecode.annotation;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationDefaultAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import java.lang.reflect.*;

/**
 * Internal-use only.  This is a helper class internally used for implementing
 * <code>toAnnotationType()</code> in <code>Annotation</code>.  
 */
public class AnnotationImpl implements InvocationHandler {
    private Annotation annotation;
    private ClassPool pool;
    private ClassLoader classLoader;

    /**
     * Constructs an annotation object.
     *
     * @param cl        class loader for obtaining annotation types.
     * @param clazz     the annotation type.
     * @param cp        class pool for containing an annotation
     *                  type (or null).
     * @param anon      the annotation.
     */
    public static Object make(ClassLoader cl, Class clazz, ClassPool cp,
                              Annotation anon) {
        AnnotationImpl handler = new AnnotationImpl(anon, cp, cl);
        return Proxy.newProxyInstance(cl, new Class[] { clazz }, handler);
    }

    private AnnotationImpl(Annotation a, ClassPool cp, ClassLoader loader) {
        annotation = a;
        pool = cp;
        classLoader = loader;
    }

    /**
     * Obtains the name of the annotation type.
     */
    public String getTypeName() {
        return annotation.getTypeName();
    }

    /**
     * Executes a method invocation on a proxy instance.
     * The implementations of <code>toString</code>, <code>equals</code>,
     * and <code>hashCode</code> are directly supplied by the
     * <code>AnnotationImpl</code>.  The <code>annotationType</code> method
     * is also available on the proxy instance.
     */
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable
    {
        String name = method.getName();
        if (Object.class == method.getDeclaringClass()) {
            if ("equals".equals(name)) {
                Object obj = args[0];
                if (obj == null || obj instanceof Proxy == false)
                    return Boolean.FALSE;

                Object other = Proxy.getInvocationHandler(obj);
                if (this.equals(other))
                    return Boolean.TRUE;
                else
                    return Boolean.FALSE;
            }
            else if ("toString".equals(name))
                return annotation.getTypeName() + '@' + hashCode();
            else if ("hashCode".equals(name))
                return new Integer(hashCode());
        }
        else if ("annotationType".equals(name)
                 && method.getParameterTypes().length == 0)
           return classLoader.loadClass(getTypeName());

        MemberValue mv = annotation.getMemberValue(name);
        if (mv == null)
            return getDefault(name, method);
        else
            return mv.getValue(classLoader, pool, method);
    }

    private Object getDefault(String name, Method method)
        throws ClassNotFoundException, RuntimeException
    {
        String classname = annotation.getTypeName();
        if (pool != null)
            try {
                CtClass cc = pool.get(classname);
                ClassFile cf = cc.getClassFile2();
                MethodInfo minfo = cf.getMethod(name);
                if (minfo != null) {
                    AnnotationDefaultAttribute ainfo
                        = (AnnotationDefaultAttribute)
                          minfo.getAttribute(AnnotationDefaultAttribute.tag);
                    if (ainfo != null) {
                        MemberValue mv = ainfo.getDefaultValue();
                        return mv.getValue(classLoader, pool, method);
                    }
                }
            }
            catch (NotFoundException e) {
                throw new RuntimeException("cannot find a class file: "
                                           + classname);
            }

        throw new RuntimeException("no default value: " + classname + "."
                                   + name + "()");
    }
}
