package javassist;

import java.lang.annotation.Annotation;
import java.lang.reflect.TypeVariable;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.InnerClassesAttribute;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

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

    public void testJIRA246() throws Exception {
        CtClass ctClass = sloader.makeClass("test5.JIRA246Test");
        ctClass.addInterface(sloader.get(test5.JIRA246.Test.class.getName()));
        String methodBody = "public void test() { defaultMethod(); }";
        CtMethod ctMethod = CtMethod.make(methodBody, ctClass);
        ctClass.addMethod(ctMethod);
    }

    public void testJIRA246b() throws Exception {
        CtClass ctClass = sloader.get(test5.JIRA246.A.class.getName());
        String src = "public void id() { get(); }";
        CtMethod make = CtNewMethod.make(src, ctClass);
    }

    public void testJIRA242() throws Exception {
        Boolean ss = new Boolean(2 > 3);
        ClassPool cp = ClassPool.getDefault();
        CtClass cc = cp.get("test5.JIRA242$Hello");
        CtMethod m = cc.getDeclaredMethod("say");
        m.insertBefore("{ System.out.println(\"Say Hello...\"); }");

        StringBuilder sb = new StringBuilder();
        sb.append("BOOL_SERIES = createBooleanSeriesStep();");
        //Below code cause the issue
        sb.append("BOOL_SERIES.setValue(3>=3);"); //lets comment this and run it will work 
        // Below code snippets will work
        // this cast into exact class and call the same function
        sb.append("((test5.JIRA242$BooleanDataSeries)BOOL_SERIES).setValue(3>=3);");
        // this code snippet will set exact boolean variable to the function.
        sb.append("boolean var = 3>=3;");
        sb.append("BOOL_SERIES.setValue(var);");

        m.insertBefore(sb.toString());
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(0, invoke(obj, "say"));
    }

    public void testJIRA249() throws Exception {
        CtClass cc = sloader.get("test5.BoolTest");
        CtMethod testMethod = cc.getDeclaredMethod("test");
        testMethod.insertBefore("i = foo(true & true);");
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "run"));
    }

    public void testInnerClassAttributeRemove() throws Exception {
        CtClass cc = sloader.get("test5.InnerClassRemove");
        ClassFile cf = cc.getClassFile();
        InnerClassesAttribute ica = (InnerClassesAttribute)cf.getAttribute(InnerClassesAttribute.tag);
        String second = ica.innerClass(1);
        String secondName = ica.innerName(1);
        String third = ica.innerClass(2);
        String thirdName = ica.innerName(2);
        assertEquals(3, ica.remove(3));
        assertEquals(2, ica.remove(0));
        assertEquals(second, ica.innerClass(0));
        assertEquals(secondName, ica.innerName(0));
        assertEquals(third, ica.innerClass(1));
        assertEquals(thirdName, ica.innerName(1));
        assertEquals(1, ica.remove(1));
        assertEquals(second, ica.innerClass(0));
        assertEquals(secondName, ica.innerName(0));
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "run"));
    }

    public void testJIRA248() throws Exception {
        CtClass cc = sloader.get("test5.JIRA248");
        String methodBody = "public int run() { return foo() + super.foo() + super.bar() + test5.JIRA248Intf2.super.baz(); }";
        CtMethod ctMethod = CtMethod.make(methodBody, cc);
        cc.addMethod(ctMethod);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(40271, invoke(obj, "run"));
    }

    public void testInvalidCastWithDollar() throws Exception {
        String code = "{ new test5.JavassistInvalidCastTest().inspectReturn((Object) ($w) $_); } ";
        CtClass c = sloader.get("test5.InvalidCastDollar");
        for (CtMethod method : c.getDeclaredMethods())
            method.insertAfter(code);
    }

    public void testJIRA256() throws Exception {
        // CtClass ec = sloader.get("test5.Entity");

        CtClass cc = sloader.makeClass("test5.JIRA256");
        ClassFile ccFile = cc.getClassFile();
        ConstPool constpool = ccFile.getConstPool();
         
        AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
        javassist.bytecode.annotation.Annotation entityAnno
            = new javassist.bytecode.annotation.Annotation("test5.Entity", constpool);
            // = new javassist.bytecode.annotation.Annotation(constpool, ec);

        entityAnno.addMemberValue("value", new javassist.bytecode.annotation.ArrayMemberValue(constpool));
        attr.addAnnotation(entityAnno);
        ccFile.addAttribute(attr);

        cc.writeFile();
        Object o = make(cc.getName());
        assertTrue(o.getClass().getName().equals("test5.JIRA256"));

        java.lang.annotation.Annotation[] annotations = o.getClass().getDeclaredAnnotations();
        assertEquals(1, annotations.length); 
    }

    public void testProceedToDefaultMethod() throws Exception {
        CtClass cc = ClassPool.getDefault().get("test5.ProceedDefault");
        CtMethod mth = cc.getDeclaredMethod("bar");
        mth.instrument(new ExprEditor() {
            public void edit(MethodCall c) throws CannotCompileException {
                c.replace("$_ = $proceed($$) + 10000;");
            }
        });
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(21713, invoke(obj, "run"));
    }
}
