package test.javassist.proxy;

import javassist.util.proxy.ProxyFactory;
import junit.framework.TestCase;

/**
 * Test for regression error detailed in JASSIST-113
 */
public class JASSIST113RegressionTest extends TestCase
{
    interface Bear
    {
        void hibernate();
    }

    public void testProxyFactoryWithNonPublicInterface()
    {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setInterfaces(new Class[]{Bear.class});
        proxyFactory.createClass();
    }
}
