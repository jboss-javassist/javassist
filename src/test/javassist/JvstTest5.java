package javassist;

import java.lang.annotation.Annotation;
import java.lang.reflect.TypeVariable;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
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
        Boolean ss = Boolean.valueOf(2 > 3);
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

    public void testJIRA250() throws Exception {
        CtClass cc = sloader.makeClass("test5.JIRA250", sloader.get("test5.JIRA250Super"));
        cc.addMethod(CtNewMethod.make(
                "    public test5.JIRA250Bar getBar() {" + 
                "        return super.getBar();\n" +
                "    }\n", cc));
        cc.addMethod(CtNewMethod.make("public int run() { getBar(); return 1; }", cc));
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "run"));
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

    public void testBadClass() throws Exception {
        CtClass badClass = ClassPool.getDefault().makeClass("badClass");
        String src = String.join(System.getProperty("line.separator"),
                "public void eval () {",
                "    if (true) {",
                "        double t=0;",
                "    } else {",
                "        double t=0;",
                "    }",
                "    for (int i=0; i < 2; i++) {",
                "        int a=0;",
                "        int b=0;",
                "        int c=0;",
                "        int d=0;",
                "        if (true) {",
                "            int e = 0;",
                "        }",
                "    }",
                "}");
        System.out.println(src);
        badClass.addMethod(CtMethod.make(src, badClass));
        Class clazzz = badClass.toClass();
        Object obj = clazzz.getConstructor().newInstance(); // <-- falls here
    }

    public void test83StackmapWithArrayType() throws Exception {
    	final CtClass ctClass = sloader.get("test5.StackmapWithArray83");
        final CtMethod method = ctClass.getDeclaredMethod("bytecodeVerifyError");
        method.addLocalVariable("test_localVariable", CtClass.intType);
        method.insertBefore("{ test_localVariable = 1; }");

        final CtMethod method2 = ctClass.getDeclaredMethod("bytecodeVerifyError2");
        method2.addLocalVariable("test_localVariable", CtClass.intType);
        method2.insertBefore("{ test_localVariable = 1; }");

        ctClass.writeFile();
        Object obj = make(ctClass.getName());
        assertEquals(1, invoke(obj, "run"));
    }

    public void testLoaderClassPath() throws Exception {
        ClassPool cp = new ClassPool();
        cp.appendClassPath(new LoaderClassPath(new Loader()));
        assertNotNull(cp.get(Object.class.getName()));
        assertNotNull(cp.get(this.getClass().getName()));
    }

    public void testAddDefaultMethod() throws Exception {
        CtClass cc = sloader.makeInterface("test5.AddDefaultMethod");
        cc.addMethod(CtNewMethod.make("static int foo() { return 1; }", cc));
        cc.addMethod(CtNewMethod.make("public static int foo1() { return 1; }", cc));
        cc.addMethod(CtNewMethod.make("public int foo2() { return 1; }", cc));
        cc.addMethod(CtNewMethod.make("int foo3() { return 1; }", cc));
        try {
            cc.addMethod(CtNewMethod.make("private int foo4() { return 1; }", cc));
            fail();
        } catch (CannotCompileException e) {}
        try {
            cc.addMethod(CtNewMethod.make("private static int foo5() { return 1; }", cc));
            fail();
        } catch (CannotCompileException e) {}
    }

    public void testRemoveAnnotatino() throws Exception {
        CtClass cc = sloader.get("test5.RemoveAnnotation");
        AnnotationsAttribute aa
            = (AnnotationsAttribute)cc.getClassFile().getAttribute(AnnotationsAttribute.invisibleTag);
        assertTrue(aa.removeAnnotation("test5.RemoveAnno1"));
        AttributeInfo ai = cc.getClassFile().removeAttribute(AnnotationsAttribute.invisibleTag);
        assertEquals(ai.getName(), AnnotationsAttribute.invisibleTag);

        CtMethod foo = cc.getDeclaredMethod("foo");
        AnnotationsAttribute aa2 = (AnnotationsAttribute)foo.getMethodInfo().getAttribute(AnnotationsAttribute.invisibleTag);
        assertTrue(aa2.removeAnnotation("test5.RemoveAnno1"));

        CtMethod bar = cc.getDeclaredMethod("bar");
        AnnotationsAttribute aa3 = (AnnotationsAttribute)bar.getMethodInfo().getAttribute(AnnotationsAttribute.invisibleTag);
        assertFalse(aa3.removeAnnotation("test5.RemoveAnno1"));
        assertTrue(aa3.removeAnnotation("test5.RemoveAnno2"));
        AttributeInfo ai2 = bar.getMethodInfo().removeAttribute(AnnotationsAttribute.invisibleTag);
        assertEquals(ai2.getName(), AnnotationsAttribute.invisibleTag);

        CtMethod run = cc.getDeclaredMethod("run");
        AttributeInfo ai3 = run.getMethodInfo().removeAttribute(AnnotationsAttribute.invisibleTag);
        assertNull(ai3);

        CtField baz = cc.getDeclaredField("baz");
        AttributeInfo ai4 = baz.getFieldInfo().removeAttribute(AnnotationsAttribute.invisibleTag);
        assertEquals(ai4.getName(), AnnotationsAttribute.invisibleTag);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "run"));
    }
}
