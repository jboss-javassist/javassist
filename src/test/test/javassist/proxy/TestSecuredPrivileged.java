package test.javassist.proxy;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.util.proxy.DefineClassHelper;

public class TestSecuredPrivileged {

    public TestSecuredPrivileged() {
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    /**
     * Test proves that you cannot even access members with
     * private static and final modifiers. */
    @Test
    public void testDefinedHelperPrivilegedFieldVisibility() {
        try {
            Field privi = DefineClassHelper.class.getDeclaredField("privileged");
            assertTrue(Modifier.isStatic(privi.getModifiers()));
            thrown.expectCause(instanceOf(IllegalAccessException.class));
            thrown.expectMessage(both(stringContainsInOrder(Arrays.asList("cannot access a member")))
                    .and(stringContainsInOrder(Arrays.asList("with modifiers \"private static final".split("", 1)))));
    
            privi.get(null);
        } catch(Throwable  t) {
            throw new RuntimeException(t);
        }
    }
    /**
     * Test proves that the default enum constant is a class and specifically
     * auto selected for Java 9. */
    @Test
    public void testDefinedHelperPrivilegedField() {
        try {
            Field privi = DefineClassHelper.class.getDeclaredField("privileged");
            assertTrue(Modifier.isStatic(privi.getModifiers()));
            Constructor<DefineClassHelper> con = DefineClassHelper.class.getDeclaredConstructor();
            con.setAccessible(true);
            DefineClassHelper inst = con.newInstance();
            assertThat(inst, instanceOf(DefineClassHelper.class));
            privi.setAccessible(true);
            Object p = privi.get(inst);
            assertThat(""+p, equalTo("JAVA_9"));
            assertThat(p.getClass().getName(), endsWith("SecuredPrivileged$1"));
        } catch(Throwable  t) {
            throw new RuntimeException(t);
        }
    }
    /**
     * Test proves that caller class security is enforced and works
     * as expected. */
    @Test
    public void testDefinedHelperPrivilegedFieldMethodAccessDenied() {
        try {
            Constructor<DefineClassHelper> con = DefineClassHelper.class.getDeclaredConstructor();
            con.setAccessible(true);
            DefineClassHelper inst = con.newInstance();
            Field privi = DefineClassHelper.class.getDeclaredField("privileged");
            privi.setAccessible(true);
            Object priviInst = privi.get(inst);
            Method defineClass = priviInst.getClass().getDeclaredMethod(
                    "defineClass", new Class[] {
                        String.class, byte[].class, int.class, int.class, 
                        ClassLoader.class, ProtectionDomain.class
                    });
        
            assertThat(defineClass, notNullValue());
            defineClass.setAccessible(true);
            assertThat(defineClass.getName(), equalTo("defineClass"));
            assertTrue(defineClass.canAccess(priviInst));
            ClassPool cp = ClassPool.getDefault();
            CtClass c = cp.makeClass("a.b.C");
            byte[] bc = c.toBytecode();

            thrown.expectCause(instanceOf(IllegalAccessError.class));
            thrown.expectMessage(equalTo("java.lang.IllegalAccessError: Access denied for caller."));

            @SuppressWarnings("unused")
            Object res = defineClass.invoke(priviInst, new Object[] {
                c.getName(), bc, 0, bc.length, new ClassLoader() {},
                ClassLoader.class.getProtectionDomain()
            });
        } catch(InvocationTargetException  t) { 
            throw new RuntimeException(t.getTargetException());
        } catch(Throwable  t) { throw new RuntimeException(t); }
    }
    /**
     * Test proves that we do have 3 enum constants in the private static 
     * inner class. */
    @Test
    public void testDefinedHelperEnumClass() {
        try {
            Constructor<DefineClassHelper> con = DefineClassHelper.class.getDeclaredConstructor();
            con.setAccessible(true);
            assertThat(DefineClassHelper.class.getDeclaredClasses(), arrayWithSize(1));
            Class<?> secPriv = DefineClassHelper.class.getDeclaredClasses()[0];
            assertTrue(secPriv.isEnum());
            assertThat(secPriv.getEnumConstants(), arrayWithSize(3));
            assertThat(""+secPriv.getEnumConstants()[0], equalTo("JAVA_9"));
            assertThat(""+secPriv.getEnumConstants()[1], equalTo("JAVA_7"));
            assertThat(""+secPriv.getEnumConstants()[2], equalTo("JAVA_OTHER"));

        } catch (Throwable t) {t.printStackTrace();}

    }
    /**
     * Test proves that you cannot modify private static final reference even 
     * with setAccessible(true). */
    @Test
    public void testDefinedHelperCannotSetPrivileged() {
        try {
            Constructor<DefineClassHelper> con = DefineClassHelper.class.getDeclaredConstructor();
            con.setAccessible(true);
            DefineClassHelper inst = con.newInstance();
            Class<?> secPriv = DefineClassHelper.class.getDeclaredClasses()[0];
            Object J7 = secPriv.getEnumConstants()[1];
            Field privi = DefineClassHelper.class.getDeclaredField("privileged");
            privi.setAccessible(true);
            thrown.expectCause(instanceOf(IllegalAccessException.class));
            thrown.expectMessage(startsWith("java.lang.IllegalAccessException: Can not set static final"));
            privi.set(inst, J7);

        } catch (Throwable t) {throw new RuntimeException(t);}

    }
    /**
     * Test proves that you can achieve the impossible and modify private
     * static final class reference without an instance. Now we can Mock
     * test JDK 6 to 8 functionality  */
    @Test
    public void testDefinedHelperSetPrivilegedToJava7() {
        try {
            Constructor<DefineClassHelper> con = DefineClassHelper.class.getDeclaredConstructor();
            con.setAccessible(true);
            DefineClassHelper inst = con.newInstance();
            Class<?> secPriv = DefineClassHelper.class.getDeclaredClasses()[0];
            Object J9 = secPriv.getEnumConstants()[0];
            Object J7 = secPriv.getEnumConstants()[1];
            Field privi = DefineClassHelper.class.getDeclaredField("privileged");
            privi.setAccessible(true);
            Object privInst = privi.get(inst);
            Field unsf = privInst.getClass().getDeclaredField("sunMiscUnsafe");
            unsf.setAccessible(true);
            Object refu = unsf.get(privInst);
            Field tuf = refu.getClass().getDeclaredField("sunMiscUnsafeTheUnsafe");
            tuf.setAccessible(true);
            Object tu = tuf.get(refu);
            Method tu_call = tu.getClass().getMethod("call", new Class<?>[] {String.class, Object[].class});
            tu_call.setAccessible(true);
            long offset = (Long) tu_call.invoke(tu, new Object[] {"staticFieldOffset",  new Object[] {privi}}); 
            tu_call.invoke(tu, new Object[] {"putObjectVolatile",  new Object[] {DefineClassHelper.class, offset, J7}});

            Object p = privi.get(inst);
            assertThat(""+p, equalTo("JAVA_7"));
            assertThat(p.getClass().getName(), endsWith("SecuredPrivileged$2"));

            tu_call.invoke(tu, new Object[] {"putObjectVolatile",  new Object[] {DefineClassHelper.class, offset, J9}});

        } catch (Throwable t) {t.printStackTrace();}

    }
    /**
     * Test proves that Java 7+ MethodHandle defineClass (or DefineClassHelper.toClass)
     * works as expected. */
    @Test
    public void testDefinedHelperJava7ToClass() {
        try {
            Constructor<DefineClassHelper> con = DefineClassHelper.class.getDeclaredConstructor();
            con.setAccessible(true);
            DefineClassHelper inst = con.newInstance();
            Class<?> secPriv = DefineClassHelper.class.getDeclaredClasses()[0];
            Object J9 = secPriv.getEnumConstants()[0];
            Object J7 = secPriv.getEnumConstants()[1];
            Field privi = DefineClassHelper.class.getDeclaredField("privileged");
            privi.setAccessible(true);
            Object privInst = privi.get(inst);
            Field unsf = privInst.getClass().getDeclaredField("sunMiscUnsafe");
            unsf.setAccessible(true);
            Object refu = unsf.get(privInst);
            Field tuf = refu.getClass().getDeclaredField("sunMiscUnsafeTheUnsafe");
            tuf.setAccessible(true);
            Object tu = tuf.get(refu);
            Method tu_call = tu.getClass().getMethod("call", new Class<?>[] {String.class, Object[].class});
            tu_call.setAccessible(true);
            long offset = (Long) tu_call.invoke(tu, new Object[] {"staticFieldOffset",  new Object[] {privi}}); 
            tu_call.invoke(tu, new Object[] {"putObjectVolatile",  new Object[] {DefineClassHelper.class, offset, J7}});

            ClassPool cp = ClassPool.getDefault();
            CtClass c = cp.makeClass("a.b.J7");
            byte[] bc = c.toBytecode();
            Class<?> bcCls = DefineClassHelper.toClass("a.b.J7", new ClassLoader() {}, null, bc);
            assertThat(bcCls.getName(), equalTo("a.b.J7"));
            assertThat(bcCls.getDeclaredConstructor().newInstance(),
                    not(equalTo(bcCls.getDeclaredConstructor().newInstance())));

            tu_call.invoke(tu, new Object[] {"putObjectVolatile",  new Object[] {DefineClassHelper.class, offset, J9}});
            
        } catch (Throwable t) {t.printStackTrace();}

    }
    /**
     * Test proves that Java 6 reflection method defineClass (or DefineClassHelper.toClass)
     * works as expected. */
    @Test
    public void testDefinedHelperJavaOtherToClass() {
        try {
            Constructor<DefineClassHelper> con = DefineClassHelper.class.getDeclaredConstructor();
            con.setAccessible(true);
            DefineClassHelper inst = con.newInstance();
            Class<?> secPriv = DefineClassHelper.class.getDeclaredClasses()[0];
            Object J9 = secPriv.getEnumConstants()[0];
            Object JO = secPriv.getEnumConstants()[2];
            Field privi = DefineClassHelper.class.getDeclaredField("privileged");
            privi.setAccessible(true);
            Object privInst = privi.get(inst);
            Field unsf = privInst.getClass().getDeclaredField("sunMiscUnsafe");
            unsf.setAccessible(true);
            Object refu = unsf.get(privInst);
            Field tuf = refu.getClass().getDeclaredField("sunMiscUnsafeTheUnsafe");
            tuf.setAccessible(true);
            Object tu = tuf.get(refu);
            Method tu_call = tu.getClass().getMethod("call", new Class<?>[] {String.class, Object[].class});
            tu_call.setAccessible(true);
            long offset = (Long) tu_call.invoke(tu, new Object[] {"staticFieldOffset",  new Object[] {privi}}); 
            tu_call.invoke(tu, new Object[] {"putObjectVolatile",  new Object[] {DefineClassHelper.class, offset, JO}});

            ClassPool cp = ClassPool.getDefault();
            CtClass c = cp.makeClass("a.b.JO");
            byte[] bc = c.toBytecode();
            Class<?> bcCls = DefineClassHelper.toClass("a.b.JO", new ClassLoader() {}, null, bc);
            assertThat(bcCls.getName(), equalTo("a.b.JO"));
            assertThat(bcCls.getDeclaredConstructor().newInstance(),
                    not(equalTo(bcCls.getDeclaredConstructor().newInstance())));

            tu_call.invoke(tu, new Object[] {"putObjectVolatile",  new Object[] {DefineClassHelper.class, offset, J9}});
            
        } catch (Throwable t) {t.printStackTrace();}

    }
    /**
     * Test proves that default Java 9 defineClass (or DefineClassHelper.toClass)
     * works as expected. */
    @Test
    public void testDefinedHelperDefaultToClass() {
        try {
            ClassPool cp = ClassPool.getDefault();
            CtClass c = cp.makeClass("a.b.D");
            byte[] bc = c.toBytecode();
            Class<?> bcCls = DefineClassHelper.toClass("a.b.D", new ClassLoader() {}, null, bc);
            assertThat(bcCls.getName(), equalTo("a.b.D"));
            assertThat(bcCls.getDeclaredConstructor().newInstance(),
                    not(equalTo(bcCls.getDeclaredConstructor().newInstance())));
        } catch (Throwable t) {t.printStackTrace();}

    }
}
