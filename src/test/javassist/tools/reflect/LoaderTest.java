package javassist.tools.reflect;

import javassist.*;
import junit.framework.*;

public class LoaderTest extends TestCase {
    private Loader loader;

    public LoaderTest(String name) {
         super(name);
    }

    public void setUp() throws Exception {
        loader = new Loader();
    }

    public void testAttemptReflectInterface() throws Exception {
        try {
            loader.makeReflective("javassist.ClassPath",
                                  "javassist.tools.reflect.Metaobject",
                                  "javassist.tools.reflect.ClassMetaobject");
            fail("Attempting to reflect an interface should throw a CannotReflectException");
        } catch (CannotReflectException e) {
            // expected
        }
    }

    public void testAttemptReflectClassMetaobject() throws Exception {
        try {
            loader.makeReflective("javassist.tools.reflect.ClassMetaobject",
                                  "javassist.tools.reflect.Metaobject",
                                  "javassist.tools.reflect.ClassMetaobject");
            fail("Attempting to reflect a ClassMetaobject should throw a CannotReflectException");
        } catch (CannotReflectException e) {
            // expected
        }
    }

    public void testAttemptReflectMetaobject() throws Exception {
        try {
            loader.makeReflective("javassist.tools.reflect.Metaobject",
                                  "javassist.tools.reflect.Metaobject",
                                  "javassist.tools.reflect.ClassMetaobject");
            fail("Attempting to reflect a Metaobject should throw a CannotReflectException");
        } catch (CannotReflectException e) {
            // expected
        }
    }

    public void testFinalMethod() throws Throwable {
        loader.makeReflective("javassist.tools.reflect.SuperClass",
                              "javassist.tools.reflect.Metaobject",
                              "javassist.tools.reflect.ClassMetaobject");

        ClassPool cp = ClassPool.getDefault();

        CtClass cc2 = cp.get("javassist.tools.reflect.SuperClass");
        cc2.debugWriteFile("reflected/");

        CtClass cc = cp.get("javassist.tools.reflect.SubClass");

        CtMethod[] ms = cc.getMethods();
        for (int i = 0; i < ms.length; ++i)
            System.out.println(ms[i] + " in "
                               + ms[i].getDeclaringClass().getName());

        loader.makeReflective("javassist.tools.reflect.SubClass",
                              "javassist.tools.reflect.Metaobject",
                              "javassist.tools.reflect.ClassMetaobject");

        loader.run("javassist.tools.reflect.SubClass", new String[] {});
    }
}
