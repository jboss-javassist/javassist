package javassist.proxyfactory;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import junit.framework.TestCase;

import java.io.*;
import java.lang.reflect.Method;

/**
 * <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class ProxyFactoryTest extends TestCase {
    public void testMethodHandlers() throws Exception {
        ProxyFactory fact = new ProxyFactory();
        fact.setSuperclass(MyCls.class);

        Class proxyClass = fact.createClass();

        MyMethodHandler myHandler = new MyMethodHandler();
        myHandler.setX(4711);

        MyCls myCls = (MyCls) proxyClass.getConstructor().newInstance();
        ((ProxyObject) myCls).setHandler(myHandler);

        MethodHandler h2 = ((ProxyObject) myCls).getHandler();
        assertNotNull(h2);
        assertTrue(h2 instanceof MyMethodHandler);
    }

    public void testSerialize() throws Exception {
        ProxyFactory fact = new ProxyFactory();
        fact.setSuperclass(MyCls.class);

        Class proxyClass = fact.createClass();

        MyMethodHandler myHandler = new MyMethodHandler();
        myHandler.setX(4711);

        MyCls myCls = (MyCls) proxyClass.getConstructor().newInstance();
        ((ProxyObject) myCls).setHandler(myHandler);


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(myCls);
        byte[] ba = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        ObjectInputStream ois = new ObjectInputStream(bais);
        MyCls myCls2 =  (MyCls) ois.readObject();

        MethodHandler h2 = ((ProxyObject) myCls2).getHandler();
        assertNotNull(h2);
        assertTrue(h2 instanceof MyMethodHandler);
    }

    
    public static class MyMethodHandler implements MethodHandler, Serializable {

        private int x;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
            // actually do nothing!
            return null;
        }
    }

    public void testJira127() throws Exception {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setInterfaces(new Class[]{ JIRA127Sub.class });
        proxyFactory.createClass();
    }

    public interface JIRA127 {
        JIRA127 get();
    }
    public interface JIRA127Sub extends JIRA127 {
        JIRA127Sub get();
    }

    public void testDefaultMethod() throws Exception {
        ProxyFactory proxyFactory = new ProxyFactory();
        //proxyFactory.writeDirectory = "./dump";
        proxyFactory.setInterfaces(new Class[]{ TestDefaultI.class });
        Class intf = proxyFactory.createClass();
        TestDefaultI obj = (TestDefaultI)intf.getConstructor().newInstance();
        obj.foo();

        ProxyFactory proxyFactory2 = new ProxyFactory();
        //proxyFactory2.writeDirectory = "./dump";
        proxyFactory2.setSuperclass(TestDefaultC.class);
        Class clazz2 = proxyFactory2.createClass();
        TestDefaultC obj2 = (TestDefaultC)clazz2.getConstructor().newInstance();
        obj2.foo();
        obj2.bar();

        ProxyFactory proxyFactory3 = new ProxyFactory();
        proxyFactory3.setSuperclass(TestDefaultC2.class);
        Class clazz3 = proxyFactory3.createClass();
        TestDefaultC2 obj3 = (TestDefaultC2)clazz3.getConstructor().newInstance();
        obj3.foo();
        obj3.bar();
        obj3.baz();
    }

    public static interface TestDefaultI {
        default int foo() { return 10; }
    }

    public static class TestDefaultC implements TestDefaultI {
        public int foo() { return 1; }
        public int bar() { return TestDefaultI.super.foo(); }
    }

    public static class TestDefaultC2 extends TestDefaultC {
        public int baz() { return super.foo(); }
    }
}
