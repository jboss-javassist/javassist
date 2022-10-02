package testproxy;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.Assert;

import java.lang.reflect.InvocationTargetException;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;
import javassist.util.proxy.Proxy;
import junit.framework.TestCase;
import java.io.*;

@SuppressWarnings({"unchecked", "rawtypes","unused"})
public class ProxyTester extends TestCase {
    public ProxyTester(String s) {
        super(s);
    }

    public ProxyTester() {
        this("proxy");
    }

    static class Interceptor1 implements MethodHandler {
        int counter = 0;

        public Object invoke(Object self, Method m, Method proceed,
                Object[] args) throws Exception {
            System.out.println("intercept: " + m + ", proceed: " + proceed);
            System.out.println("     modifier: "
                               + Modifier.toString(proceed.getModifiers()));
            counter++;
            return proceed.invoke(self, args);
        }
    }

    static class Interceptor2 implements MethodHandler {
        int counter = 0;
        public Object invoke(Object self, Method m, Method proceed,
                             Object[] args) throws Exception {
            System.out.println("intercept: " + m + ", proceed: " + proceed);
            counter++;
            if (proceed != null)
                return proceed.invoke(self, args);
            else
                if (m.getReturnType() == int.class)
                    return Integer.valueOf(3);
                else
                    return "OK";
        }
    }

    static MethodFilter finalizeRemover = new MethodFilter() {
        public boolean isHandled(Method m) {
            return !m.getName().equals("finalize");
        }
    };

    public void testTarget() throws Exception {
        ProxyFactory f = new ProxyFactory();
        f.setSuperclass(Target.class);
        Interceptor1 interceptor = new Interceptor1();
        // f.setHandler(interceptor);
        f.setFilter(finalizeRemover);
        f.writeDirectory = ".";
        Class c = f.createClass();
        Target obj = (Target)c.getConstructor().newInstance();
        ((Proxy)obj).setHandler(interceptor);
        obj.m();
        assertEquals(true, obj.m(true));
        assertEquals((byte)1, obj.m1((byte)1));
        assertEquals('a', obj.m2('a'));
        assertEquals((short)2, obj.m3((short)2));
        assertEquals(3, obj.m(3));
        assertEquals(4L, obj.m5(4L));
        assertTrue(5.0F == obj.m6(5.0F));
        assertTrue(6.0 == obj.m7(6.0));
        assertEquals("test", obj.m("test"));
        int[] ia = { 1, 2, 3 };
        assertEquals(ia, obj.m7(ia));
        String[] sa = { "1", "2" };
        assertEquals(sa, obj.m8(sa));
        assertEquals(obj, obj.m9(3, obj, null));
        assertEquals(14, interceptor.counter);
    }

    public void testTarget1() throws Exception {
        ProxyFactory f = new ProxyFactory();
        f.setSuperclass(Target1.class);
        Interceptor1 interceptor = new Interceptor1();
        // f.setHandler(interceptor);
        f.setFilter(finalizeRemover);
        Class c = f.createClass();
        Target1 obj = (Target1)c.getConstructor().newInstance();
        ((Proxy)obj).setHandler(interceptor);
        assertEquals(null, obj.m(null));
        assertEquals(1, interceptor.counter);
    }

    public void testObject() throws Exception {
        ProxyFactory f = new ProxyFactory();
        Interceptor1 interceptor = new Interceptor1();
        // f.setHandler(interceptor);
        f.setFilter(finalizeRemover);
        Class c = f.createClass();
        Object obj = (Object)c.getConstructor().newInstance();
        ((Proxy)obj).setHandler(interceptor);
        System.out.println(obj.toString());
        assertEquals(2, interceptor.counter);
    }

