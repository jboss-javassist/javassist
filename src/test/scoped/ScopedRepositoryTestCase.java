/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */
package scoped;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.LongStream;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.scopedpool.ScopedClassPool;
import javassist.scopedpool.ScopedClassPoolRepository;
import javassist.scopedpool.ScopedClassPoolRepositoryImpl;
import javassist.scopedpool.SoftValueHashMap;
import junit.framework.TestCase;


/**
 * ScopedRepositoryTest.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision$
 */
public class ScopedRepositoryTestCase extends TestCase
{
   private static final ScopedClassPoolRepository repository = ScopedClassPoolRepositoryImpl.getInstance();
   
   public void testJDKClasses() throws Exception
   {
      ClassPool poolClass = repository.findClassPool(Class.class.getClassLoader());
      assertNotNull(poolClass);
      ClassPool poolString = repository.findClassPool(String.class.getClassLoader());
      assertNotNull(poolString);
      assertEquals(poolClass, poolString);
   }
   
   public void testScopedClasses() throws Exception
   {
      ClassLoader cl = getURLClassLoader("test-classes14-jar1");
      ClassPool pool1 = repository.findClassPool(cl);
      CtClass clazz = pool1.get("scoped.jar1.TestClass1");
      assertNotNull(clazz);
      ClassPool poolClass = repository.findClassPool(Class.class.getClassLoader());
      assertNotNull(poolClass);
      assertNotSame(pool1, poolClass);
   }
   
   public void testUnscopedAnnotationUsage() throws Exception
   {
      CtClass clazz = getCtClass(UnscopedAnnotationUsage.class);
      checkTestAnnotation(clazz, "notDefault");
   }
   
   public void testUnscopedAnnotationDefaultUsage() throws Exception
   {
      CtClass clazz = getCtClass(UnscopedAnnotationDefaultUsage.class);
      checkTestAnnotation(clazz, "defaultValue");
   }
   
   public void testScopedAnnotationUsage() throws Exception
   {
      ClassLoader cl = getURLClassLoader("test-classes14-jar1");
      CtClass clazz = getCtClass("scoped.jar1.ScopedAnnotationUsage", cl);
      checkTestAnnotation(clazz, "notDefault");
   }
   
   public void testScopedAnnotationDefaultUsage() throws Exception
   {
      ClassLoader cl = getURLClassLoader("test-classes14-jar1");
      CtClass clazz = getCtClass("scoped.jar1.ScopedAnnotationDefaultUsage", cl);
      checkTestAnnotation(clazz, "defaultValue");
   }
   
   public void testFullyScopedAnnotationUsage() throws Exception
   {
      ClassLoader cl = getURLClassLoader("test-classes14-jar1");
      CtClass clazz = getCtClass("scoped.jar1.FullyScopedAnnotationUsage", cl);
      checkScopedAnnotation(cl, clazz, "notDefault");
   }
   
   public void testFullyScopedAnnotationDefaultUsage() throws Exception
   {
      ClassLoader cl = getURLClassLoader("test-classes14-jar1");
      CtClass clazz = getCtClass("scoped.jar1.FullyScopedAnnotationDefaultUsage", cl);
      checkScopedAnnotation(cl, clazz, "defaultValue");
   }
   
   public void testSoftValueHashMap() throws Exception {
       Map<String,Class<?>> map = new SoftValueHashMap<>();
       Class<?> cls = this.getClass();
       assertTrue(map.put(cls.getName(), cls) == null);
       assertTrue(map.put(cls.getName(), cls) == cls);
       assertTrue(map.size() == 1);
       assertTrue(map.get(cls.getName()) == cls);
       assertTrue(map.values().iterator().next() == cls);
       assertTrue(map.entrySet().iterator().next().getValue() == cls);
       assertTrue(map.containsValue(cls));
       assertTrue(map.remove(cls.getName()) == cls);
       assertTrue(map.size() == 0);
   }

   public void testSoftCache() throws Exception {     
       // Overload the heap to test that the map auto cleans
       Map<String,long[]> map = new SoftValueHashMap<>();
       // 12+8*30000000 = +- 252 MB 
       long[] data = LongStream.range(0, 30000000).toArray();
       int current = map.size();
       while (current <= map.size()) {
           current = map.size();
           for (int ii = 0; ii < 5; ii++) {
               map.put(current+"-"+ii, Arrays.copyOf(data, data.length));
           }
       }
       assertTrue(current > map.size());
   }
   
