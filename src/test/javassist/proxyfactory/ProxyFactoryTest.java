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

        MyCls myCls = (MyCls) proxyClass.newInstance();
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

        MyCls myCls = (MyCls) proxyClass.newInstance();
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
    
}
