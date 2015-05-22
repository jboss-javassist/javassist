package test.javassist.proxy;

import javassist.util.proxy.*;
import junit.framework.TestCase;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Test to ensure that serialization and deserialization of javassist proxies via
 * {@link javassist.util.proxy.ProxyObjectOutputStream} and  @link javassist.util.proxy.ProxyObjectInputStream}
 * reuses classes located in the proxy cache. This tests the fixes provided for JASSIST-42 and JASSIST-97.
 */
public class ProxySerializationTest extends TestCase
{
    public void testSerialization()
    {
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(TestClass.class);
        factory.setInterfaces(new Class[] {TestInterface.class});

        factory.setUseWriteReplace(true);
        Class proxyClass = factory.createClass(new TestFilter());

        MethodHandler handler = new TestHandler();

        // first try serialization using writeReplace

        try {
            String name = "proxytest_1";
            Constructor constructor = proxyClass.getConstructor(new Class[] {String.class});
            TestClass proxy = (TestClass)constructor.newInstance(new Object[] {name});
            ((ProxyObject)proxy).setHandler(handler);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(proxy);
            out.close();
            byte[] bytes = bos.toByteArray();
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis);
            TestClass newProxy = (TestClass)in.readObject();
            // inherited fields should not have been deserialized
            assertTrue("new name should be null", newProxy.getName() == null);
            // since we are reading into the same JVM the new proxy should have the same class as the old proxy
            assertTrue("classes should be equal", newProxy.getClass() == proxy.getClass());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        // second try serialization using proxy object output/input streams

        factory.setUseWriteReplace(false);
        proxyClass = factory.createClass(new TestFilter());

        try {
            String name = "proxytest_2";
            Constructor constructor = proxyClass.getConstructor(new Class[] {String.class});
            TestClass proxy = (TestClass)constructor.newInstance(new Object[] {name});
            ((ProxyObject)proxy).setHandler(handler);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ProxyObjectOutputStream out = new ProxyObjectOutputStream(bos);
            out.writeObject(proxy);
            out.close();
            byte[] bytes = bos.toByteArray();
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ProxyObjectInputStream in = new ProxyObjectInputStream(bis);
            TestClass newProxy = (TestClass)in.readObject();
            // inherited fields should have been deserialized
            assertTrue("names should be equal", proxy.getName().equals(newProxy.getName()));
            // since we are reading into the same JVM the new proxy should have the same class as the old proxy
            assertTrue("classes should still be equal", newProxy.getClass() == proxy.getClass());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public static class TestFilter implements MethodFilter, Serializable
    {
        public boolean isHandled(Method m) {
            if (m.getName().equals("getName")) {
                return true;
            }
            return false;
        }

        public boolean equals(Object o)
        {
            if (o instanceof TestFilter) {
                // all test filters are equal
                return true;
            }

            return false;
        }

        public int hashCode()
        {
            return TestFilter.class.hashCode();
        }
    }

    public static class TestHandler implements MethodHandler, Serializable
    {
        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
        {
            return proceed.invoke(self, args);
        }
        public boolean equals(Object o)
        {
            if (o instanceof TestHandler) {
                // all test handlers are equal
                return true;
            }

            return false;
        }

        public int hashCode()
        {
            return TestHandler.class.hashCode();
        }
    }

    public static class TestClass implements Serializable
    {
        public String name;

        public TestClass()
        {
        }

        public TestClass(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }
    }

    public static interface TestInterface
    {
        public String getName();
    }
}
