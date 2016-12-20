package test.javassist.proxy;

import junit.framework.TestCase;
import testproxy.ProxyTester.ReadWriteData;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

public class ProxySimpleTest extends TestCase {

    String testResult;

    public void testProxyFactory() throws Exception {
        ProxyFactory f = new ProxyFactory();
        f.writeDirectory = "./proxy";
        f.setSuperclass(Foo.class);
        f.setFilter(new MethodFilter() {
            public boolean isHandled(Method m) {
                return m.getName().startsWith("f");
            }
        });
        Class c = f.createClass();
        MethodHandler mi = new MethodHandler() {
            public Object invoke(Object self, Method m, Method proceed,
                                 Object[] args) throws Throwable {
                testResult += args[0].toString();
                return proceed.invoke(self, args);  // execute the original method.
            }
        };
        Foo foo = (Foo)c.getConstructor().newInstance();
        ((Proxy)foo).setHandler(mi);
        testResult = "";
        foo.foo(1);
        foo.foo2(2);
        foo.bar(3);
        assertEquals("12", testResult);
    }

    public static class Foo {
        public int foo(int i) { return i + 1; }
        public int foo2(int i) { return i + 2; }
        public int bar(int i) { return i + 1; }
    }

    public void testReadWrite() throws Exception {
        final String fileName = "read-write.bin";
        ProxyFactory.ClassLoaderProvider cp = ProxyFactory.classLoaderProvider;
        try {
            ProxyFactory.classLoaderProvider = new ProxyFactory.ClassLoaderProvider() {
                public ClassLoader get(ProxyFactory pf) {
                    /* If javassist.Loader is returned, the super type of ReadWriteData class,
                     * which is Serializable, is loaded by javassist.Loader as well as ReadWriteData.
                     * This breaks the implementation of the object serializer.
                     */
                    // return new javassist.Loader();
                    return Thread.currentThread().getContextClassLoader();
                }
            };
            ProxyFactory pf = new ProxyFactory();
            pf.setSuperclass(ReadWriteData.class);
            Object data = pf.createClass().getConstructor().newInstance();
            // Object data = new ReadWriteData();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName));
            oos.writeObject(data);
            oos.close();
        }
        finally {
            ProxyFactory.classLoaderProvider = cp;
        }

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
        Object data2 = ois.readObject();
        ois.close();
        int i = ((ReadWriteData)data2).foo();
        assertEquals(4, i);
    }

    public static class ReadWriteData implements Serializable {
        public int foo() { return 4; }
    }

    public void testWriteReplace() throws Exception {
        ProxyFactory pf = new ProxyFactory();
        pf.setSuperclass(WriteReplace.class);
        Object data = pf.createClass().getConstructor().newInstance();
        assertEquals(data, ((WriteReplace)data).writeReplace());

        ProxyFactory pf2 = new ProxyFactory();
        pf2.setSuperclass(WriteReplace2.class);
        Object data2 = pf2.createClass().getConstructor().newInstance();
        Method meth = data2.getClass().getDeclaredMethod("writeReplace", new Class[0]);
        assertEquals("javassist.util.proxy.SerializedProxy",
                    meth.invoke(data2, new Object[0]).getClass().getName());
    }

    public static class WriteReplace implements Serializable {
        public Object writeReplace() { return this; }
    }

    public static class WriteReplace2 implements Serializable {
        public Object writeReplace(int i) { return Integer.valueOf(i); }
    }

    String value244;

    public void testJIRA244() throws Exception {
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(Extended244.class);
        Extended244 e = (Extended244)factory.create(null, null, new MethodHandler() {
            @Override
            public Object invoke(Object self, Method thisMethod,
                    Method proceed, Object[] args) throws Throwable {
                value244 += thisMethod.getDeclaringClass().getName();
                return proceed.invoke(self);
            }
        });

        value244 = "";
        assertEquals("base", e.base());
        System.out.println(value244);
        assertEquals(Extended244.class.getName(), value244);

        value244 = "";
        assertEquals("ext", e.extended());
        System.out.println(value244);
        assertEquals(Extended244.class.getName(), value244);

        value244 = "";
        assertEquals("base2ext2", e.base2());
        System.out.println(value244);
        assertEquals(Extended244.class.getName(), value244);
    }

    // if Base244 is private, then Extended244 has a bridge method for base().
    private static abstract class Base244 {
        public String base() { return "base"; }
        public String base2() { return "base2"; }
    }

    public static class Extended244 extends Base244 {
        public String extended() { return "ext"; }
        public String base2() { return super.base2() + "ext2"; }
    }

    String valueDefaultMethods = "";

    public void testDefaultMethods() throws Exception {
        valueDefaultMethods = "";
        ProxyFactory f = new ProxyFactory();
        f.writeDirectory = "./proxy";
        f.setSuperclass(Default3.class);
        Class c = f.createClass();
        MethodHandler mi = new MethodHandler() {
            public Object invoke(Object self, Method m, Method proceed,
                                 Object[] args) throws Throwable {
                valueDefaultMethods += "1";
                return proceed.invoke(self, args);  // execute the original method.
            }
        };
        Default3 foo = (Default3)c.getConstructor().newInstance();
        ((Proxy)foo).setHandler(mi);
        foo.foo();
        foo.bar();
        assertEquals("11", valueDefaultMethods);   
    }

    public void testDefaultMethods2() throws Exception {
        valueDefaultMethods = "";
        ProxyFactory f = new ProxyFactory();
        f.writeDirectory = "./proxy";
        f.setInterfaces(new Class[] { Default2.class });
        Class c = f.createClass();
        MethodHandler mi = new MethodHandler() {
            public Object invoke(Object self, Method m, Method proceed,
                                 Object[] args) throws Throwable {
                valueDefaultMethods += "1";
                return proceed.invoke(self, args);  // execute the original method.
            }
        };
        Default2 foo = (Default2)c.getConstructor().newInstance();
        ((Proxy)foo).setHandler(mi);
        foo.foo();
        foo.bar();
        assertEquals("11", valueDefaultMethods);
    }

    public static interface Default1 {
        default int foo() { return 0; }
        default int baz() { return 2; }
    }

    public static interface Default2 extends Default1 {
        default int bar() { return 1; }
    }

    public static class Default3 implements Default2 {
        public int foo() { return Default2.super.foo(); }
    }

    public static class Default4 extends Default3 {
        public int baz() { return super.baz(); }
    }
}