   protected CtClass getCtClass(Class<?> clazz) throws Exception
   {
      return getCtClass(clazz.getName(), clazz.getClassLoader());
   }
   
   protected CtClass getCtClass(String name, ClassLoader cl) throws Exception
   {
      ClassPool pool = repository.findClassPool(cl);
      assertNotNull(pool);
      CtClass clazz = pool.get(name);
      assertNotNull(clazz);
      return clazz;
   }
   
   protected void checkTestAnnotation(CtClass ctClass, String value) throws Exception
   {
      checkTestAnnotation(ctClass.getAnnotations(), value);
      checkTestAnnotation(getFieldAnnotations(ctClass), value);
      checkTestAnnotation(getConstructorAnnotations(ctClass), value);
      checkTestAnnotation(getConstructorParameterAnnotations(ctClass), value);
      checkTestAnnotation(getMethodAnnotations(ctClass), value);
      checkTestAnnotation(getMethodParameterAnnotations(ctClass), value);
   }
   
   protected void checkTestAnnotation(Object[] annotations, String value) throws Exception
   {
      assertNotNull(annotations);
      assertEquals(1, annotations.length);
      assertNotNull(annotations[0]);
      assertTrue(annotations[0] instanceof TestAnnotation);
      TestAnnotation annotation = (TestAnnotation) annotations[0];
      assertEquals(value, annotation.something());
   }
   
   protected void checkScopedAnnotation(ClassLoader cl, CtClass ctClass, String value) throws Exception
   {
      Class<?> annotationClass = cl.loadClass("scoped.jar1.ScopedTestAnnotation");
      checkScopedAnnotation(annotationClass, ctClass.getAnnotations(), value);
      checkScopedAnnotation(annotationClass, getFieldAnnotations(ctClass), value);
      checkScopedAnnotation(annotationClass, getConstructorAnnotations(ctClass), value);
      checkScopedAnnotation(annotationClass, getConstructorParameterAnnotations(ctClass), value);
      checkScopedAnnotation(annotationClass, getMethodAnnotations(ctClass), value);
      checkScopedAnnotation(annotationClass, getMethodParameterAnnotations(ctClass), value);
   }
   
   protected void checkScopedAnnotation(Class<?> annotationClass, Object[] annotations, String value) throws Exception
   {
      assertNotNull(annotations);
      assertEquals(1, annotations.length);
      assertNotNull(annotations[0]);
      assertTrue(annotationClass.isInstance(annotations[0]));
      
      Method method = annotationClass.getMethod("something", new Class<?>[0]);
      assertEquals(value, method.invoke(annotations[0], (Object[]) null));
   }
   
   protected Object[] getFieldAnnotations(CtClass clazz) throws Exception
   {
      CtField field = clazz.getField("aField");
      assertNotNull(field);
      return field.getAnnotations();
   }
   
   protected Object[] getMethodAnnotations(CtClass clazz) throws Exception
   {
      CtMethod method = clazz.getMethod("doSomething", "(I)V");
      assertNotNull(method);
      return method.getAnnotations();
   }
   
   protected Object[] getMethodParameterAnnotations(CtClass clazz) throws Exception
   {
      CtMethod method = clazz.getMethod("doSomething", "(I)V");
      assertNotNull(method);
      Object[] paramAnnotations = method.getParameterAnnotations();
      assertNotNull(paramAnnotations);
      assertEquals(1, paramAnnotations.length);
      return (Object[]) paramAnnotations[0];
   }
   
   protected Object[] getConstructorAnnotations(CtClass clazz) throws Exception
   {
      CtConstructor constructor = clazz.getConstructor("(I)V");
      assertNotNull(constructor);
      return constructor.getAnnotations();
   }
   
   protected Object[] getConstructorParameterAnnotations(CtClass clazz) throws Exception
   {
      CtConstructor constructor = clazz.getConstructor("(I)V");
      assertNotNull(constructor);
      Object[] paramAnnotations = constructor.getParameterAnnotations();
      assertNotNull(paramAnnotations);
      assertEquals(1, paramAnnotations.length);
      return (Object[]) paramAnnotations[0];
   }
   
   protected ClassLoader getURLClassLoader(String context) throws Exception
   {
      String output = ".";
      File file = new File(output + File.separator + context);
      URL url = file.toURI().toURL();
      return new URLClassLoader(new URL[] { url });
   }
}