    public void testSetter() throws Exception {
        ProxyFactory f = new ProxyFactory();
        f.writeDirectory = ".";
        Interceptor1 interceptor = new Interceptor1();
        // f.setHandler(interceptor);
        f.setFilter(finalizeRemover);
        Class c = f.createClass();
        Object obj = (Object)c.getConstructor().newInstance();
        ((Proxy)obj).setHandler(interceptor);
        System.out.println("setter1: " + obj.toString());
        ((ProxyObject)obj).setHandler(new MethodHandler() {
            public Object invoke(Object self, Method m, Method proceed,
                    Object[] args) throws Exception {
                System.out.print("intercept: " + m);
                return "OK";
            }
        });
        assertEquals("OK", obj.toString());
    }

    public void testString() throws Exception {
        ProxyFactory f = new ProxyFactory();
        Interceptor1 interceptor = new Interceptor1();
        // f.setHandler(interceptor);
        f.setFilter(finalizeRemover);
        f.setSuperclass(String.class);
        try {
            Class c = f.createClass();
            Assert.fail("String is final!");
        }
        catch (RuntimeException e) {
            System.out.println(e);
        }
    }

    public void testConstructor() throws Exception {
        ProxyFactory f = new ProxyFactory();
        Interceptor1 interceptor = new Interceptor1();
        // f.setHandler(interceptor);
        f.setFilter(finalizeRemover);
        f.setSuperclass(Target2.class);
        Class c = f.createClass();
        Constructor[] cons = c.getDeclaredConstructors();
        assertEquals(3, cons.length);
        Constructor con1 = c.getDeclaredConstructor(new Class[] { int.class });
        Constructor con2 = c.getDeclaredConstructor(new Class[] { int.class, int.class });
        Method m1 = c.getDeclaredMethod("get", new Class[0]);
        Method m2 = c.getDeclaredMethod("foo", new Class[0]);
        assertEquals(0, m1.getExceptionTypes().length);
        assertEquals("java.io.IOException", m2.getExceptionTypes()[0].getName());

        Target2 t2 = (Target2)con1.newInstance(new Object[] { Integer.valueOf(1) });
        ((Proxy)t2).setHandler(interceptor);
        System.out.println(t2.toString());
        assertEquals(2, interceptor.counter);

        interceptor.counter = 0;
        assertEquals(2, t2.foo());
        assertEquals(4, t2._dfoo());
        assertEquals(2, interceptor.counter);
    }

    public void testInterface() throws Exception {
        ProxyFactory f = new ProxyFactory();
        Interceptor2 interceptor2 = new Interceptor2();
        // f.setHandler(interceptor2);
        f.setFilter(finalizeRemover);
        f.setInterfaces(new Class[] { Target3.class });
        Class c = f.createClass();
        Target3 obj = (Target3)c.getConstructor().newInstance();
        ((Proxy)obj).setHandler(interceptor2);
        assertEquals("OK", obj.m());
        System.out.println(obj.toString());
        assertEquals(3, interceptor2.counter);
    }

    public void test2Interfaces() throws Exception {
        ProxyFactory f = new ProxyFactory();
        Interceptor2 interceptor2 = new Interceptor2();
        // f.setHandler(interceptor2);
        f.setFilter(finalizeRemover);
        f.setInterfaces(new Class[] { Target3.class, Target4.class });
        Class c = f.createClass();
        Target3 obj = (Target3)c.getConstructor().newInstance();
        ((Proxy)obj).setHandler(interceptor2);
        assertEquals("OK", obj.m());
        System.out.println(obj.toString());
        assertEquals(3, interceptor2.counter);

        interceptor2.counter = 0;
        Target4 obj4 = (Target4)c.getConstructor().newInstance();
        ((Proxy)obj4).setHandler(interceptor2);
        assertEquals(3, obj4.bar4());
        assertEquals(3, obj4.foo4());
        assertEquals(2, interceptor2.counter);
    }

