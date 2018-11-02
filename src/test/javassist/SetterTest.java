package javassist;
import java.lang.reflect.Method;

import junit.framework.TestCase;

@SuppressWarnings({"rawtypes","unchecked"})
public class SetterTest extends TestCase {

    ClassPool pool;
    Class<?> capability;

    public SetterTest(String name) {
         super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        pool = ClassPool.getDefault();
        capability = Class.forName("DefineClassCapability");
    }

    /**
     * Tests a getter only on a field without a Modifier.
     * 
     * @throws Exception
     */
    public void testFieldGetter() throws Exception {
        CtClass clazz = pool.makeClass("HasFieldGetter");
        clazz.setSuperclass(pool.get("java.lang.Object"));
        CtField field = new CtField(CtClass.booleanType, "broken", clazz);
        clazz.addField(field, "true");
        clazz.addMethod(CtNewMethod.getter("isBroken", field));
        Class _class = clazz.toClass(capability);

        Object object = _class.getConstructor().newInstance();
        check(_class, object, true);
    }

    /**
     * Tests a getter and a setter on a field without a Modifier.
     * 
     * @throws Exception
     */
    public void testFieldGetterSetter() throws Exception {
        CtClass clazz = pool.makeClass("HasFieldGetterSetter");
        clazz.setSuperclass(pool.get("java.lang.Object"));
        CtField field = new CtField(CtClass.booleanType, "broken", clazz);
        clazz.addField(field, "true");
        clazz.addMethod(CtNewMethod.getter("isBroken", field));
        clazz.addMethod(CtNewMethod.setter("setBroken", field));
        Class _class = clazz.toClass(capability);

        Object object = _class.getConstructor().newInstance();

        set(_class, object, false);
        check(_class, object, false);
    }

    /**
     * Tests a getter only on a field with Modifier.STATIC.
     * 
     * @throws Exception
     */
    public void testStaticFieldGetter() throws Exception {
        CtClass clazz = pool.makeClass("HasStaticFieldGetter");
        clazz.setSuperclass(pool.get("java.lang.Object"));
        CtField field = new CtField(CtClass.booleanType, "broken", clazz);
        field.setModifiers(Modifier.STATIC);
        clazz.addField(field, "true");
        clazz.addMethod(CtNewMethod.getter("isBroken", field));
        Class _class = clazz.toClass(capability);

        Object object = _class.getConstructor().newInstance();
        check(_class, object, true);
    }

    /**
     * Tests a getter and setter on a field with Modifier.STATIC.
     * 
     * @throws Exception
     */
    public void testStaticFieldGetterSetter() throws Exception {
        CtClass clazz = pool.makeClass("HasStaticFieldGetterSetter");
        clazz.setSuperclass(pool.get("java.lang.Object"));
        CtField field = new CtField(CtClass.booleanType, "broken", clazz);
        field.setModifiers(Modifier.STATIC);
        clazz.addField(field, "true");
        clazz.addMethod(CtNewMethod.getter("isBroken", field));
        clazz.addMethod(CtNewMethod.setter("setBroken", field));
        Class _class = clazz.toClass(capability);

        Object object = _class.getConstructor().newInstance();

        set(_class, object, false);
        check(_class, object, false);
    }

    private void check(Class _class, Object object, boolean shouldBe)
        throws Exception
    {
        Method method = _class.getMethod("isBroken", new Class[] {});
        Boolean result = (Boolean) method.invoke(object, new Object[] {});
        assertEquals("boolean is wrong value",
                     shouldBe, result.booleanValue());
    }

    private void set(Class _class, Object object, boolean willBe)
        throws Exception
    {
        Method method = _class.getMethod("setBroken",
                                         new Class[] {Boolean.TYPE});
        method.invoke(object, new Object[] { Boolean.valueOf(willBe)});
    }
}
