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

package javassist.bytecode.annotation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationDefaultAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

/**
 * Internal-use only.  This is a helper class internally used for implementing
 * <code>toAnnotationType()</code> in <code>Annotation</code>.
 *   
 * @author Shigeru Chiba
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
 */
public class AnnotationImpl implements InvocationHandler {
    private static final String JDK_ANNOTATION_CLASS_NAME = "java.lang.annotation.Annotation";
    private static Method JDK_ANNOTATION_TYPE_METHOD = null;
   
    private Annotation annotation;
    private ClassPool pool;
    private ClassLoader classLoader;
    private transient Class annotationType;
    private transient int cachedHashCode = Integer.MIN_VALUE;

    static {
        // Try to resolve the JDK annotation type method
        try {
            Class clazz = Class.forName(JDK_ANNOTATION_CLASS_NAME);
            JDK_ANNOTATION_TYPE_METHOD = clazz.getMethod("annotationType", (Class[])null);
        }
        catch (Exception ignored) {
            // Probably not JDK5+
        }
    }
    
    /**
     * Constructs an annotation object.
     *
     * @param cl        class loader for obtaining annotation types.
     * @param clazz     the annotation type.
     * @param cp        class pool for containing an annotation
     *                  type (or null).
     * @param anon      the annotation.
     * @return the annotation
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
     * 
     * @return the type name
     */
    public String getTypeName() {
        return annotation.getTypeName();
    }

    /**
     * Get the annotation type
     * 
     * @return the annotation class
     * @throws NoClassDefFoundError when the class could not loaded
     */
    private Class getAnnotationType() {
        if (annotationType == null) {
            String typeName = annotation.getTypeName();
            try {
                annotationType = classLoader.loadClass(typeName);
            }
            catch (ClassNotFoundException e) {
                NoClassDefFoundError error = new NoClassDefFoundError("Error loading annotation class: " + typeName);
                error.setStackTrace(e.getStackTrace());
                throw error;
            }
        }
        return annotationType;
    }
    
    /**
     * Obtains the internal data structure representing the annotation.
     * 
     * @return the annotation
     */
    public Annotation getAnnotation() {
        return annotation;
    }

    /**
     * Executes a method invocation on a proxy instance.
     * The implementations of <code>toString()</code>, <code>equals()</code>,
     * and <code>hashCode()</code> are directly supplied by the
     * <code>AnnotationImpl</code>.  The <code>annotationType()</code> method
     * is also available on the proxy instance.
     */
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable
    {
        String name = method.getName();
        if (Object.class == method.getDeclaringClass()) {
            if ("equals".equals(name)) {
                Object obj = args[0];
                return new Boolean(checkEquals(obj));
            }
            else if ("toString".equals(name))
                return annotation.toString();
            else if ("hashCode".equals(name))
                return new Integer(hashCode());
        }
        else if ("annotationType".equals(name)
                 && method.getParameterTypes().length == 0)
           return getAnnotationType();

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
        if (pool != null) {
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
        }

        throw new RuntimeException("no default value: " + classname + "."
                                   + name + "()");
    }

    /**
     * Returns a hash code value for this object.
     */
    public int hashCode() {
        if (cachedHashCode == Integer.MIN_VALUE) {
            int hashCode = 0;

            // Load the annotation class
            getAnnotationType();

            Method[] methods = annotationType.getDeclaredMethods();
            for (int i = 0; i < methods.length; ++ i) {
                String name = methods[i].getName();
                int valueHashCode = 0;

                // Get the value
                MemberValue mv = annotation.getMemberValue(name);
                Object value = null;
                try {
                   if (mv != null)
                       value = mv.getValue(classLoader, pool, methods[i]);
                   if (value == null)
                       value = getDefault(name, methods[i]);
                }
                catch (RuntimeException e) {
                    throw e;
                }
                catch (Exception e) {
                    throw new RuntimeException("Error retrieving value " + name + " for annotation " + annotation.getTypeName(), e);
                }

                // Calculate the hash code
                if (value != null) {
                    if (value.getClass().isArray())
                        valueHashCode = arrayHashCode(value);
                    else
                        valueHashCode = value.hashCode();
                } 
                hashCode += 127 * name.hashCode() ^ valueHashCode;
            }
          
            cachedHashCode = hashCode;
        }
        return cachedHashCode;
    }
    
    /**
     * Check that another annotation equals ourselves.
     * 
     * @param obj the other annotation
     * @return the true when equals false otherwise
     * @throws Exception for any problem
     */
    private boolean checkEquals(Object obj) throws Exception {
        if (obj == null)
            return false;

        // Optimization when the other is one of ourselves
        if (obj instanceof Proxy) {
            InvocationHandler ih = Proxy.getInvocationHandler(obj);
            if (ih instanceof AnnotationImpl) {
                AnnotationImpl other = (AnnotationImpl) ih;
                return annotation.equals(other.annotation);
            }
        }

        Class otherAnnotationType = (Class) JDK_ANNOTATION_TYPE_METHOD.invoke(obj, (Object[])null);
        if (getAnnotationType().equals(otherAnnotationType) == false)
           return false;
        
        Method[] methods = annotationType.getDeclaredMethods();
        for (int i = 0; i < methods.length; ++ i) {
            String name = methods[i].getName();

            // Get the value
            MemberValue mv = annotation.getMemberValue(name);
            Object value = null;
            Object otherValue = null;
            try {
               if (mv != null)
                   value = mv.getValue(classLoader, pool, methods[i]);
               if (value == null)
                   value = getDefault(name, methods[i]);
               otherValue = methods[i].invoke(obj, (Object[])null);
            }
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new RuntimeException("Error retrieving value " + name + " for annotation " + annotation.getTypeName(), e);
            }

            if (value == null && otherValue != null)
                return false;
            if (value != null && value.equals(otherValue) == false)
                return false;
        }
        
        return true;
    }

    /**
     * Calculates the hashCode of an array using the same
     * algorithm as java.util.Arrays.hashCode()
     * 
     * @param object the object
     * @return the hashCode
     */
    private static int arrayHashCode(Object object)
    {
       if (object == null)
          return 0;

       int result = 1;
       
       Object[] array = (Object[]) object;
       for (int i = 0; i < array.length; ++i) {
           int elementHashCode = 0;
           if (array[i] != null)
              elementHashCode = array[i].hashCode();
           result = 31 * result + elementHashCode;
       }
       return result;
    }
}
