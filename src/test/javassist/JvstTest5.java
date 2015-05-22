package javassist;

import java.lang.annotation.Annotation;
import java.lang.reflect.TypeVariable;

public class JvstTest5 extends JvstTestRoot {
    public JvstTest5(String name) {
        super(name);
    }

    public void testDollarClassInStaticMethod() throws Exception {
        CtClass cc = sloader.makeClass("test5.DollarClass");
        CtMethod m = CtNewMethod.make("public static int run(){ return $class.getName().length(); }", cc);
        cc.addMethod(m);
        m = CtNewMethod.make("public int run2(){ return $class.getName().length(); }", cc);
        cc.addMethod(m);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(cc.getName().length(), invoke(obj, "run"));
        assertEquals(cc.getName().length(), invoke(obj, "run2"));
    }

    public void testSuperDefaultMethodCall() throws Exception {
        CtClass cc = sloader.get("test5.DefaultMethod");
        CtMethod m = CtNewMethod.make("public int run(){ return test5.DefaultMethodIntf.super.foo(); }", cc);
        cc.addMethod(m);
        m = CtNewMethod.make("public int run2(){ return test5.DefaultMethodIntf.baz(); }", cc);
        cc.addMethod(m);
        m = CtNewMethod.make("public int run3(){ return test5.DefaultMethodIntf.super.baz(); }", cc);
        cc.addMethod(m);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "run"));
        assertEquals(10, invoke(obj, "run2"));
        assertEquals(10, invoke(obj, "run3"));
    }

    public void testTypeAnno() throws Exception {
        CtClass cc = sloader.get("test5.TypeAnno");
        cc.getClassFile().compact();
        cc.writeFile();
        Object obj = make(cc.getName());
        TypeVariable<?> t = obj.getClass().getTypeParameters()[0];
        Annotation[] annos = t.getAnnotations();
        assertEquals("@test5.TypeAnnoA()", annos[0].toString());
    }

    public void testJIRA241() throws Exception {
        CtClass cc = sloader.get("test5.JIRA241");
        CtMethod testMethod = cc.getDeclaredMethod("test");
        testMethod.insertAfter("System.out.println(\"inserted!\");");
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(10, invoke(obj, "run"));
    }
}
