package test.javassist.proxy;

import javassist.*;
import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import junit.framework.TestCase;

/**
 * test which checks that proxy classes are not retained after their classloader is released.
 * this is a before and after test which validates JASSIST-104
 */
public class ProxyCacheGCTest extends TestCase
{
    /**
     * creates a large number of proxies in separate classloaders then lets go of the classloaders and
     * forces a GC. If we run out of heap then we know there is a problem.
     */

    public final static int REPETITION_COUNT = 10000;
    private ClassPool basePool;
    private CtClass baseHandler;
    private CtClass baseFilter;

    protected void setUp()
    {
        basePool = ClassPool.getDefault();
        try {
            baseHandler = basePool.get("javassist.util.proxy.MethodHandler");
            baseFilter = basePool.get("javassist.util.proxy.MethodFilter");
        } catch (NotFoundException e) {
            e.printStackTrace();
            fail("could not find class " + e);
        }
    }

    public void testCacheGC()
    {
        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        try {
        ProxyFactory.useCache = false;
        for (int i = 0; i < REPETITION_COUNT; i++) {
            ClassLoader newCL = new TestLoader();
            try {
                Thread.currentThread().setContextClassLoader(newCL);
                createProxy(i);
            } finally {
                Thread.currentThread().setContextClassLoader(oldCL);
            }
        }
        } finally {
            ProxyFactory.useCache = true;
        }
    }

    /**
     * called when a specific classloader is in place on the thread to create a method handler, method filter
     * and proxy in the current loader and then
     */
    public void createProxy(int counter)
    {
        try {
            ClassPool classPool = new ClassPool(basePool);

            // create a target class in the current class loader
            String targetName = "test.javassist.MyTarget_" + counter;
            String targetConstructorName = "MyTarget_" + counter;
            CtClass ctTargetClass =  classPool.makeClass(targetName);
            CtMethod targetMethod = CtNewMethod.make("public Object test() { return this; }", ctTargetClass);
            ctTargetClass.addMethod(targetMethod);
            CtConstructor targetConstructor = CtNewConstructor.make("public " + targetConstructorName + "() { }", ctTargetClass);
            ctTargetClass.addConstructor(targetConstructor);

            // create a handler in the current classloader
            String handlerName = "test.javassist.MyHandler_" + counter;
            CtClass ctHandlerClass =  classPool.makeClass(handlerName);
            ctHandlerClass.addInterface(baseHandler);
            CtMethod handlerInvoke = CtNewMethod.make("public Object invoke(Object self, java.lang.reflect.Method thisMethod, java.lang.reflect.Method proceed, Object[] args) throws Throwable { return proceed.invoke(self, args); }", ctHandlerClass);
            ctHandlerClass.addMethod(handlerInvoke);

            // create a filter in the current classloader
            String filterName = "test.javassist.MyFilter" + counter;
            CtClass ctFilterClass =  classPool.makeClass(filterName);
            ctFilterClass.addInterface(baseFilter);
            CtMethod filterIsHandled = CtNewMethod.make("public boolean isHandled(java.lang.reflect.Method m) { return true; }", ctFilterClass);
            ctFilterClass.addMethod(filterIsHandled);

            // now create a proxyfactory and use it to create a proxy

            ProxyFactory factory = new ProxyFactory();
            Class javaTargetClass = classPool.toClass(ctTargetClass);
            Class javaHandlerClass = classPool.toClass(ctHandlerClass);
            Class javaFilterClass = classPool.toClass(ctFilterClass);

            MethodHandler handler= (MethodHandler)javaHandlerClass.newInstance();
            MethodFilter filter = (MethodFilter)javaFilterClass.newInstance();

            // ok, now create a factory and a proxy class and proxy from that factory
            factory.setFilter(filter);
            factory.setSuperclass(javaTargetClass);
            // factory.setSuperclass(Object.class);

            Class proxyClass = factory.createClass();
            Object target = proxyClass.newInstance();
            ((ProxyObject)target).setHandler(handler);
        } catch (Exception e) {
            e.printStackTrace();
            fail("cannot create proxy " + e);
        }

    }

    /**
     * a classloader which inherits from the system class loader and within which a proxy handler,
     * filter and proxy will be located.
     */
    public static class TestLoader extends ClassLoader
    {
    }
}
