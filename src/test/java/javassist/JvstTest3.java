package javassist;

import javassist.bytecode.*;
import javassist.bytecode.annotation.*;
import javassist.expr.*;
import test3.*;

@SuppressWarnings({"rawtypes","unchecked","unused"})
public class JvstTest3 extends JvstTestRoot {
    public JvstTest3(String name) {
         super(name);
    }

    public void testAnnotation() throws Exception {
        CtClass cc = sloader.get("test3.AnnoTest");
        Object[] all = cc.getAnnotations();
        Anno a = (Anno)all[0];
        assertEquals('0', a.c());
        assertEquals(true, a.bool());
        assertEquals(1, a.b());
        assertEquals(2, a.s());
        assertEquals(3, a.i());
        assertEquals(4L, a.j());
        assertEquals(5.0F, a.f());
        assertEquals(6.0, a.d());
        assertEquals("7", a.str());
        assertEquals(AnnoTest.class, a.clazz());
        assertEquals(3, a.anno2().str().length);
    }

    public void testAnnotation2() throws Exception {
        CtClass cc = sloader.get("test3.AnnoTest2");
        Object[] all = cc.getAnnotations();
        Anno a = (Anno)all[0];
        assertEquals('a', a.c());
        assertEquals(false, a.bool());
        assertEquals(11, a.b());
        assertEquals(12, a.s());
        assertEquals(13, a.i());
        assertEquals(14L, a.j());
        assertEquals(15.0F, a.f());
        assertEquals(16.0, a.d());
        assertEquals("17", a.str());
        assertEquals(String.class, a.clazz());
        assertEquals(11, a.anno2().i()[0]);
    }

    public void testAnnotation3() throws Exception {
        CtClass cc = sloader.get("test3.AnnoTest3");
        Object[] all = cc.getAnnotations();
        assertEquals(2, all.length);
        int i;
        if (all[0] instanceof Anno2)
            i = 0;
        else
            i = 1;

        Anno2 a = (Anno2)all[i];
        assertEquals(1, a.i()[0]);
        assertEquals(test3.ColorName.RED, a.color());
        assertEquals(test3.ColorName.BLUE, a.color2()[0]);
    }

    public void testAnnotation4() throws Exception {
        CtClass cc = sloader.get("test3.AnnoTest4");
        Object[] all = cc.getAnnotations();
        Anno3 a = null;
        for (int i = 0; i < all.length; i++)
            if (all[i] instanceof Anno3)
                a = (Anno3)all[i];

        assertTrue(a != null);
        assertEquals('0', a.c()[0]);
        assertEquals(true, a.bool()[0]);
        assertEquals(1, a.b()[0]);
        assertEquals(2, a.s()[0]);
        assertEquals(3, a.i()[0]);
        assertEquals(4L, a.j()[0]);
        assertEquals(5.0F, a.f()[0]);
        assertEquals(6.0, a.d()[0]);
        assertEquals("7", a.str()[0]);
        assertEquals(AnnoTest.class, a.clazz()[0]);
        assertEquals(11, a.anno2()[0].i()[0]);
    }

    public void testAnnotation5() throws Exception {
        CtClass cc = sloader.get("test3.AnnoTest5");
        Object[] all = cc.getField("bar").getAnnotations();
        Anno2 a2 = (Anno2)all[0];
        assertEquals(test3.ColorName.RED, a2.color());

        all = cc.getDeclaredMethod("foo").getAnnotations();
        Anno a = (Anno)all[0];
        assertEquals("7", a.str());
    }

    public void testAnnotation6() throws Exception {
        CtClass cc = sloader.get("test3.AnnoTest6");
        Object[] all = cc.getAnnotations();
        Anno6 a = (Anno6)all[0];
        assertEquals(0, a.str1().length);
        assertEquals(0, a.str2().length);
    }

    public void testChainedException() throws Exception {
        try {
            throwChainedException();
        }
        catch (CannotCompileException e) {
            e.printStackTrace(System.out);
        }

        try {
            throwChainedException2();
        }
        catch (CannotCompileException e) {
            e.printStackTrace(System.out);
        }

        try {
            throwChainedException3();
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

    public void throwChainedException() throws Exception {
        throw new CannotCompileException("test");
    }

    public void throwChainedException2() throws Exception {
        Throwable e = new CannotCompileException("test");
        throw new CannotCompileException("test2", e);
    }

    public void throwChainedException3() throws Exception {
        Throwable e = new CannotCompileException("testA");
        Throwable e2 = new CannotCompileException("testB", e);
        throw new Exception(e2);
    }

    // JIRA Javassist-12
    public void testInnerClassMethod() throws Exception {
        CtClass cc = sloader.get("test3.InnerMethod");
        CtMethod m1 = cc.getDeclaredMethod("test");
        m1.setBody("{inner.test();}");

        CtMethod m2 = CtNewMethod.make(
            "public int bar() {"
          + "  if (counter-- <= 0) return 3;"
          + "  else return bar();"
          + "}",
            cc);
        cc.addMethod(m2);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "foo"));
        assertEquals(3, invoke(obj, "bar"));
    }

    public void testCheckModifyAndPruned() throws Exception {
        CtClass cc = sloader.get("test3.CheckModify");
        cc.addField(new CtField(CtClass.intType, "j", cc));
        cc.stopPruning(false);
        cc.toBytecode();
        try {
            cc.getClassFile();
            fail();
        }
        catch (RuntimeException e) {
            // System.err.println(e.getMessage());
            assertTrue(e.getMessage().indexOf("prune") >= 0);
        }
    }

