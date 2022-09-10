package test.javassist.proxy;

import java.lang.reflect.Method;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyObject;
import javassist.util.proxy.Proxy;
import junit.framework.TestCase;

@SuppressWarnings({"rawtypes","unchecked"})
public class JBPAPP9257Test extends TestCase {
    public void testGetHandler() throws Exception {
        ProxyFactory f = new ProxyFactory();
        f.setSuperclass(Foo.class);
        f.setFilter(new MethodFilter() {
            public boolean isHandled(Method m) {
                // ignore finalize()
                return !m.getName().equals("finalize");
            }
        });
        Class c = f.createClass();
        MethodHandler mi = new MethodHandler() {
            public Object invoke(Object self, Method m, Method proceed,
                    Object[] args) throws Throwable {
                System.out.println("Name: " + m.getName());
                return proceed.invoke(self, args) + "!"; // execute the original
                // method.
            }
        };
        Foo foo = (Foo)c.getConstructor().newInstance();
        try {
            ((ProxyObject)foo).setHandler(mi);
            fail("foo is a ProxyObject!");
        } catch (ClassCastException e) {}
        ((Proxy)foo).setHandler(mi);
        assertEquals("I'm doing something!", foo.doSomething());
        assertEquals("This is a secret handler!", foo.getHandler());
    }

    public void testGetHandler2() throws Exception {
        ProxyFactory f = new ProxyFactory();
        f.setSuperclass(Foo2.class);
        f.setFilter(new MethodFilter() {
            public boolean isHandled(Method m) {
                // ignore finalize()
                return !m.getName().equals("finalize");
            }
        });
        Class c = f.createClass();
        MethodHandler mi = new MethodHandler() {
            public Object invoke(Object self, Method m, Method proceed,
                    Object[] args) throws Throwable {
                System.out.println("Name: " + m.getName());
                return proceed.invoke(self, args) + "!"; // execute the original
                // method.
            }
        };
        Foo2 foo = (Foo2)c.getConstructor().newInstance();
        try {
            ((ProxyObject)foo).setHandler(mi);
            fail("foo is a ProxyObject!");
        } catch (ClassCastException e) {}
        ((Proxy)foo).setHandler(mi);
        assertEquals("do something!", foo.doSomething());
        assertEquals("return a string!", foo.getHandler());
    }
}