    public void testFilter() throws Exception {
        ProxyFactory f = new ProxyFactory();
        Interceptor2 interceptor2 = new Interceptor2();
        // f.setHandler(interceptor2);
        f.setFilter(finalizeRemover);
        f.setInterfaces(new Class[] { Target3.class });
        f.setFilter(new MethodFilter() {
                public boolean isHandled(Method m) {
                    return m.getDeclaringClass() != Object.class;
                }
            });
        Class c = f.createClass();
        Target3 obj = (Target3)c.getConstructor().newInstance();
        ((Proxy)obj).setHandler(interceptor2);
        assertEquals("OK", obj.m());
        System.out.println(obj.toString());
        assertEquals(1, interceptor2.counter);
    }

    public static boolean testInitFlag;

    public void testInit() throws Exception {
        ProxyFactory f = new ProxyFactory();
        f.setSuperclass(TargetInit.class);
        MethodHandler handler = new MethodHandler() {
            public Object invoke(Object self, Method m,
                    Method proceed, Object[] args) throws Exception {
                System.out.println("testInit " + testInitFlag);
                return proceed.invoke(self, args);
            }
        };
        testInitFlag = false;
        Class c = f.createClass();
        assertTrue(testInitFlag); // since 3.12.  Before then, this line was assertFalse(testInitFlag);
        System.out.println("testInit createClass(): " + testInitFlag);
        TargetInit obj = (TargetInit)c.getConstructor().newInstance();
        assertTrue(testInitFlag);
        System.out.println("testInit newInstance(): " + testInitFlag);
        ((ProxyObject)obj).setHandler(handler);
        assertEquals("OK", obj.m());
    }

    public void testCreate() throws Exception {
        ProxyFactory f = new ProxyFactory();
        f.setSuperclass(Target5.class);
        Interceptor1 interceptor = new Interceptor1();
        // f.setHandler(interceptor);
        f.setFilter(finalizeRemover);
        Class c = f.createClass();
        Target5 obj = (Target5)f.create(new Class[] { int.class }, new Object[] { Integer.valueOf(3) });
        ((Proxy)obj).setHandler(interceptor);
        assertEquals(3, obj.get());
    }


    public void testBridgeMethod() throws Exception {
        ProxyFactory f = new ProxyFactory();
        f.writeDirectory = ".";
        f.setSuperclass(BridgeMethod.class);
        Interceptor1 interceptor = new Interceptor1();
        // f.setHandler(interceptor);
        f.setFilter(finalizeRemover);
        Class c = f.createClass();
        BridgeMethod obj = (BridgeMethod)c.getConstructor().newInstance();
        ((Proxy)obj).setHandler(interceptor);
        Integer value = obj.m1();
        assertEquals(7, value.intValue());
        BridgeMethodInf inf = (BridgeMethodInf)obj;
        Number num = inf.m1();
        assertEquals(7, num.intValue());
        BridgeMethodSuper sup = obj;
        try {
            Object x = sup.id(new Object());
            fail("not cast error");
        }
        catch (ClassCastException e) {}
        catch (Exception e) {
            if (e instanceof InvocationTargetException)
                if (e.getCause() instanceof ClassCastException)
                    return;

            throw e;
        }
    }

    public void testGetters() throws Exception {
        ProxyFactory f = new ProxyFactory();
        Class c = ProxyTester.class;
        f.setSuperclass(c);
        assertEquals(c, f.getSuperclass());
        Class i = java.io.Serializable.class;
        f.setInterfaces(new Class[] { i });
        assertEquals(i, f.getInterfaces()[0]);
    }

    static class ProxyFactory2 extends ProxyFactory {
        public ClassLoader getClassLoader2() {
            return getClassLoader();
        }
    }

    public void testProvider() throws Exception {
        ProxyFactory.ClassLoaderProvider cp = ProxyFactory.classLoaderProvider;
        try {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            ProxyFactory.classLoaderProvider = new ProxyFactory.ClassLoaderProvider() {
                public ClassLoader get(ProxyFactory pf) {
                    return Thread.currentThread().getContextClassLoader();
                }
            };

            ProxyFactory2 pf = new ProxyFactory2();
            assertEquals(cl, pf.getClassLoader2());
        }
        finally {
            ProxyFactory.classLoaderProvider = cp;
        }
    }

