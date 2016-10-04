package javassist.proxyfactory;

import junit.framework.*;
import javassist.util.proxy.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

class Hand implements java.io.Serializable {
    public int setHandler(int i) { return i; }
    int getHandler() { return 3; }
}

public class Tester extends TestCase {
    static class MHandler implements MethodHandler, java.io.Serializable {
        public Object invoke(Object self, Method m, Method proceed,
                             Object[] args) throws Throwable {
            System.out.println("Name: " + m.getName());
            return proceed.invoke(self, args);
        }
    }

    static MethodHandler mi = new MHandler();

    public void test() throws Exception {
        ProxyFactory f = new ProxyFactory();
        f.setSuperclass(Hand.class);
        Class c = f.createClass();
        Hand foo = (Hand)c.getConstructor().newInstance();
        ((Proxy)foo).setHandler(mi);
        assertTrue(ProxyFactory.isProxyClass(c));
        assertEquals(3, foo.getHandler());
    }

    public void test2() throws Exception {
        ProxyFactory f = new ProxyFactory();
        f.setSuperclass(Hand.class);
        Hand h = (Hand)f.create(new Class[0], new Object[0], mi);
        assertEquals(3, h.getHandler());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ProxyObjectOutputStream out = new ProxyObjectOutputStream(bos);
        out.writeObject(h);
        out.close();
        byte[] bytes = bos.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ProxyObjectInputStream in = new ProxyObjectInputStream(bis);
        Hand h2 = (Hand)in.readObject();
        assertEquals(3, h2.getHandler());
    }
}
