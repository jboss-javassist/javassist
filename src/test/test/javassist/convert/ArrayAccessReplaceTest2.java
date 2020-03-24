package test.javassist.convert;

import javassist.ClassPool;
import javassist.CodeConverter;
import javassist.CtClass;
import junit.framework.TestCase;

public class ArrayAccessReplaceTest2 extends TestCase {

    public void testAdvancedInstrumentation() throws Exception {
        ClassPool pool = new ClassPool(true);
        CtClass monitoringClass = pool.get(ArrayAccessReplaceTest2.class.getName());
        CtClass targetClass = pool.get(InstrumentationTarget.class.getName());
        CodeConverter converter = new CodeConverter();
        // we just test if the instrumentation works, the monitoring class does not need to actually contain the replacer methods
        // what is only relevant when code gets executed
        converter.replaceArrayAccess(monitoringClass, new CodeConverter.DefaultArrayAccessReplacementMethodNames());
        targetClass.instrument(converter);
    }

}