package testproxy;

import java.io.Serializable;
import java.lang.reflect.Method;
/**
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;
import net.sf.cglib.proxy.NoOp;
*/
import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class ProxyFactoryPerformanceTest extends TestCase {

	public static final int COUNT = 100; 
	
	public static final int MAX_THREADS = 30;

    static Throwable error = null;

	public ProxyFactoryPerformanceTest() {}
	
	public ProxyFactoryPerformanceTest(String name) { 
		super(name); 
	}
	
	public void testJavassist() throws Throwable {
		callCreateClass("javassist", ProxyMaker.class);
	}
	
	/**
	public void testCglib() throws Exception {
		callCreateClass("cglib", EnhancerUser.class);
	}
	*/
	
	public void callCreateClass(String translator, Class cl) throws Throwable {
        error = null;
		Thread[] threads = new Thread[MAX_THREADS];
		for (int i = 0; i < threads.length; ++i) {
			threads[i] = (Thread)cl.getDeclaredConstructor().newInstance();
		}
		long time = System.currentTimeMillis();
		for (int i = 0; i < threads.length; ++i) {
			threads[i].start();
		}
		for (int i = 0; i < threads.length; ++i) {
			threads[i].join();
		}
		time = System.currentTimeMillis() - time;
		System.out.println("ProxyFactoryPerformanceTest: " + translator + " time: " + time);
        if (error != null)
            throw error;
	}

	public static Test suite() {
		return new TestSuite(ProxyFactoryPerformanceTest.class);
	}

    public static void callOnce() {
        try {
            Thread t = new ProxyMaker();
            t.start();
            t.join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("** Done");
    }

    public static void main(String[] args) {
        // callOnce();
        ProxyFactory.useCache = args.length == 0;
		TestRunner.run(suite());
	}
}

class ProxyMaker extends Thread implements MethodHandler {
	private static final MethodFilter FINALIZE_FILTER = new MethodFilter() {
		public boolean isHandled(Method m) {
			// skip finalize methods
			return !( m.getParameterTypes().length == 0 && m.getName().equals( "finalize" ) );
		}
	};
	
	public void run() {
		for (int i = 0; i < ProxyFactoryPerformanceTest.COUNT; ++i) {
			callCreateClass();
		}
    }
	
	public void callCreateClass() {
		try {
			ProxyFactory factory = new ProxyFactory();
			factory.setSuperclass(SampleBean.class);
			factory.setInterfaces(SampleBean.class.getInterfaces());
			factory.setFilter(FINALIZE_FILTER);
			// factory.setHandler(this);

			Class proxyClass = factory.createClass();
			//System.out.println("proxy name: " + proxyClass.getName());
		} catch (Throwable e) {
			e.printStackTrace();
            ProxyFactoryPerformanceTest.error = e;
		}
	}

	public Object invoke(Object arg0, Method arg1, Method arg2, Object[] arg3) throws Throwable {
		return null;
	}	
}

/**
class EnhancerUser extends Thread implements InvocationHandler {
	private static final CallbackFilter FINALIZE_FILTER = new CallbackFilter() {
		public int accept(Method method) {
			if ( method.getParameterTypes().length == 0 && method.getName().equals("finalize") ){
				return 1;
			}
			else {
				return 0;
			}
		}
	};
	
	public void run() {
		for (int i = 0; i < ProxyFactoryPerformanceTest.COUNT; ++i) {
			callCreateClass();
		}
	}
	
	public void callCreateClass() {
		try {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(SampleBean.class);
			enhancer.setInterfaces(SampleBean.class.getInterfaces());
			enhancer.setCallbackTypes(new Class[] { InvocationHandler.class, NoOp.class });
			enhancer.setCallbackFilter(FINALIZE_FILTER);
			enhancer.setInterceptDuringConstruction(false);
			// TODO
			enhancer.setUseCache(false);
			enhancer.setUseFactory(false);
			Class proxyClass = enhancer.createClass();
			//System.out.println("proxy name: " + proxyClass.getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
		return null;
	}
}
*/

class SampleBean implements Serializable {
    long oid;
    
    int version;
    
    SampleBean bean;
    
    public void setOid(long _oid) {
    	oid = _oid;
    }
    
    public long getOid() {
    	return oid;
    }

    public void setVersion(int _ver) {
    	version = _ver;
    }
    
    public int getVersion() {
    	return version;
    }

    public void setBean(SampleBean _bean) {
    	bean = _bean;
    }
    
    public SampleBean getBean() {
    	return bean;
    }
}