    public void testReplaceNew() throws Exception {
        CtClass cc = sloader.get("test3.ReplaceNew");
        CtMethod m1 = cc.getDeclaredMethod("run");
        m1.instrument(new ExprEditor() {
            public void edit(NewExpr n) throws CannotCompileException {
                n.replace("{ i++; $_ = null; }");
            }
        });

        CtMethod m2 = cc.getDeclaredMethod("run2");
        m2.instrument(new ExprEditor() {
            public void edit(NewExpr n) throws CannotCompileException {
                n.replace("{ j++; $_ = null; }");
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(5, invoke(obj, "run"));
        assertEquals(2, invoke(obj, "run2"));
    }

    public void testPublicInner() throws Exception {
        CtClass cc0 = sloader.get("test3.PublicInner2");
        int mod = cc0.getClassFile2().getAccessFlags();
        System.out.println("testPublicInner: " + mod);

        CtClass cc = sloader.get("test3.PublicInner");
        CtClass jp = cc.makeNestedClass("Inner", true);
        assertEquals(Modifier.PUBLIC | Modifier.STATIC, jp.getModifiers());
        assertEquals(Modifier.PUBLIC | Modifier.STATIC,
                     getPublicInner(jp, "Inner"));
        assertEquals(Modifier.PUBLIC | Modifier.STATIC,
                     getPublicInner(cc, "Inner"));

        jp.setModifiers(Modifier.STATIC);
        assertEquals(Modifier.STATIC, jp.getModifiers());
        assertEquals(Modifier.STATIC, getPublicInner(jp, "Inner"));
        assertEquals(Modifier.STATIC, getPublicInner(cc, "Inner"));

        jp.setModifiers(Modifier.PUBLIC | Modifier.ABSTRACT);
        assertEquals(Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.STATIC, jp.getModifiers());
        assertEquals(Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.STATIC,
                     getPublicInner(jp, "Inner"));
        assertEquals(Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.STATIC,
                     getPublicInner(cc, "Inner"));

        cc.writeFile();
        jp.writeFile();
    }

    private int getPublicInner(CtClass cc, String name) throws Exception {
        ClassFile cf = cc.getClassFile();
        InnerClassesAttribute ica
            = (InnerClassesAttribute)cf.getAttribute(
                                         InnerClassesAttribute.tag);
        assertEquals(name, ica.innerName(0));
        return ica.accessFlags(0);
    }

    public void testConstructorToMethod() throws Exception {
        CtClass cc = sloader.get("test3.Constructor");
        CtConstructor[] cons = cc.getConstructors();
        CtConstructor sinit = cc.getClassInitializer();

        for (int i = 0; i < cons.length; i++) {
            CtConstructor ccons = cons[i];
            String desc = ccons.getSignature();
            boolean result = false;
            if (desc.equals("()V"))
                result = false;
            else if (desc.equals("(I)V"))
                result = true;
            else if (desc.equals("(Ljava/lang/String;)V"))
                result = false;
            else if (desc.equals("(D)V"))
                result = true;
            else
                fail("unknonw constructor");

            assertEquals(result, ccons.callsSuper());
        }

        CtClass cc2 = sloader.get("test3.Constructor2");
        for (int i = 0; i < cons.length; i++)
            cc2.addMethod(cons[i].toMethod("m", cc2));

        cc2.addMethod(sinit.toMethod("sinit", cc2));
        cc2.addMethod(CtMethod.make(
            "public int run() { m(); m(5); m(\"s\"); m(0.0);" +
            "  sinit(); return i + str.length(); }",
            cc2));
        cc2.writeFile();

        Object obj = make(cc2.getName());
        assertEquals(119, invoke(obj, "run"));
    }

    public void testUnique() throws Exception {
        CtClass cc = sloader.get("test3.Unique");
        CtClass cc2 = sloader.get("test3.Unique3");
        assertEquals("poi", cc.makeUniqueName("poi"));
        assertEquals("foo102", cc.makeUniqueName("foo"));
        assertEquals("bar102", cc2.makeUniqueName("bar"));
        assertEquals("foo100", cc2.makeUniqueName("foo"));
    }

    public void testGetMethods() throws Exception {
        CtClass cc = sloader.get("test3.GetMethods");
        assertEquals(3, cc.getConstructors().length);
        assertEquals(6, cc.getFields().length);
        assertEquals(6 + Object.class.getMethods().length + 2,
                     cc.getMethods().length);
    }

    public void testVisiblity() throws Exception {
        CtClass cc = sloader.get("test3.Visible");
        CtClass cc2 = sloader.get("test3.Visible2");
        CtClass subcc = sloader.get("test3.sub.Visible");
        CtClass subcc2 = sloader.get("test3.sub.Visible2");
        CtClass top = sloader.get("VisibleTop");
        CtClass top2 = sloader.get("VisibleTop2");

        assertEquals(true, cc.getField("pub").visibleFrom(cc2));
        assertEquals(true, cc.getField("pub").visibleFrom(subcc));

        assertEquals(true, cc.getField("pri").visibleFrom(cc));
        assertEquals(false, cc.getField("pri").visibleFrom(cc2));

        assertEquals(true, cc.getField("pack").visibleFrom(cc));
        assertEquals(true, cc.getField("pack").visibleFrom(cc2));
        assertEquals(false, cc.getField("pack").visibleFrom(subcc));
        assertEquals(false, cc.getField("pack").visibleFrom(top));

        assertEquals(true, cc.getField("pro").visibleFrom(cc));
        assertEquals(true, cc.getField("pro").visibleFrom(cc2));
        assertEquals(true, cc.getField("pro").visibleFrom(subcc2));
        assertEquals(false, cc.getField("pro").visibleFrom(subcc));
        assertEquals(false, cc.getField("pro").visibleFrom(top));

        assertEquals(true, top.getField("pack").visibleFrom(top2));
        assertEquals(false, top.getField("pack").visibleFrom(cc));
    }

    public void testNewAnnotation() throws Exception {
        CtClass c = sloader.makeClass("test3.NewClass");
        ClassFile cf = c.getClassFile();
        ConstPool cp = cf.getConstPool();
        AnnotationsAttribute attr
            = new AnnotationsAttribute(cp, AnnotationsAttribute.visibleTag);
        javassist.bytecode.annotation.Annotation a
            = new Annotation("test3.ChibaAnnotation", cp);
        a.addMemberValue("name", new StringMemberValue("Chiba", cp));
        a.addMemberValue("version", new StringMemberValue("Chiba", cp));
        a.addMemberValue("description", new StringMemberValue("Chiba", cp));
        a.addMemberValue("interfaceName", new StringMemberValue("Chiba", cp));
        attr.setAnnotation(a);
        System.out.println(attr);
        cf.addAttribute(attr);
        cf.setVersionToJava5();
        Object [] ans = c.getAnnotations() ;
        System.out.println("Num Annotation : " +ans.length);

        // c.debugWriteFile();
        Class newclass = c.toClass(DefineClassCapability.class);
        java.lang.annotation.Annotation[] anns = newclass.getAnnotations();
        System.out.println("Num NewClass Annotation : " +anns.length);
        assertEquals(ans.length, anns.length);
    }

    public void testRecursiveReplace() throws Exception {
        CtClass cc = sloader.get("test3.RecReplace");
        CtMethod m1 = cc.getDeclaredMethod("run");
        final ExprEditor e2 = new ExprEditor() {
            public void edit(MethodCall mc) throws CannotCompileException {
                if (mc.getMethodName().equals("bar")) {
                    mc.replace("{ double k = 10.0; $_ = $proceed($1 + k); }");
                }
            }
        };
        m1.instrument(new ExprEditor() {
            public void edit(MethodCall mc) throws CannotCompileException {
                if (mc.getMethodName().equals("foo")) {
                    mc.replace("{ int k = bar($$); $_ = k + $proceed(7.0); }",
                    e2);
                }
            }
        });

        CtMethod m2 = cc.getDeclaredMethod("run2");
        m2.instrument(new ExprEditor() {
            public void edit(MethodCall mc) throws CannotCompileException {
                if (mc.getMethodName().equals("foo")) {
                    mc.replace("{ int k = bar($$); $_ = k + $proceed(7.0); }");
                }
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(26, invoke(obj, "run"));
        assertEquals(16, invoke(obj, "run2"));
    }

    public void testRecursiveReplace2() throws Exception {
        final ExprEditor[] ref = new ExprEditor[1];
        ExprEditor e2 = new ExprEditor() {
            public void edit(FieldAccess fa) throws CannotCompileException {
                if (fa.getFieldName().equals("value2")
                    && fa.isWriter()) {
                    fa.replace("{ $_ = $proceed($1 + 2); }");
                }
            }
        };
        ExprEditor e1 = new ExprEditor() {
            public void edit(FieldAccess fa) throws CannotCompileException {
                if (fa.getFieldName().equals("value")
                    && fa.isWriter()) {
                    fa.replace("{ value2 = $1; value = value2; }",
                               ref[0]);
                }
            }
        };

        CtClass cc = sloader.get("test3.RecReplace2");
        CtMethod m1 = cc.getDeclaredMethod("run");
        ref[0] = e2;
        m1.instrument(e1);
        CtMethod m2 = cc.getDeclaredMethod("run2");
        ref[0] = null;
        m2.instrument(e1);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(28, invoke(obj, "run"));
        assertEquals(24, invoke(obj, "run2"));
    }

    public void testInnerModifier() throws Exception {
        CtClass cc = sloader.get("test3.InnerClass$Inner");
        assertEquals(Modifier.PUBLIC | Modifier.STATIC, cc.getModifiers());
        CtClass cc2 = sloader.get("test3.InnerClass$Inner2");
        assertEquals(Modifier.PUBLIC, cc2.getModifiers());
    }

    public void testMethodLookup() throws Exception {
        CtClass cc = sloader.get("test3.SubValue");
        CtMethod m1 = CtNewMethod.make(
                "public int run() {" +
                " test3.SuperValue sup = new test3.SuperValue();" +
                " test3.SubValue sub = new test3.SubValue();" +
                " return this.after(sup, sub, sub) == null ? 0 : 1;" +
                "}",
                cc);
        cc.addMethod(m1);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "run"));
    }

    public void testFieldAccessType() throws Exception {
        CtClass cc = sloader.get("test3.FieldAccessType");
        CtMethod m1 = cc.getDeclaredMethod("access");
        final boolean[] result = new boolean[1];
        result[0] = true;
        ExprEditor e = new ExprEditor() {
            public void edit(FieldAccess fa) throws CannotCompileException {
                if (!fa.getSignature().equals("[I"))
                    result[0] = false;
            }
        };
        m1.instrument(e);
        assertTrue(result[0]);
    }

    public void testGetNestedClasses() throws Exception {
        CtClass cc = sloader.get("test3.NestedClass");
        CtClass[] nested = cc.getNestedClasses();
        assertEquals(4, nested.length);
        testGetNestedClasses("test3.NestedClass$Inner", nested);
        testGetNestedClasses("test3.NestedClass$StaticNested", nested);
        testGetNestedClasses("test3.NestedClass$1Local", nested);
        testGetNestedClasses("test3.NestedClass$1", nested);
    }

    private void testGetNestedClasses(String name, CtClass[] classes) {
        for (int i = 0; i < classes.length; i++)
            if (classes[i].getName().equals(name))
                return;

        fail("no class: " + name);
    }

    public void testGetParmeterAnnotations() throws Exception {
        CtClass cc = sloader.get("test3.ParamAnno");
        Object[][] anno = cc.getDeclaredMethod("foo").getParameterAnnotations();
        assertEquals(4, anno.length);
        assertEquals(0, anno[0].length);
        assertEquals(0, anno[1].length);
        assertEquals(0, anno[2].length);
        assertEquals(0, anno[3].length);

        Object[][] anno2 = cc.getDeclaredMethod("bar").getParameterAnnotations();
        assertEquals(0, anno2.length);

        Class rc = Class.forName("test3.ParamAnno");
        java.lang.reflect.Method[] ms = rc.getDeclaredMethods();
        java.lang.reflect.Method m1, m2;
        if (ms[0].getName().equals("foo")) {
            m1 = ms[0];
            m2 = ms[1];
        }
        else {
            m1 = ms[1];
            m2 = ms[0];
        }

        java.lang.annotation.Annotation[][] ja;
        ja = m1.getParameterAnnotations();
        assertEquals(4, ja.length);
        assertEquals(0, ja[0].length);
        assertEquals(0, ja[1].length);
        assertEquals(0, ja[2].length);
        assertEquals(0, ja[3].length);

        assertEquals(0, m2.getParameterAnnotations().length);
    }

    public void testSetModifiers() throws Exception {
        CtClass cc = sloader.get("test3.SetModifiers");
        try {
            cc.setModifiers(Modifier.STATIC | Modifier.PUBLIC);
            fail("static public class SetModifiers");
        }
        catch (RuntimeException e) {
            assertEquals("cannot change test3.SetModifiers into a static class", e.getMessage());
        }

        cc = sloader.get("test3.SetModifiers$A");
        cc.setModifiers(Modifier.STATIC | Modifier.PUBLIC);
        assertTrue(Modifier.isStatic(cc.getModifiers()));
        assertTrue((cc.getClassFile2().getAccessFlags() & AccessFlag.STATIC) == 0);
    }

    public void testFieldCopy() throws Exception {
        CtClass cc = sloader.get("test3.FieldCopy");
        CtClass cc2 = sloader.get("test3.FieldCopy2");
        CtField f = cc.getDeclaredField("foo");
        cc2.addField(new CtField(f, cc2));
        CtField f2 = cc2.getDeclaredField("foo");
        Object[] anno = f2.getAnnotations();
        assertTrue(anno[0] instanceof test3.FieldCopy.Test);
        assertEquals(Modifier.PRIVATE | Modifier.STATIC,
                     f2.getModifiers());
    }

    public void testMethodRedirect() throws Exception {
        CtClass cc = sloader.get("test3.MethodRedirect");
        CtClass cc2 = sloader.get("test3.MethodRedirectIntf");
        CtMethod foo = cc.getDeclaredMethod("foo");
        CtMethod poi = cc.getDeclaredMethod("poi");
        CtMethod bar = cc.getDeclaredMethod("bar");
        CtMethod afo = cc2.getDeclaredMethod("afo");
        CodeConverter conv = new CodeConverter();

        try {
            conv.redirectMethodCall(foo, bar);
            fail("foo");
        }
        catch (CannotCompileException e) {}

        try {
            conv.redirectMethodCall(poi, bar);
            fail("bar");
        }
        catch (CannotCompileException e) {}

        try {
            conv.redirectMethodCall(bar, afo);
            fail("afo");
        }
        catch (CannotCompileException e) {}
        bar.setName("bar2");
        conv.redirectMethodCall("bar", bar);
        cc.instrument(conv);
        // cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(2, invoke(obj, "test"));
    }

    public void testMethodRedirect2() throws Exception {
        CtClass cc = sloader.get("test3.MethodRedirect2");
        CtClass sup = sloader.get("test3.MethodRedirect2Sup");
        CtClass supsup = sloader.get("test3.MethodRedirect2SupSup");
        CtClass intf = sloader.get("test3.MethodRedirect2SupIntf");
        CtMethod bfo2 = supsup.getDeclaredMethod("bfo2");
        CtMethod afo2 = sup.getDeclaredMethod("afo2");
        CtMethod foo = intf.getDeclaredMethod("foo");
        CodeConverter conv = new CodeConverter();

        conv.redirectMethodCall("bfo", bfo2);
        conv.redirectMethodCall("afo", afo2);
        conv.redirectMethodCall("bar", foo);
        conv.redirectMethodCall("bar2", foo);
        cc.instrument(conv);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(524, invoke(obj, "test"));
    }

    public void testMethodRedirectToStatic() throws Exception {
        CtClass targetClass = sloader.get("test3.MethodRedirectToStatic");
        CtClass staticClass = sloader.get("test3.MethodRedirectToStatic2");
        CtMethod targetMethod = targetClass.getDeclaredMethod("add");
        CtMethod staticMethod = staticClass.getDeclaredMethod("add2");
        CodeConverter conv = new CodeConverter();

        conv.redirectMethodCallToStatic(targetMethod, staticMethod);
        targetClass.instrument(conv);
        targetClass.writeFile();
        Object obj = make(targetClass.getName());
        assertEquals(30, invoke(obj, "test"));
    }

    public void testClassMap() throws Exception {
        ClassMap map = new ClassMap();
        map.put("aa", "AA");
        map.put("xx", "XX");
        assertEquals("AA", map.get("aa"));
        assertEquals(null, map.get("bb"));
        ClassMap map2 = new ClassMap(map);
        map2.put("aa", "A1");
        map2.put("cc", "CC");
        assertEquals("A1", map2.get("aa"));
        assertEquals("CC", map2.get("cc"));
        assertEquals("XX", map2.get("xx"));
        assertEquals(null, map2.get("bb"));
    }

    public void testEmptyConstructor() throws Exception {
        CtClass cc = sloader.get("test3.EmptyConstructor");
        CtConstructor[] cons = cc.getDeclaredConstructors();
        for (int i = 0; i < cons.length; i++)
            assertTrue("index: " + i, cons[i].isEmpty());

        cc = sloader.get("test3.EmptyConstructor2");
        cons = cc.getDeclaredConstructors();
        for (int i = 0; i < cons.length; i++)
            assertTrue("index: " + i, cons[i].isEmpty());

        cc = sloader.get("test3.EmptyConstructor3");
        cons = cc.getDeclaredConstructors();
        for (int i = 0; i < cons.length; i++)
            assertTrue("index: " + i, cons[i].isEmpty());

        cc = sloader.get("test3.EmptyConstructor4");
        cons = cc.getDeclaredConstructors();
        for (int i = 0; i < cons.length; i++)
            assertFalse("index: " + i, cons[i].isEmpty());
    }

    public void testTransformRead() throws Exception {
        CtClass cc = sloader.get("test3.TransformRead");
        CtClass parent = cc.getSuperclass();
        CtMethod m = cc.getDeclaredMethod("foo");
        CodeConverter conv = new CodeConverter();
        conv.replaceFieldRead(parent.getField("value"), cc, "getValue");
        conv.replaceFieldRead(parent.getField("value2"), cc, "getValue2");
        m.instrument(conv);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(11100, invoke(obj, "foo"));
    }

    public void testDescriptor() throws Exception {
        assertEquals("int", Descriptor.toString("I")); 
        assertEquals("long[][]", Descriptor.toString("[[J")); 
        assertEquals("java.lang.Object", Descriptor.toString("Ljava/lang/Object;"));
        assertEquals("()", Descriptor.toString("()V")); 
        assertEquals("(int)", Descriptor.toString("(I)V")); 
        assertEquals("(int,int[])", Descriptor.toString("(I[I)V"));
        assertEquals("(java.lang.String,Foo[][])", Descriptor.toString("(Ljava/lang/String;[[LFoo;)V"));
    }

    public void testLongName() throws Exception {
        CtClass cc = sloader.get("test3.Name");
        assertEquals("test3.Name.<clinit>()", cc.getClassInitializer().getLongName());
        assertEquals("test3.Name()", cc.getConstructor("()V").getLongName());
        assertEquals("test3.Name(int)", cc.getConstructor("(I)V").getLongName());
        assertEquals("test3.Name(test3.Name)", cc.getConstructor("(Ltest3/Name;)V").getLongName());
        assertEquals("test3.Name(test3.Name,java.lang.String)",
                     cc.getConstructor("(Ltest3/Name;Ljava/lang/String;)V").getLongName());

        assertEquals("test3.Name.foo()", cc.getDeclaredMethod("foo").getLongName());
        assertEquals("test3.Name.foo2(int)", cc.getDeclaredMethod("foo2").getLongName());
        assertEquals("test3.Name.foo3(java.lang.String)", cc.getDeclaredMethod("foo3").getLongName());
        assertEquals("test3.Name.foo4(java.lang.String[])", cc.getDeclaredMethod("foo4").getLongName());
        assertEquals("test3.Name.foo5(int,java.lang.String)", cc.getDeclaredMethod("foo5").getLongName());
    }

    public void testPackageName() throws Exception {
        CtClass cc = sloader.get("test3.PackName");
        CtMethod m1 = CtNewMethod.make(
                "public int run() {" +
                " return test3.PackName.get() + test3.sub.SubPackName.get(); }",
                cc);
        cc.addMethod(m1);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(8, invoke(obj, "run"));
    }

    public void testErasure() throws Exception {
        CtClass cc = sloader.get("test3.Erasure");
        cc.addInterface(sloader.get("test3.ErasureGet"));
        CtMethod m1 = CtNewMethod.make(
                "public Object get() { return value; }",
                cc);
        cc.addMethod(m1);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(4, invoke(obj, "run"));
    }

    /* Check the result of JvstTest#testFieldInit()
     * This tests CtClassType#saveClassFile().
     */
    public void testFieldInitAgain() throws Exception {
        System.gc();
        CtClass cc = sloader.get("test1.FieldInit");
        CtField f = cc.getDeclaredField("f1");
        assertEquals(CtClass.intType, f.getType());
        assertTrue("f.hashCode() doesn't change!", f.hashCode() != JvstTest.testFieldInitHash);
    }

    /* This tests CtClassType#saveClassFile().
     * A CtMethod is not garbage collected, its CtClass is never
     * compressed.
     */
    public void testCalleeBeforeAgain() throws Exception {
        CtClass cc = sloader.get("test1.CalleeBefore");
        assertEquals(JvstTest.testCalleeBeforeMethod,
                     cc.getDeclaredMethod("m1"));
        assertEquals(JvstTest.testCalleeBeforeMethod2,
                     cc.getDeclaredMethod("m2").getMethodInfo2().hashCode());
    }

    public void testSetSuper() throws Exception {
        CtClass cc = sloader.get("test3.Superclass");
        CtClass cc3 = sloader.get("test3.Superclass3");
        cc3.setModifiers(Modifier.setPublic(cc3.getModifiers()));
        cc.setSuperclass(sloader.get("test3.Superclass3"));
        cc.writeFile();
        cc3.writeFile();
        Object obj = make(cc.getName());
        assertEquals(21, invoke(obj, "test"));
    }

    public void testFrozen2() throws Exception {
        CtClass cc = sloader.get("test3.Frozen");
        cc.addField(new CtField(CtClass.intType, "k", cc));
        cc.toBytecode();
        cc.toBytecode();
        cc = sloader.makeClass("test3.Frozen2");
        cc.toBytecode();
        cc.toBytecode();
    }

    public void testCopyAnnotation() throws Exception {
        CtClass cc1 = sloader.get("test3.CopyAnnoBase");
        CtMethod m1 = cc1.getDeclaredMethod("getX");
        CtClass cc2 = sloader.get("test3.CopyAnno");
        CtMethod m2 = cc2.getDeclaredMethod("getX");
        copyAnnotations(m1, m2);
        cc2.getClassFile();
        Class clazz = cc2.toClass(DefineClassCapability.class);
        java.lang.reflect.Method m = clazz.getDeclaredMethod("getX", new Class[0]);
        assertEquals(1, m.getAnnotations().length);
        test3.VisibleAnno a = m.getAnnotation(test3.VisibleAnno.class);
        assertNotNull(a);
    }

    private void copyAnnotations(CtMethod src, CtMethod dest)
        throws NotFoundException
    {
       MethodInfo srcInfo = src.getMethodInfo2();
       MethodInfo destInfo = dest.getMethodInfo2();
       copyAnnotations(srcInfo, destInfo, AnnotationsAttribute.invisibleTag);
       copyAnnotations(srcInfo, destInfo, AnnotationsAttribute.visibleTag);
    }

    private void copyAnnotations(MethodInfo src, MethodInfo dest, String annotationTag) {
       AnnotationsAttribute attribute = (AnnotationsAttribute)src.getAttribute(annotationTag);
       if (attribute != null)
          dest.addAttribute(attribute.copy(dest.getConstPool(), new java.util.HashMap()));
    }

    public void testStaticFinalField() throws Exception {
        CtClass cc = sloader.makeClass("test3.StaticFinalField");
        CtField fj = new CtField(CtClass.longType, "j", cc);
        fj.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        cc.addField(fj, CtField.Initializer.constant(2L));
        CtField fi = new CtField(CtClass.intType, "i", cc);
        fi.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        cc.addField(fi, CtField.Initializer.constant(3));
        CtField fs = new CtField(CtClass.shortType, "s", cc);
        fs.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        cc.addField(fs, CtField.Initializer.constant(4));
        CtField fc = new CtField(CtClass.charType, "c", cc);
        fc.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        cc.addField(fc, CtField.Initializer.constant('5'));
        CtField fby = new CtField(CtClass.byteType, "by", cc);
        fby.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        cc.addField(fby, CtField.Initializer.constant(6));
        CtField fb = new CtField(CtClass.booleanType, "b", cc);
        fb.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        cc.addField(fb, CtField.Initializer.constant(true));
        CtField ff = new CtField(CtClass.floatType, "f", cc);
        ff.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        cc.addField(ff, CtField.Initializer.constant(7.0F));
        CtField fstr = new CtField(sloader.get("java.lang.String"), "str", cc);
        fstr.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        cc.addField(fstr, CtField.Initializer.constant("foo"));
        CtField fobj = new CtField(sloader.get("java.lang.Object"), "obj", cc);
        fobj.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        cc.addField(fobj, CtField.Initializer.constant("bar"));

        cc.writeFile();
        Class clazz = cc.toClass(DefineClassCapability.class);
        assertEquals(2L, clazz.getField("j").getLong(null));
        assertEquals(3, clazz.getField("i").getInt(null));
        assertEquals(4, clazz.getField("s").getShort(null));
        assertEquals('5', clazz.getField("c").getChar(null));
        assertEquals(6, clazz.getField("by").getByte(null));
        assertEquals(true, clazz.getField("b").getBoolean(null));
        assertEquals(7.0F, clazz.getField("f").getFloat(null));
        assertEquals("foo", clazz.getField("str").get(null));
        assertEquals("bar", clazz.getField("obj").get(null));
    }

    /*
    public void testClassPath() throws Exception {
        ClassPool cp = new ClassPool(null);
        cp.appendClassPath("./test-classpath.JaR");
        assertNotNull(cp.get("test.Point"));
        cp = new ClassPool(null);
        cp.appendClassPath("./*");
        assertNotNull(cp.get("javassist.bytecode.Gap0Example"));
    }*/

    public void testVoidReturn() throws Exception {
        CtClass cc = sloader.get("test3.VoidReturn");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        m1.insertAfter("System.out.println(\"return value: \" + $_);", true);
        cc.writeFile();
        make(cc.getName());
    }

    public void testInsertParam0() throws Exception {
        assertEquals("(I)V", Descriptor.insertParameter(CtClass.intType, "()V"));
        assertEquals("(ILjava/lang/Object;)V", Descriptor.insertParameter(CtClass.intType, "(Ljava/lang/Object;)V"));
        assertEquals("(IJ)V", Descriptor.appendParameter(CtClass.longType, "(I)V"));
        assertEquals("(Ljava/lang/String;I)V", Descriptor.insertParameter(sloader.get("java.lang.String"), "(I)V"));
    }

    public void testInsertParam() throws Exception {
        CtClass cc = sloader.get("test3.InsParam");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        m1.insertParameter(CtClass.longType);
        m1.insertBefore("$2 += (int)$1;");

        CtMethod m2 = cc.getDeclaredMethod("bar");
        m2.addParameter(CtClass.doubleType);
        m2.insertBefore("$1 += (int)$2;");

        CtMethod m3 = cc.getDeclaredMethod("poi");
        m3.addParameter(sloader.get("java.lang.Object"));
        m3.insertBefore("$2 = (String)$3;");

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(11, invoke(obj, "foo", 10L, 1));
        assertEquals(11, invoke(obj, "bar", 1, 10.0));
        assertEquals(3, invoke(obj, "poi", 1, "x", "xx"));
    }

    private int invoke(Object target, String method, long arg1, int arg2)
        throws Exception
    {
        java.lang.reflect.Method m =
            target.getClass().getMethod(method, new Class[] { long.class, int.class });
        Object res = m.invoke(target, new Object[] { Long.valueOf(arg1), Integer.valueOf(arg2)});
        return ((Integer)res).intValue();
    }

    private int invoke(Object target, String method, int arg1, double arg2)
        throws Exception
    {
        java.lang.reflect.Method m =
            target.getClass().getMethod(method, new Class[] { int.class, double.class });
        Object res = m.invoke(target, new Object[] { Integer.valueOf(arg1), Double.valueOf(arg2)});
        return ((Integer)res).intValue();
    }

    private int invoke(Object target, String method, int arg1, String arg2, Object arg3)
        throws Exception
    {
        java.lang.reflect.Method m =
            target.getClass().getMethod(method, new Class[] { int.class, String.class, Object.class });
        Object res = m.invoke(target, new Object[] { Integer.valueOf(arg1), arg2, arg3});
        return ((Integer)res).intValue();
    }

    public void testInvokeinterface() throws Exception {
        // JIRA JASSIST-60
        CtClass cc = sloader.get("test3.InvokeIntf");
        CtMethod mth = cc.getDeclaredMethod("doit");
        ExprEditor e = new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                String block = "{$_=$proceed($$);}";
                m.replace(block);
            }
        };
        mth.instrument(e);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(7, invoke(obj, "test"));
    }

    public void testInvokeArrayObj() throws Exception {
        // JIRA JASSIST-61
        CtClass cc = sloader.get("test3.InvokeArray");
        CtMethod mth = cc.getDeclaredMethod("doit");
        ExprEditor e = new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                String block = "{$_=$proceed($$);}";
                m.replace(block);
            }
        };
        mth.instrument(e);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "test"));
    }

    public void testNewExprTry() throws Exception {
        CtClass cc = sloader.get("test3.NewExprTryCatch");
        CtMethod mth = cc.getDeclaredMethod("instrumentMe");
        ExprEditor e = new ExprEditor() {
            public void edit(NewExpr m) throws CannotCompileException {
                String block = "{$_=$proceed($$);}";
                m.replace(block);
            }
        };
        mth.instrument(e);

        // JIRA JASSIST-52
        CtMethod mth2 = cc.getDeclaredMethod("me2");
        ExprEditor e2 = new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                String block = "{try{$_=$proceed($$);} catch(Throwable t) {}}";
                m.replace(block);
            }
        };
        //mth2.instrument(e2);

        /*
        // JIRA JASSIST-53
        CtClass cc2 = sloader.get("test3.NewExprTryCatch2");
        CtBehavior mth3 = cc2.getDeclaredConstructors()[0];
        ExprEditor e3 = new ExprEditor() {
            public void edit(ConstructorCall m) throws CannotCompileException {
                String block = "{try {$_=$proceed($$);} catch (Throwable t) {}}";
                m.replace(block);
            }
        };
        mth3.instrument(e3);
        */

        cc.writeFile();
        // cc2.writeFile();
        Object obj = make(cc.getName());
        assertEquals(0, invoke(obj, "test"));
        // Object obj2 = make(cc2.getName());
    }

    public void testJIRA63() throws Exception {
        frameTypeTest(2);
        frameTypeTest(7);
    }

    private void frameTypeTest(final int initializerRepeatCount) throws Exception {
        // Get a class
        final CtClass cTst = sloader.get("test3.JIRA63");
        cTst.getClassFile().setMajorVersion(ClassFile.JAVA_6);
        try {
            // Create an initializer for the fields
            String initializer = "test3.JIRA63Helper";
            for(int i=0; i < initializerRepeatCount; i++)
                initializer += ".getAnObject(new Integer(1))";

            initializer += ";";

            // Add some fields
            final CtClass cObj = sloader.get("java.lang.Object");
            for(int i = 0; i < 7; i++)
                cTst.addField(new CtField(cObj, "a" + i, cTst), initializer);

            // Modify the constructors
            for(final CtConstructor m : cTst.getConstructors()) {
                m.insertAfter(initializer, true);
                m.insertBefore(initializer);
            }

            // Get the byte code.
            // cTst.toBytecode();
            cTst.writeFile();
            //make(cTst.getName());
        }
        finally {
            cTst.detach();
        }
    }

    public void testInsertBeforeType() throws Exception {
        CtClass cc = sloader.get("test3.InsertBeforeType");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        m1.insertBefore("value = $type.getName();");
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(5, invoke(obj, "test")); 
    }

    public void testTransformNewClass() throws Exception {
        CodeConverter conv = new CodeConverter();
        conv.replaceNew(sloader.get("test3.TransNewClassOld"),
                        sloader.get("test3.TransNewClassNew"));
        CtClass cc = sloader.get("test3.TransNewClass");
        cc.instrument(conv);
        cc.writeFile();

        CtClass cc2 = sloader.get("test3.TransNewClass$TransNewClass2");
        cc2.instrument(conv);
        cc2.writeFile();

        Object obj = make(cc.getName());
        assertEquals(170, invoke(obj, "test")); 
        Object obj2 = make(cc2.getName());
        assertEquals(50, invoke(obj2, "test")); 
    }

    public void testInsertAfter() throws Exception {
        CtClass cc = sloader.get("test3.InsertAfter");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        m1.insertAfter("k++;", true);
        CtConstructor cons = cc.getConstructor("()V");
        cons.insertAfter("k++;", true);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(6, invoke(obj, "test")); 
    }

    public void testInsertSwitch() throws Exception {
        CtClass cc = sloader.get("test3.Switch");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        String sourceCode = "{"
            + "System.out.println(\"Bla!\");"
            + "}";
        String toInsert =
            " try " +
            " {   " +
            sourceCode +
            " }   " +
            " catch(Throwable e) " +
            " {   " +
            "   e.printStackTrace(); " +
            " }   ";

        m1.insertBefore(toInsert);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(0, invoke(obj, "test"));
    }

    public void testStringBuilder() throws Exception {
        CtClass cc = sloader.get("test3.StrBuild");
        CtMethod mth = cc.getDeclaredMethod("test");
        ExprEditor ed = new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                String block = "$_=$proceed($$);";
                m.replace(block);
            }
        };
        mth.instrument(ed);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals('t', invoke(obj, "test"));
    }

    public void testInheritCons() throws Exception {
        CtClass s = sloader.get("test3.InheritCons");
        CtClass cc = sloader.makeClass("test3.InheritCons2", s);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals("test3.InheritCons2", obj.getClass().getName());
        assertEquals(3, obj.getClass().getDeclaredConstructors().length);

        cc = sloader.makeClass("InheritCons3", s);
        cc.writeFile();
        obj = make(cc.getName());
        assertEquals("InheritCons3", obj.getClass().getName());
        assertEquals(2, obj.getClass().getDeclaredConstructors().length);
    }

    public void testAddInterfaceMethod() throws Exception {
        CtClass cc = sloader.makeInterface("test3.AddIntfMth");
        CtMethod m = CtMethod.make("void foo();", cc);
        cc.addMethod(m);
        assertTrue(Modifier.isPublic(m.getModifiers()));
        CtMethod m2 = CtMethod.make("public void foo2();", cc);
        cc.addMethod(m2);
        assertTrue(Modifier.isPublic(m2.getModifiers()));
        CtMethod m3 = CtMethod.make("public void foo3() {}", cc);
        try {
            cc.addMethod(m3);
            if (ClassFile.MAJOR_VERSION < ClassFile.JAVA_8)
                fail();
        }
        catch (CannotCompileException e) {
            // System.out.println(e);
        }
    }

    // JIRA-67
    public void test67() throws Exception {
        ClassPool pool = new ClassPool(true); 
        CtClass ctc = pool.makeClass("test3.Clazz67"); 
        StringBuilder sb = new StringBuilder("public void test() {"); 
        for (int i = 0; i < 1000; i++) { 
            sb.append("for(int i=0; i<10; i++) {}"); // line 1 
            // sb.append("for(int i=0; i<10; i++) {int j=i;}"); // line 2 
        } 

        sb.append("}"); 
        ctc.addMethod(CtNewMethod.make(sb.toString(), ctc));
        ctc.debugWriteFile();
        ctc.toClass(DefineClassCapability.class).getConstructor().newInstance();
    }

    // JIRA-83
    public void testEmptyCatch() throws Exception {
        CtClass cc = sloader.get("test3.EmptyCatch");
        CtMethod mth = cc.getDeclaredMethod("test");
        mth.instrument(new ExprEditor() {
            public void edit(Handler h) throws CannotCompileException {
                try {
                    assertEquals(null, h.getType());
                    assertTrue(h.isFinally());
                } catch (NotFoundException e) {}
            }
        });
    }
}
