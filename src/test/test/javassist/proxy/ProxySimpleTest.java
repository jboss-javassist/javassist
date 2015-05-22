package test.javassist.proxy;

import junit.framework.TestCase;

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
        Foo foo = (Foo)c.newInstance();
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
        ProxyFactory.classLoaderProvider = new ProxyFactory.ClassLoaderProvider() {
            public ClassLoader get(ProxyFactory pf) {
                return new javassist.Loader();
            }
        };
        ProxyFactory pf = new ProxyFactory();
        pf.setSuperclass(ReadWriteData.class);
        Object data = pf.createClass().newInstance();
        // Object data = new ReadWriteData();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName));
        oos.writeObject(data);
        oos.close();
        ProxyFactory.classLoaderProvider = cp;

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
        Object data = pf.createClass().newInstance();
        assertEquals(data, ((WriteReplace)data).writeReplace());

        ProxyFactory pf2 = new ProxyFactory();
        pf2.setSuperclass(WriteReplace2.class);
        Object data2 = pf2.createClass().newInstance();
        Method meth = data2.getClass().getDeclaredMethod("writeReplace", new Class[0]);
        assertEquals("javassist.util.proxy.SerializedProxy",
                    meth.invoke(data2, new Object[0]).getClass().getName());
    }

    public static class WriteReplace implements Serializable {
        public Object writeReplace() { return this; }
    }

    public static class WriteReplace2 implements Serializable {
        public Object writeReplace(int i) { return new Integer(i); }
    }
}