    @SuppressWarnings("deprecation")
	public void testCache() throws Exception {
        boolean prev = ProxyFactory.useCache;
        ProxyFactory.useCache = true;
        ProxyFactory f = new ProxyFactory();
        f.setSuperclass(Cache1.class);
        Class c = f.createClass();
        ProxyFactory f2 = new ProxyFactory();
        f2.setSuperclass(Cache1.class);
        assertEquals(c, f2.createClass());
        ProxyFactory f3 = new ProxyFactory();
        f3.setSuperclass(Cache1.class);
        f3.setHandler(new Interceptor1());	// deprecated
        assertFalse(c == f3.createClass());
        ProxyFactory.useCache = true;
        ProxyFactory f4 = new ProxyFactory();
        f4.setSuperclass(Cache1.class);
        f4.setInterfaces(new Class[] { Cache2.class });
        Class c4 = f4.createClass();
        assertFalse(c == c4);
        ProxyFactory f5 = new ProxyFactory();
        f5.setSuperclass(Cache1.class);
        f5.setInterfaces(new Class[] { Cache2.class });
        assertEquals(c4, f5.createClass());
        ProxyFactory f6 = new ProxyFactory();
        f6.setInterfaces(new Class[] { Cache2.class });
        assertFalse(c4 == f6.createClass());
        ProxyFactory.useCache = prev;
    }

    public static class Cache1 {
        public int foo() { return 0; }
    }

    public static interface Cache2 {
        public int bar();
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
            //Object data = new ReadWriteData();
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
        /** default serialVersionUID */
        private static final long serialVersionUID = 1L;

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
        /** default serialVersionUID */
        private static final long serialVersionUID = 1L;

        public Object writeReplace() { return this; }
    }

    public static class WriteReplace2 implements Serializable {
        /** default serialVersionUID */
        private static final long serialVersionUID = 1L;

        public Object writeReplace(int i) { return Integer.valueOf(i); }
    }

    public static void testJIRA189() throws Exception {
        Class persistentClass = Target189.PublishedArticle.class;
        ProxyFactory factory = new ProxyFactory();
        //factory.writeDirectory = ".";
        factory.setUseCache(false);
        factory.setSuperclass(persistentClass);
        factory.setInterfaces(new Class[] { Target189.TestProxy.class });
        Class cl = factory.createClass();
        Object obj = cl.getConstructor().newInstance();
        System.out.println("JIRA189:" + obj.getClass().getClassLoader() + ", " + obj.getClass().getSuperclass().getName()
                            + ", " + Target189.PublishedArticle.class.getClassLoader());
        Target189.TestProxy proxy = (Target189.TestProxy)cl.getConstructor().newInstance();
        Target189.TestMethodHandler methodHandler = new Target189.TestMethodHandler();
        ((ProxyObject)proxy).setHandler(methodHandler);
        ((Target189.Article)proxy).getIssue();
        assertTrue(methodHandler.wasInvokedOnce());
        methodHandler.reset();
        Target189.PublishedArticle article = (Target189.PublishedArticle)proxy;
        article.getIssue();
        assertTrue(methodHandler.wasInvokedOnce());
    }

    public void testJIRA127() throws Exception {
        ProxyFactory proxyFactory = new ProxyFactory();
        // proxyFactory.writeDirectory = ".";
        proxyFactory.setInterfaces(new Class[]{ Target127.Sub.class });
        Target127.Sub proxy = (Target127.Sub)proxyFactory.create(new Class[0], new Object[0], new MethodHandler() {
            public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
                return null;
            }
        });
        ((Target127.Super)proxy).item();    // proxyFactory must generate a bridge method.
        ((Target127.Sub)proxy).item();
    }

    public static void main(String[] args) {
        // javassist.bytecode.ClassFile.MAJOR_VERSION = javassist.bytecode.ClassFile.JAVA_6;
        junit.textui.TestRunner.run(ProxyTester.class);
    }
}
