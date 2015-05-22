package javassist.tools;

import javassist.*;
import junit.framework.TestCase;
import test.javassist.tools.DummyClass;

import static javassist.tools.Callback.*;

public class CallbackTest extends TestCase {

    public void testSomeCallbacks() throws Exception {

        // Get class and method to change
        ClassPool pool = ClassPool.getDefault();
        CtClass classToChange = pool.get("test.javassist.tools.DummyClass");
        CtMethod methodToChange = classToChange.getDeclaredMethod("dummyMethod");

        // Insert after
        methodToChange.insertAfter(new Callback("Thread.currentThread(), dummyString") {
            @Override
            public void result(Object... objects) {
                assertEquals(objects[0], Thread.currentThread());
                assertEquals(objects[1], "dummyStringValue");
            }
        }.sourceCode());

        // Insert after using utility method
        insertAfter(methodToChange, new Callback("Thread.currentThread(), dummyString") {
            @Override
            public void result(Object... objects) {
                assertEquals(objects[0], Thread.currentThread());
                assertEquals(objects[1], "dummyStringValue");
            }
        });

        // Change class and invoke method;
        classToChange.toClass();
        new DummyClass().dummyMethod();
    }
}
