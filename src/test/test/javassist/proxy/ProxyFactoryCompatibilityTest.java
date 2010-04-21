package test.javassist.proxy;

import javassist.*;
import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import junit.framework.TestCase;

import java.lang.reflect.Method;

/**
 * test which checks that it is still possible to use the old style proxy factory api
 * to create proxy classes which set their own handler. it checks that caching is
 * automatically disabled if this legacy api is used. it also exercises the new style
 * api, ensuring that caching works correctly with this model.
 */
public class ProxyFactoryCompatibilityTest extends TestCase
{
    private ClassPool basePool;
    MethodFilter filter;
    MethodHandler handler;

    protected void setUp()
    {
        basePool = ClassPool.getDefault();
        filter =  new MethodFilter() {
            public boolean isHandled(Method m) {
                return !m.getName().equals("finalize");
            }
        };

        handler = new MethodHandler() {
            public Object invoke(Object self, Method m, Method proceed,
                                 Object[] args) throws Throwable {
                System.out.println("calling: " + m.getName());
                return proceed.invoke(self, args);  // execute the original method.
            }
        };
    }

    public void testFactoryCompatibility() throws Exception
    {
        System.out.println("ProxyFactory.useCache = " + ProxyFactory.useCache);
        // create a factory which, by default, uses caching
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(TestClass.class);
        factory.setInterfaces(new Class[] { TestInterface.class});
        factory.setFilter(filter);

        // create the same class twice and check that it is reused
        Class proxyClass1 =  factory.createClass();
        System.out.println("created first class " + proxyClass1.getName());
        TestClass proxy1 = (TestClass)proxyClass1.newInstance();
        ((ProxyObject) proxy1).setHandler(handler);
        proxy1.testMethod();
        assertTrue(proxy1.isTestCalled());

        Class proxyClass2 =  factory.createClass();
        System.out.println("created second class " + proxyClass2.getName());
        TestClass proxy2 = (TestClass)proxyClass2.newInstance();
        ((ProxyObject) proxy2).setHandler(handler);
        proxy2.testMethod();
        assertTrue(proxy2.isTestCalled());

        assertTrue(proxyClass1 == proxyClass2);

        // create a factory which, by default, uses caching then set the handler so it creates
        // classes which do not get cached.
        ProxyFactory factory2 = new ProxyFactory();
        factory.setSuperclass(TestClass.class);
        factory.setInterfaces(new Class[] { TestInterface.class});
        factory.setFilter(filter);
        factory.setHandler(handler);

        // create the same class twice and check that it is reused
        Class proxyClass3 =  factory.createClass();
        System.out.println("created third class " + proxyClass3.getName());
        TestClass proxy3 = (TestClass)proxyClass3.newInstance();
        proxy3.testMethod();
        assertTrue(proxy3.isTestCalled());

        Class proxyClass4 =  factory.createClass();
        System.out.println("created fourth class " + proxyClass4.getName());
        TestClass proxy4 = (TestClass)proxyClass4.newInstance();
        proxy4.testMethod();
        assertTrue(proxy4.isTestCalled());

        assertTrue(proxyClass3 != proxyClass4);
    }

    /**
     * test class used as the super for the proxy
     */
    public static class TestClass {
        private boolean testCalled = false;
        public void testMethod()
        {
            // record the call
            testCalled = true;
        }
        public boolean isTestCalled()
        {
            return testCalled;
        }
    }

    /**
     * test interface used as an interface implemented by the proxy
     */
    public static interface TestInterface {
        public void testMethod();
    }

}