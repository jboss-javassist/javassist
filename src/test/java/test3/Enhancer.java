package test3;

import javassist.*;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

/* Test code
 */
class EnhanceTest {
    public EnhanceTest() { super(); }
    public void foo(String s) { System.out.println(s); }
}

@SuppressWarnings({"rawtypes","unchecked","unused"})
public class Enhancer {
    private ClassPool pool;
    private CtClass superClass;
    private CtClass thisClass;
    private Class thisJavaClass;
    private Interceptor interceptor;
    private int unique;

    private static final String INTERCEPTOR = "interceptor";

    /* Test method
     */
    public static void main(String[] args) throws Exception {
        Enhancer e = new Enhancer(test3.EnhanceTest.class);
        e.overrideAll();
        e.setCallback(new Interceptor() {
                public Object invoke(Object self, Method m, Object[] args)
                    throws Exception
                {
                    System.out.println("intercept: " + m);
                    return m.invoke(self, args);
                }
            });
        Class c = e.createClass();
        EnhanceTest obj = (EnhanceTest)c.getConstructor().newInstance();
        obj.foo("test");
    }

    public static interface Interceptor {
        Object invoke(Object self, Method m, Object[] args) throws Exception;
    }

    public Enhancer(Class clazz)
        throws CannotCompileException, NotFoundException
    {
        this(makeClassPool(clazz).get(clazz.getName()));
    }

    private static ClassPool makeClassPool(Class clazz) {
        ClassPool cp = new ClassPool();
        cp.appendSystemPath();
        cp.insertClassPath(new ClassClassPath(clazz));
        return cp;
    }

    public Enhancer(CtClass superClass)
        throws CannotCompileException, NotFoundException
    {
        this.pool = superClass.getClassPool();
        this.superClass = superClass;
        String name = superClass.getName() + "_proxy";
        thisClass = pool.makeClass(name);
        thisClass.setSuperclass(superClass);
        String src =
            "public static " + this.getClass().getName()
          + ".Interceptor " + INTERCEPTOR + ";";

        thisClass.addField(CtField.make(src, thisClass));
        this.thisJavaClass = null;
        unique = 0;
    }

    public void overrideAll()
        throws CannotCompileException, NotFoundException
    {
        CtMethod[] methods = superClass.getMethods();
        String delegatorNamePrefix = thisClass.makeUniqueName("d");
        for (int i = 0; i < methods.length; i++) {
            CtMethod m = methods[i];
            int mod = m.getModifiers();
            if (!Modifier.isFinal(mod) && !Modifier.isAbstract(mod)
                && !Modifier.isStatic(mod))
                override(m, delegatorNamePrefix + i);
        }
    }

    public void override(CtMethod m, String delegatorName)
        throws CannotCompileException, NotFoundException
    {
        String fieldName = "m" + unique++;
        thisClass.addField(
            CtField.make("private java.lang.reflect.Method "
                         + fieldName + ";", thisClass));
        CtMethod delegator = CtNewMethod.delegator(m, thisClass);
        delegator.setModifiers(Modifier.clear(delegator.getModifiers(),
                                              Modifier.NATIVE));
        delegator.setName(delegatorName);
        thisClass.addMethod(delegator);
        thisClass.addMethod(makeMethod(m, fieldName, delegatorName));
    }

    private CtMethod makeMethod(CtMethod m, String fieldName,
                                String delegatorName)
        throws CannotCompileException, NotFoundException
    {
        String factory = this.getClass().getName() + ".findMethod(this, \"" +
            delegatorName + "\");";
        String body
            = "{ if (" + fieldName + " == null) " +
                   fieldName + " = " + factory +
                 "return ($r)" + INTERCEPTOR + ".invoke(this, " + fieldName +
            				                ", $args); }";
        CtMethod m2 = CtNewMethod.make(m.getReturnType(),
                                       m.getName(),
                                       m.getParameterTypes(),
                                       m.getExceptionTypes(),
                                       body, thisClass);
        m2.setModifiers(Modifier.clear(m.getModifiers(),
                                       Modifier.NATIVE));
        return m2;
    }

    /* A runtime support routine called by an enhanced object.
     */
    public static Method findMethod(Object self, String name) {
        Method[] methods = self.getClass().getMethods();
        int n = methods.length;
        for (int i = 0; i < n; i++)
            if (methods[i].getName().equals(name))
                return methods[i];

        throw new RuntimeException("not found " + name
                                   + " in " + self.getClass());
    }

    public Class createClass() {
        if (thisJavaClass == null)
            try {
                thisClass.debugWriteFile();
                thisJavaClass = thisClass.toClass();
                setInterceptor();
            }
            catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }

        return thisJavaClass;
    }

    private static void writeFile(CtClass cc) {
        try {
            cc.stopPruning(true);
            cc.writeFile();
            cc.defrost();
            cc.stopPruning(false);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setCallback(Interceptor mi) {
        interceptor = mi;
        setInterceptor();
    }

    private void setInterceptor() {
        if (thisJavaClass != null && interceptor != null)
            try {
                Field f = thisJavaClass.getField(INTERCEPTOR);
                f.set(null, interceptor);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
    }
}
