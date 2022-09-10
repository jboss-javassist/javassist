package javassist;

import java.io.*;
import java.net.URL;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import java.lang.reflect.Method;

import javassist.expr.*;
import test2.DefineClassCapability;

@SuppressWarnings({"rawtypes","unused"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JvstTest2 extends JvstTestRoot {
    public JvstTest2(String name) {
         super(name);
    }

    public void testInsertAt() throws Exception {
        CtClass cc = sloader.get("test2.InsertAt");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        int line = 6;
        int ln = m1.insertAt(line, false, null);
        int ln2 = m1.insertAt(line, "counter++;");
        assertEquals(ln, ln2);
        assertEquals(7, ln2);

        line = 8;
        ln = m1.insertAt(line, false, null);
        ln2 = m1.insertAt(line, "counter++;");
        assertEquals(ln, ln2);
        assertEquals(8, ln2);

        CtMethod m2 = cc.getDeclaredMethod("bar2");
        int ln3 = m2.insertAt(20, "{ int m = 13; j += m; }");
        assertEquals(20, ln3);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(7, invoke(obj, "foo"));
        assertEquals(25, invoke(obj, "bar"));
    }

    public void testInsertLocal() throws Exception {
        CtClass cc = sloader.get("test2.InsertLocal");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        m1.insertBefore("{ i = s.length(); d = 0.14; }");
        m1.insertAfter("{ field = i; }");

        CtMethod m2 = cc.getDeclaredMethod("run2");
        m2.insertAt(22, "{ s = \"12\"; k = 5; }");

        CtMethod m3 = cc.getDeclaredMethod("run3");
        m3.instrument(new ExprEditor() {
            public void edit(NewExpr n) throws CannotCompileException {
                n.replace("{ i++; $_ = $proceed($$); }");
            }
            public void edit(FieldAccess f) throws CannotCompileException {
                f.replace("{ i++; $_ = $proceed($$); }");
            }
            public void edit(MethodCall m) throws CannotCompileException {
                m.replace("{ i++; $_ = $proceed($$); }");
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(317, invoke(obj, "run"));
        assertEquals(7, invoke(obj, "run2"));
        assertEquals(3, invoke(obj, "run3"));
    }

    public void testStaticMember() throws Exception {
        CtClass cc = sloader.get("test2.StaticMember");
        CtMethod m = CtNewMethod.make(
            "public int run() {" +
            "return test2.StaticMember#k + test2.StaticMember#foo(); }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make(
            "public int run2() {" +
            "return k + foo(); }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make(
            "public int run3() {" +
            " test2.StaticMember sm = this;" +
            " return sm.k + sm.foo(); }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make(
            "public int run4() {" +
            " return this.k + this.foo(); }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make(
            "public static int run5() {" +
            " return k + foo(); }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make(
            "public int run6() {" +
            " test2.IStaticMember i = this; return i.bar(); }", cc);
        cc.addMethod(m);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(10, invoke(obj, "run"));
        assertEquals(10, invoke(obj, "run2"));
        assertEquals(10, invoke(obj, "run3"));
        assertEquals(10, invoke(obj, "run4"));
        assertEquals(10, invoke(obj, "run5"));
        assertEquals(3, invoke(obj, "run6"));
    }

    public void testStaticMember2() throws Exception {
        CtClass cc = sloader.get("test2.StaticMember2");

        cc.addMethod(CtNewMethod.make(
            "public int run() {"
          + "  return test2.StaticMember2.k + test2.StaticMember2.seven()"
          + "         + (test2.StaticMember2.f + f)"
          + "         + test2.StaticMember2.f + f; }",
            cc));

        cc.addMethod(CtNewMethod.make(
            "public int run1() {"
          + "  long j = 1L;"
          + "  return (int)(j + (test2.StaticMember2.fj + fj)"
          + "         + test2.StaticMember2.fj + fj); }",
            cc));

        cc.addMethod(CtNewMethod.make(
            "public int run2() {"
          + "  double x = 1.0;"
          + "  double d = x + test2.StaticMember2.fd + fd"
          + "             + (test2.StaticMember2.fd + fd);"
          + "  return (int)(d * 10); }",
            cc));

        cc.addMethod(CtNewMethod.make(
            "public int run3() {"
            + "  return (test2.StaticMember2.fb & fb) ? 1 : 0; }",
            cc));

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(54, invoke(obj, "run"));
        assertEquals(53, invoke(obj, "run1"));
        assertEquals(958, invoke(obj, "run2"));
        assertEquals(0, invoke(obj, "run3"));
    }

    public void testSuperCall() throws Exception {
        CtClass cc = sloader.get("test2.SuperCall");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        m1.instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    m.replace("{ $_ = $proceed($$); }");
                }
            });
        cc.writeFile();
        Object obj = make(cc.getName());
        invoke(obj, "bar");
    }

    public void testSetSuper() throws Exception {
        CtClass cc = sloader.makeClass("test2.SetSuper");
        CtClass cc2 = sloader.makeClass("test2.SetSuperParent");
        CtClass intf = sloader.makeInterface("test2.SetSuperIntf");
        CtClass remote = sloader.get("java.rmi.Remote");

        cc.setSuperclass(cc2);
        cc.setInterfaces(new CtClass[] { intf });
        intf.setSuperclass(remote);
        intf.writeFile();
        cc2.writeFile();
        cc.writeFile();

        assertEquals(cc2, cc.getSuperclass());
        assertEquals(intf, cc.getInterfaces()[0]);
        assertEquals(sloader.get("java.lang.Object"), intf.getSuperclass());
        assertEquals(remote, intf.getInterfaces()[0]);

        make(cc.getName());
    }

    public void testReplaceClassName() throws Exception {
        String oldName = "test2.ReplaceClassName2";
        String newName = "test2.ReplaceClassName3";
        CtClass cc = sloader.get("test2.ReplaceClassName");
        cc.replaceClassName(oldName, newName);
        cc.writeFile();
        CtClass cc2 = dloader.get(cc.getName());
        CtMethod m = cc2.getDeclaredMethod("foo");
        assertEquals(newName, m.getParameterTypes()[0].getName());
    }

    public void testCodeGen() throws Exception {
        CtClass cc = sloader.get("test2.CodeGen");
        CtMethod m1 = cc.getDeclaredMethod("run");
        m1.insertBefore(
            "{ double d = true ? 1 : 0.1; "
          + "  d = d > 0.5 ? 0.0 : - 1.0; "
          + "  System.out.println(d); "
          + "  String s = \"foo\"; "
          + "  s = 1 + 2 + s + \"bar\"; "
          + "  s += \"poi\" + 3 + seven() + seven(\":\" + ' '); "
          + "  s += .14; "
          + "  msg = s; "
          + "  System.out.println(s); }");

        // recursive type check is done if $proceed is used.
        CtMethod m2 = CtNewMethod.make(
            "public int test() {"
          + "  String s = $proceed(\"int\" + (3 + 0.14)) + '.'; "
          + "  System.out.println(s); return s.length(); }",
            cc, "this", "seven");
        cc.addMethod(m2);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(19, invoke(obj, "run"));
        assertEquals(9, invoke(obj, "test"));
    }

    public void testCodeGen2() throws Exception {
        CtClass cc = sloader.makeClass("test2.CodeGen2");

        CtMethod m1 = CtNewMethod.make(
            "public int test() {"
          + "  int len;"
          + "  String s = \"foo\" + \"bar\" + 3;"
          + "  System.out.println(s); len = s.length();"
          + "  len = -3 + len; len = len - (7 - 2 + -1);"
          + "  int k = 3; len += ~k - ~3;"
          + "  return len; }",
            cc);
        cc.addMethod(m1);

        CtMethod m2 = CtNewMethod.make(
            "public int test2() {"
          + "  double d = 0.2 - -0.1;"
          + "  d += (0.2 + 0.3) * 1.0;"
          + "  return (int)(d * 10); }",
            cc);
        cc.addMethod(m2);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(0, invoke(obj, "test"));
        assertEquals(8, invoke(obj, "test2"));
    }
                        
    // not used anymore.
    private void notTestGetInner() throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass c = pool.get("javassist.CtMethod.ConstParameter");
        CtClass d = pool.get("javassist.CtMethod.ConstParameter");
        CtClass e = pool.get("javassist.CtMethod$ConstParameter");
        assertSame(c, d);
        assertSame(c, e);
        try {
            c = pool.get("test2.Inner.Fake");
            fail("found not-existing class");
        }
        catch (NotFoundException ex) {}
    }

    public void testInner() throws Exception {
        ClassPool pool = ClassPool.getDefault();
        String classname = "test2.Inner";
        CtClass target = pool.get(classname);
        String src =
            "public void sampleMethod() throws Exception {"
          + "java.util.Properties props = new java.util.Properties();"
          + "test2.Inner2.Child ace = null;"
          + "test2.Inner2 agd = new test2.Inner2(props, ace);}";
        CtMethod newmethod = CtNewMethod.make(src, target);
        target.addMethod(newmethod);

        String src2 =
            "public java.lang.Character.Subset sampleMethod2() {"
          + "  java.lang.Character.Subset s "
          + "    = Character.UnicodeBlock.HIRAGANA; "
          + "  return s; }";  
        CtMethod newmethod2 = CtNewMethod.make(src2, target);
        target.addMethod(newmethod2);

        target.writeFile();
    }

    public void testURL() throws Exception {
        String url;

        ClassPool cp = new ClassPool(null);
        cp.appendSystemPath();

        url = cp.find("java.lang.Object").toString();
        System.out.println(url);
        if (JvstTest.java9)
            assertEquals("jrt:/java.base/java/lang/Object.class", url);
        else {
            assertTrue(url.startsWith("jar:file:"));
            assertTrue(url.endsWith(".jar!/java/lang/Object.class"));
        }

        assertNull(cp.find("class.not.Exist"));

        cp = new ClassPool(null);
        cp.insertClassPath(".");

        url = cp.find("test2.Inner").toString();
        System.out.println("testURL: " + url);
        assertTrue(url.startsWith("file:/"));
        assertTrue(url.endsWith("/test2/Inner.class"));

        assertNull(cp.find("test2.TestURL"));

        cp = new ClassPool(null);
        cp.insertClassPath(JAR_PATH + "javassist.jar");

        url = cp.find("javassist.CtClass").toString();
        System.out.println("testURL: " + url);
        assertTrue(url.startsWith("jar:file:"));
        assertTrue(url.endsWith("javassist.jar!/javassist/CtClass.class"));

        assertNull(cp.find("javassist.TestURL"));

        cp = new ClassPool(null);
        cp.insertClassPath(new LoaderClassPath(cloader));

        url = cp.find("javassist.CtMethod").toString();
        System.out.println("testURL: " + url);
        assertTrue(url.startsWith("file:"));
        assertTrue(url.endsWith("/javassist/CtMethod.class"));

        assertNull(cp.find("javassist.TestURL"));

        cp = new ClassPool(null);
        cp.insertClassPath(new ByteArrayClassPath("test2.ByteArray", null));

        url = cp.find("test2.ByteArray").toString();
        System.out.println("testURL: " + url);
        assertTrue(
            url.equals("file:/ByteArrayClassPath/test2/ByteArray.class"));

        assertNull(cp.find("test2.TestURL"));
    }

    public void not_testURLClassPath() throws Exception {
        String host = "www.csg.is.titech.ac.jp";
        String path = "/~chiba/tmp/";
        String url;

        ClassPool cp = new ClassPool(null);
        cp.insertClassPath(new URLClassPath(host, 80, path, "test"));

        url = cp.find("test.TestClassPath").toString();
        System.out.println(url);
        assertEquals("http://" + host + ":80" + path
                 + "test/TestClassPath.class", url);

        assertNull(cp.find("test.No"));
    }

    public void testGetURL() throws Exception {
        CtClass cc = sloader.get("java.lang.String");
        String url = cc.getURL().toString();
        System.out.println(url);
        if (JvstTest.java9) {
            assertEquals("jrt:/java.base/java/lang/String.class", url);
        }
        else {
            assertTrue(url.startsWith("jar:file:"));
            assertTrue(url.endsWith(".jar!/java/lang/String.class"));
        }

        cc = sloader.get("int");
        try {
            URL u = cc.getURL();
            fail();
        }
        catch (NotFoundException e) {
            assertEquals("int", e.getMessage());
        }
    }

    public void testInheritance() throws Exception {
        ClassPool pool = ClassPool.getDefault();
        String classname = "test2.Inherit";
        CtClass target = pool.get(classname);
        String src =
            "public void sampleMethod() {" +
            "  test2.Inherit i = new test2.Inherit();" +
            "  test2.Inherit2 i2 = i;" +
            "  test2.Inherit3 i3 = i;" +
            "  i3.foo2(); i3.foo2(); i2.foo1(); }";

        CtMethod newmethod = CtNewMethod.make(src, target);
        target.addMethod(newmethod);
        target.writeFile();
    }

    public void testIncOp() throws Exception {
        CtClass target = sloader.makeClass("test2.IncOp");
        String src =
            "public int sample() {"
          + "    int ia[] = new int[50];"
          + "    ia[0] = 1;"
          + "    int v = ++(ia[0]);"
          + "    return v; }";

        CtMethod newmethod = CtNewMethod.make(src, target);
        target.addMethod(newmethod);
        target.writeFile();
        Object obj = make(target.getName());
        assertEquals(2, invoke(obj, "sample"));
    }

    public void testSetExceptions() throws Exception {
        CtClass cc = sloader.get("test2.SetExceptions");
        CtMethod m = cc.getDeclaredMethod("f");
        CtClass ex = m.getExceptionTypes()[0];
        assertEquals("java.lang.Exception", ex.getName());
        m.setExceptionTypes(null);
        assertEquals(0, m.getExceptionTypes().length);
        m.setExceptionTypes(new CtClass[0]);
        assertEquals(0, m.getExceptionTypes().length);
        m.setExceptionTypes(new CtClass[] { ex });
        assertEquals(ex, m.getExceptionTypes()[0]);
    }

    public void testNullArg() throws Exception {
        CtClass cc = sloader.makeClass("test2.NullArgTest");
        CtMethod m1 = CtNewMethod.make(
            "public Object foo(Object[] obj, int idx) throws Throwable {" +
            "    return null; }", cc);
	cc.addMethod(m1);
	CtMethod m2 = CtNewMethod.make(
                     "public void bar() { this.foo(null, 0); }", cc);
	cc.addMethod(m2);
	CtMethod m3 = CtNewMethod.make(
                 "public void bar2() { this.foo((Object[])null, 0); }", cc);
	cc.addMethod(m3);
        cc.writeFile();
    }

    public void testAddMethod() throws Exception {
        CtClass cc = sloader.get("test2.AddMethod");
        CtMethod m = CtNewMethod.make(
                         "public int f() { return 1; }", cc);
        try {
            cc.addMethod(m);
            fail();
        }
        catch (CannotCompileException e) {}
        CtMethod m2 = CtNewMethod.make(
                         "public void f(int i, int j) { return 1; }", cc);
        cc.addMethod(m2);
        try {
            cc.addField(new CtField(CtClass.longType, "f", cc));
            fail();
        }
        catch (CannotCompileException e) {}
    }

    public void testCopyStream() throws Exception {
        int[] size = { 100, 4096, 8000, 1023, 1024, 1025, 2047,
                       4096*3, 4096*6, 4096*6-1, 4096*256*3, 4096*256*6 };
        for (int i = 0; i < size.length; i++) {
            byte[] data = new byte[size[i]];
            for (int j = 0; j < data.length; j++)
                data[j] = (byte)j;

            InputStream ins = new ByteArrayInputStream(data);
            ByteArrayOutputStream outs = new ByteArrayOutputStream();
            ClassPoolTail.copyStream(ins, outs);
            byte[] data2 = outs.toByteArray();
            if (data2.length != data.length)
                throw new Exception("bad size");

            for (int k = 0; k < data.length; k++)
                if (data[k] != data2[k])
                    throw new Exception("bad element");
        }
    }

    public void testDeclaringClass() throws Exception {
        try {
            CtClass cc = sloader.get("test2.NotExistingClass");
        }
        catch (NotFoundException e) { System.out.println(e); }
        CtClass inner = sloader.get("test2.Nested$Inner");
        CtClass outer = sloader.get("test2.Nested");
        assertEquals(outer, inner.getDeclaringClass());
        assertEquals(null, outer.getDeclaringClass());
        assertEquals(null, CtClass.intType.getDeclaringClass());

        CtClass inner3 = sloader.get("test2.Nested$Inner3");
        outer.writeFile();
        try {
            CtMethod m = CtNewMethod.make(
                     "public void f(test2.Nested n) { return n.geti(); }",
                     inner3);
            fail();
        }
        catch (RuntimeException e) {}
        outer.defrost();
        CtMethod m = CtNewMethod.make(
                 "public int f(test2.Nested n) { " +
                 "return n.geti() + test2.Nested.getj(1) + f() + g(); } ",
                 inner3);
        inner3.addMethod(m);
        inner3.writeFile();
        outer.writeFile();

        Object nobj = make(outer.getName());
        Object iobj = make(inner3.getName());
        Method mth = iobj.getClass().getMethod("f",
                                     new Class[] { nobj.getClass() });
        Object resobj = mth.invoke(iobj, new Object[] { nobj });
        int res = ((Integer) resobj).intValue();
        assertEquals(6, res);
    }

    public void testDeclaringClass2() throws Exception {
        CtClass out = sloader.get("test2.Anon");
        CtClass inner = sloader.get("test2.Anon$1");
        if (System.getProperty("java.vm.version").startsWith("1.4"))
            assertTrue(inner.getEnclosingBehavior() == null);
        else {
            assertEquals("make", inner.getEnclosingBehavior().getName());
            assertEquals(out, inner.getDeclaringClass());
            assertEquals(out,
                         inner.getEnclosingBehavior().getDeclaringClass());
        }

        assertNull(out.getEnclosingBehavior());
        assertNull(out.getEnclosingBehavior());

        CtClass inner2 = sloader.get("test2.Anon$Anon2$1");
        assertTrue(inner2.getEnclosingBehavior() instanceof CtConstructor);
        assertEquals(sloader.get("test2.Anon$Anon2"), inner2.getEnclosingBehavior().getDeclaringClass());
        CtClass inner3 = sloader.get("test2.Anon$Anon3$1");
        assertTrue(inner3.getEnclosingBehavior() instanceof CtConstructor);
        assertTrue(((CtConstructor)inner3.getEnclosingBehavior()).isClassInitializer());
        assertEquals(sloader.get("test2.Anon$Anon3"), inner3.getEnclosingBehavior().getDeclaringClass());
    }

    public void testMethodInInner() throws Exception {
        CtClass inner = sloader.get("test2.Nested2$Inner");
        CtClass outer = sloader.get("test2.Nested2");
        String src =
            "public int f(test2.Nested2 n) {" +
            "  n.i = 1; n.i++; n.i += 2; return n.i; }";

        outer.writeFile();
        try {
            CtMethod m = CtNewMethod.make(src, inner);
            fail();
        }
        catch (RuntimeException e) {}
        outer.defrost();

        CtMethod m = CtNewMethod.make(src, inner);
        inner.addMethod(m);

        src = "public int g(test2.Nested2 n) {" +
              "  n.d = 1.0; n.d++; n.d += 2.0;" +
              "  return n.d == 4.0 ? 7 : 8; }";
        m = CtNewMethod.make(src, inner);
        inner.addMethod(m);

        src = "public int h(test2.Nested2 n) {" +
              "  n.s = \"poi\";" +
              "return n.s.length() + f(n) + g(n); }";
        m = CtNewMethod.make(src, inner);
        inner.addMethod(m);

        inner.writeFile();
        outer.writeFile();

        Object nobj = make(outer.getName());
        Object iobj = make(inner.getName());
        Method mth = iobj.getClass().getMethod("h",
                                     new Class[] { nobj.getClass() });
        Object resobj = mth.invoke(iobj, new Object[] { nobj });
        int res = ((Integer) resobj).intValue();
        assertEquals(14, res);
    }

    public void testMethodInInner2() throws Exception {
        CtClass inner = sloader.get("test2.Nested3$Inner");
        CtClass outer = sloader.get("test2.Nested3");
        String src =
            "public int f() {" +
            "  int k = 0;" +
            "  test2.Nested3 n = new test2.Nested3(3);" +
            "  k += n.geti();" +
            "  n = new test2.Nested3();" +
            "  k += n.geti();" +
            "  n = new test2.Nested3(\"foo\");" +
            "  k += n.geti();" +
            "  return k; }";

        outer.stopPruning(true);
        outer.writeFile();
        try {
            CtMethod m = CtNewMethod.make(src, inner);
            fail();
        }
        catch (RuntimeException e) {}
        outer.defrost();

        CtMethod m = CtNewMethod.make(src, inner);
        inner.addMethod(m);

        inner.writeFile();
        outer.writeFile();

        Object iobj = make(inner.getName());
        assertEquals(6, invoke(iobj, "f"));
    }

    public void testMakeNestedClass() throws Exception {
        CtClass outer = sloader.get("test2.Nested4");
        try {
            CtClass inner = outer.makeNestedClass("Inner", false);
            fail();
        }
        catch (RuntimeException e) {
            print(e.getMessage());
        }

        CtClass nested = outer.makeNestedClass("Inner", true);
        outer.stopPruning(true);
        outer.writeFile();
        outer.defrost();
        String src =
            "public int f() { return test2.Nested4.value; }";

        CtMethod m = CtNewMethod.make(src, nested);
        nested.addMethod(m);
        nested.writeFile();
        outer.writeFile();

        Object iobj = make(nested.getName());
        assertEquals(6, invoke(iobj, "f"));
    }

    public void testPrivateMethod() throws Exception {
        CtClass cc = sloader.get("test2.PrivateMethod");
        try {
            CtMethod m = CtNewMethod.make(
                "public int f(test2.PrivateMethod2 p) { return p.f(); }",
                cc);
            fail();
        }
        catch (CannotCompileException e) {}
    }

    public void testArrayLength() throws Exception {
        CtClass cc = sloader.makeClass("test2.ArrayLength");
        CtMethod m2 = CtNewMethod.make(
		"public int f() { String[] s = new String[3]; " +
                "return s.length; }", cc);
        cc.addMethod(m2);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "f"));
    }

    public void testMakeStaticMethod() throws Exception {
        CtClass cc = sloader.makeClass("test2.MakeStaticMethod");
        CtMethod m = CtNewMethod.make(Modifier.PUBLIC | Modifier.STATIC,
                                      CtClass.intType, "create",
                                      new CtClass[] { CtClass.intType }, null,
                                      "{ return $1; }", cc);
        cc.addMethod(m);
        cc.addMethod(CtNewMethod.make(
		"public int test() { return create(13); }", cc));
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(13, invoke(obj, "test"));
    }

    public void testNewExprTry() throws Exception {
        ExprEditor ed = new ExprEditor() {
            public void edit(NewExpr expr) throws CannotCompileException {
                StringBuffer code = new StringBuffer(300); 
                code.append("{ try ");
                code.append("{ $_ = $proceed($$); }");
                code.append("catch (OutOfMemoryError e) {}}"); 
                expr.replace(code.toString());
            }
        };

        CtClass cc = sloader.get("test2.NewExprTry");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        m1.instrument(ed);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(16, invoke(obj, "run"));
    }

    public void testToClass() throws Exception {
        ClassPool cp = ClassPool.getDefault();
        CtClass cc = cp.makeClass("test2.ToClassTest");
        Class c = cc.toClass(DefineClassCapability.class);
	assertEquals(getClass().getClassLoader(), c.getClassLoader());
    }

    public void testAddCatchForConstructor() throws Exception {
        CtClass cc = sloader.get("test2.AddCatchForConstructor");
        CtConstructor m1 = cc.getDeclaredConstructors()[0];
        m1.addCatch("return;", sloader.get("java.lang.Exception"));
        cc.writeFile();
        Object obj = make(cc.getName());
    }

    public void testAddLocalVar() throws Exception {
        CtClass cc = sloader.get("test2.AddLocalVar");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        m1.addLocalVariable("i", CtClass.intType);
        m1.insertBefore("i = 3;");
        m1.insertAfter("$_ = i + 1;");
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(4, invoke(obj, "foo"));
    }

    public void testNewArray() throws Exception {
        ExprEditor ed = new ExprEditor() {
            int dim[] = { 1, 2, 2, 1, 2, 2, 3 };
            int cdim[] = { 1, 1, 2, 1, 1, 2, 2 };
            int counter = 0;
            public void edit(NewArray expr) throws CannotCompileException {
                try {
                    CtClass str = sloader.get("java.lang.String");
                    if (counter < 3)
                        assertEquals(str, expr.getComponentType());
                    else
                        assertEquals(CtClass.intType, expr.getComponentType());

                    assertEquals(dim[counter], expr.getDimension());
                    assertEquals(cdim[counter], expr.getCreatedDimensions());
                    expr.replace("{ i += $1; $_ = $proceed($$); }");
                    ++counter;
                }
                catch (NotFoundException e) {
                    throw new CannotCompileException(e);
                }
            }
        };

        CtClass cc = sloader.get("test2.NewArray");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        m1.instrument(ed);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(48, invoke(obj, "run"));
    }

    public void testToString() throws Exception {
        System.out.println(sloader.get("int"));
        System.out.println(sloader.get("int[]"));
        System.out.println(sloader.get("java.lang.Object"));
        System.out.println(sloader.get("java.lang.String"));
        System.out.println(sloader.get("javassist.CtNewClass"));
    }

    public void testDotClass() throws Exception {
        testDotClass("test2.DotClass", false);
        testDotClass("test2.DotClass_", true);
    }

    private void testDotClass(String cname, boolean java5) throws Exception {
        CtClass cc = sloader.makeClass(cname);
        if (java5)
            cc.getClassFile2().setVersionToJava5();

        CtMethod m = CtNewMethod.make(
        "public String getclass() {" +
        "  return int.class.getName() + int[].class.getName()" +
        "          + String.class.getName() + String[].class.getName()" +
        "          + java.lang.Object.class.getName()" +
        "          + java.util.Vector.class.getName(); }", cc);
        cc.addMethod(m);
        CtMethod m2 = CtNewMethod.make(
        "public int test() {" +
        "  String s = getclass(); System.out.println(s);" +
        "  return s.length(); }", cc);
        cc.addMethod(m2);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(72, invoke(obj, "test"));
    }

    public void testDotClass2() throws Exception {
        testDotClass2("test2.DotClass2", false);
        testDotClass2("test2.DotClass2_", true);
    }

    private void testDotClass2(String cname, boolean java5) throws Exception {
        CtClass cc = sloader.makeClass(cname);
        CtClass cc3 = sloader.makeClass("test2.DotClass3");
        if (java5)
            cc.getClassFile2().setVersionToJava5();

        CtMethod m = CtNewMethod.make(
        "public int test() {" +
        "  return test2.DotClass3.class.getName().length(); }", cc);
        cc.addMethod(m);
        cc.writeFile();
        // don't execute cc3.writeFile();
        Object obj = make(cc.getName());
        try {
            assertEquals(15, invoke(obj, "test"));
        }
        catch (java.lang.reflect.InvocationTargetException e) {
            Throwable t = e.getCause();
            assertTrue(t instanceof java.lang.NoClassDefFoundError);
        }
    }

    public void testDotClass4() throws Exception {
        testDotClass4("test2.DotClass4", false);
        testDotClass4("test2.DotClass4_", true);
    }

    private void testDotClass4(String cname, boolean java5) throws Exception {
        CtClass cc = sloader.makeClass(cname);
        if (java5)
            cc.getClassFile2().setVersionToJava5();

        CtMethod m = CtNewMethod.make(
            "public int test() {" +
            "  String s = Object.class.getName()" +
            "      + Object[].class.getName();" +
            "  return s.length(); }", cc);
        cc.addMethod(m);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(35, invoke(obj, "test"));
    }

    public void testSuperInterface() throws Exception {
        CtClass cc = sloader.makeClass("test2.SuperInterface3");
        CtClass cc2 = sloader.get("test2.SuperInterface2");
        cc.addInterface(cc2);
        cc.addField(new CtField(cc2, "inner", cc));
        CtMethod m = CtNewMethod.make(
            "public int getAge() { return inner.getAge(); }", cc);
        cc.addMethod(m);
        cc.writeFile();
    }

    public void testPrune() throws Exception {
        CtClass cc = sloader.get("test2.Prune");
        cc.stopPruning(false);
        System.out.println(cc);
        cc.addField(new CtField(CtClass.intType, "f", cc));
        cc.toBytecode();
        try {
            cc.defrost();
            fail("can call defrost()");
        }
        catch (RuntimeException e) {
            assertTrue(e.getMessage().indexOf("prune") >= 0);
        }

        System.out.println(cc);
    }

    public void testNewExprInTry() throws Exception {
        ExprEditor ed = new ExprEditor() {
            public void edit(NewExpr expr) throws CannotCompileException {
                expr.replace("$_ = new test2.HashMapWrapper($1, 1);");
            }
        };

        CtClass cc = sloader.get("test2.NewExprInTry");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        m1.instrument(ed);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "run"));
    }

    public void testConstField() throws Exception {
        CtClass cc = sloader.get("test2.ConstField");
        CtField f;
        f = cc.getField("b");
        assertEquals(true, ((Boolean)f.getConstantValue()).booleanValue());
        f = cc.getField("i");
        assertEquals(3, ((Integer)f.getConstantValue()).intValue());
        f = cc.getField("j");
        assertEquals(7L, ((Long)f.getConstantValue()).longValue());
        f = cc.getField("f");
        assertEquals(8.0F, ((Float)f.getConstantValue()).floatValue(), 0.0);
        f = cc.getField("d");
        assertEquals(9.0, ((Double)f.getConstantValue()).doubleValue(), 0.0);
        f = cc.getField("s");
        assertEquals("const", f.getConstantValue());
        f = cc.getField("obj");
        assertEquals(null, f.getConstantValue());
        f = cc.getField("integer");
        assertEquals(null, f.getConstantValue());
        f = cc.getField("k");
        assertEquals(null, f.getConstantValue());

        cc.getClassFile().prune();

        f = cc.getField("i");
        assertEquals(3, ((Integer)f.getConstantValue()).intValue());
        f = cc.getField("k");
        assertEquals(null, f.getConstantValue());
    }

    public void testWhere() throws Exception {
        CtClass cc = sloader.get("test2.Where");
        CtConstructor cons = cc.getClassInitializer();
        cons.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                System.out.println(m.where().getName());
            }
        });

    }

    public void testNewOp() throws Exception {
        CtClass cc = sloader.get("test2.NewOp");

        CtMethod add = CtNewMethod.make( 
"public test2.NewOp2 " +
"    addMonitoringRemoteEventListener(" +
"        test2.NewOp2 listener)" +
"        throws java.rmi.RemoteException {" +
"    $0.listenerList.addElement(listener);" +
"    return new test2.NewOp2(0L, this, null, 0L);" +
"}",  cc);

        add.setModifiers(Modifier.PUBLIC);
        cc.addMethod(add);
    /*
        CtMethod type= CtNewMethod.make( 
           "public test2.Where getNewType() { return new test2.Where(); }",
            cc);
        cc.addMethod(type);
    */
        cc.writeFile();
    }

    public void testSwitch() throws Exception {
        CtClass cc = sloader.makeClass("test2.Switch");

        cc.addMethod(CtNewMethod.make(
            "public int test1() {" +
            "  int i = 1;" +
            "  int j;" +
            "  switch (i) {" +
            "  case 0: j = i; break;" +
            "  case 1: j = -i; break;" +
            "  default: j = 0; break;" +
            "  }" +
            "  return j; }", cc));

        cc.addMethod(CtNewMethod.make(
            "public int test2() {" +
            "  int i = 2;" +
            "  int j = 7;" +
            "  switch (i) {" +
            "  case 0: j = i; break;" +
            "  case 1: j = -i; break;" +
            "  }" +
            "  return j; }", cc));

        cc.addMethod(CtNewMethod.make(
            "public int test3() {" +
            "  int i = Byte.MAX_VALUE;" +
            "  int j;" +
            "  switch (i) {" +
            "  case Byte.MAX_VALUE: j = i; break;" +
            "  case Byte.MIN_VALUE: j = -i; break;" +
            "  default: j = 0; break;" +
            "  }" +
            "  return j; }", cc));

        try {
            cc.addMethod(CtNewMethod.make(
            "public int test4() {" +
            "  int i = Byte.MAX_VALUE;" +
            "  int j;" +
            "  switch (i) {" +
            "  case Byte.MAX_VALUE: j = i; return j;" +
            "  case Byte.MIN_VALUE: j = -i; return j;" +
            "  default: j = 0;" +
            "  }" +
            "}", cc));
            fail("does not report an error (no return)");
        }
        catch (CannotCompileException e) { System.out.println(e); }

        try {
            cc.addMethod(CtNewMethod.make(
            "public int test5() {" +
            "  int i = Byte.MAX_VALUE;" +
            "  int j;" +
            "  switch (i) {" +
            "  case Byte.MAX_VALUE: j = i; return j;" +
            "  case Byte.MIN_VALUE: j = -i; return j;" +
            "  }" +
            "}", cc));
            fail("does not report an error (not default)");
        }
        catch (CannotCompileException e) { System.out.println(e); }

        try {
            cc.addMethod(CtNewMethod.make(
            "public int test6() {" +
            "  int i = Byte.MAX_VALUE;" +
            "  int j;" +
            "  switch (i) {" +
            "  case Byte.MAX_VALUE: j = i; break;" +
            "  default: j = -i; return j;" +
            "  }" +
            "  }", cc));
            fail("does not report an error (break)");
        }
        catch (CannotCompileException e) { System.out.println(e); }

        cc.addField(CtField.make("public static int k;", cc));

        cc.addMethod(CtNewMethod.make(
        "public void foo() {" +
        "  int i = 0;" +
        "  k = 3;" +
        "  switch (i) {" +
        "  case Byte.MAX_VALUE: k = 1;" +
        "  case Byte.MIN_VALUE: k = 2;" +
        "  }" +
        "}", cc));

        cc.addMethod(CtNewMethod.make(
        "public int test7() {" +
        "  int i = Byte.MAX_VALUE;" +
        "  int j = 3; foo();" +
        "  System.out.println(k);" +
        "  switch (i) {" +
        "  case Byte.MAX_VALUE: return k;" +
        "  case Byte.MIN_VALUE: return j;" +
        "  default: return 0;" +
        "  }" +
        "}", cc));

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(-1, invoke(obj, "test1"));
        assertEquals(7, invoke(obj, "test2"));
        assertEquals(Byte.MAX_VALUE, invoke(obj, "test3"));
        assertEquals(3, invoke(obj, "test7"));
    }

    public void testGet() throws Exception {
        CtClass cc = sloader.get("char[]");
        CtClass cc2 = cc.getComponentType();
        System.out.println(cc2);
    }

    public void testSynchronized() throws Exception {
        CtClass cc = sloader.makeClass("test2.Synch");

        cc.addMethod(CtNewMethod.make(
            "public synchronized int test1() {" +
            "  int i = 0;" +
            "  synchronized (this) {" +
            "    i = 3;" +
            "  }" +
            "  return i; }", cc));

        cc.addMethod(CtNewMethod.make(
            "public synchronized int test2() {" +
            "  int i = 0;" +
            "  synchronized (this) {" +
            "    i = 3;" +
            "    return i;" +
            "  }" +
            "}", cc));

        cc.addMethod(CtNewMethod.make(
            "public synchronized int test3() {" +
            "  int i = 0;" +
            "  synchronized (this) {" +
            "    if (this instanceof String)" +
            "      return i;" +
            "    else" +
            "      i = 3;" +
            "  }" +
            "  return i;" +
            "}", cc));

        cc.addMethod(CtNewMethod.make(
            "public synchronized int test4() {" +
            "  int i = 0;" +
            "  synchronized (this) {" +
            "  }" +
            "  return i; }", cc));

        try {
            cc.addMethod(CtNewMethod.make(
                "public synchronized int test5() {" +
                "  while (true)" +
                "    synchronized (this) {" +
                "      break;" +
                "    }" +
                "  return i; }", cc));
            fail("does not report an error");
        }
        catch (CannotCompileException e) { System.out.println(e); }

        cc.addMethod(CtNewMethod.make(
            "public synchronized int test6() {" +
            "  int i = 0;" +
            "  while (true) {" +
            "    synchronized (this) {" +
            "      i = 3;" +
            "    }" +
            "    break; }" +
            "  return i; }", cc));

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "test1"));
        assertEquals(3, invoke(obj, "test2"));
        assertEquals(3, invoke(obj, "test3"));
        assertEquals(0, invoke(obj, "test4"));
    }

    public void testTryFinally() throws Exception {
        CtClass cc = sloader.get("test2.Finally");

        cc.addMethod(CtNewMethod.make(
            "public int test1() {" +
            "  a = 0;" +
            "  try {" +
            "    update();" +
            "  } catch (NullPointerException e) {" +
            "    a = 1;" +
            "  } finally {" +
            "    a = 2;" +
            "  }" +
            "  return a; }", cc));

        cc.addMethod(CtNewMethod.make(
            "public int test2() {" +
            "  a = 0;" +
            "  try {" +
            "    update(); return a;" +
            "  } catch (NullPointerException e) {" +
            "    a = 1; throw e;" +
            "  } finally {" +
            "    a = 2; return a;" +
            "  }" +
            "}", cc));

        cc.addMethod(CtNewMethod.make(
            "public int test3() {" +
            "  a = 0;" +
            "  try {" +
            "    update(); return a;" +
            "  } catch (NullPointerException e) {" +
            "    a = 1;" +
            "  } finally {" +
            "    a = 2;" +
            "  }" +
            "  return a;" +
            "}", cc));

        cc.addMethod(CtNewMethod.make(
            "public int test4() {" +
            "  a = 0;" +
            "  try {" +
            "    update(); return a;" +
            "  } catch (NullPointerException e) {" +
            "    a = 1; return a;" +
            "  } finally {" +
            "    a = 2;" +
            "  }" +
            "}", cc));

        cc.addMethod(CtNewMethod.make(
                "public double test5() {" +
                "  b = 1.0;" +
                "  try {" +
                "    return b;" +
                // "  } catch (NullPointerException e) {" +
                // "    b = 2.0; return b;" +
                "  } finally {" +
                "    b += 3.0;" +
                "  }" +
                "}", cc));

        cc.addMethod(CtNewMethod.make(
                "public int test5a() {" +
                "  return (int)test5();" +
                "}", cc));

        cc.addMethod(CtNewMethod.make(
                "public int test6() {" +
                "  a = 0;" +
                "  try {" +
                "    if (a > 0)" +
                "        return a;" +
                "    update(); a = 1;" +
                "  }" +
                "  catch (RuntimeException e) {" +
                "    if (a > 0) a = 2; else a = 3;" +
                "  }" +
                "  catch (Throwable e) {" +
                "    a = 4;" +
                "  } finally {" +
                "    try { if (a < 0) update(); return a; }" +
                "    finally { if (a > 0) return a; a = 5; }" +
                "    " +
                "  }" +
                "}", cc));

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(2, invoke(obj, "test1"));
        assertEquals(2, invoke(obj, "test2"));
        assertEquals(2, invoke(obj, "test3"));
        assertEquals(1, invoke(obj, "test4"));
        assertEquals(1, invoke(obj, "test5a"));
        assertEquals(3, invoke(obj, "test6"));
    }

    public void testConstructorName() throws Exception {
        CtClass cc = sloader.get("test2.Construct");
        CtConstructor[] cons = cc.getDeclaredConstructors();
        assertEquals("Construct", cons[0].getName());
        assertEquals("Construct", cons[1].getName());
        assertEquals("<clinit>", cc.getClassInitializer().getName());
    }

    public void testRemoveCall() throws Exception {
        CtClass cc = sloader.get("test2.RemoveCall");
        CtMethod m1 = cc.getDeclaredMethod("bar");
        m1.instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    m.replace("{ $_ = ($r)null; }");
                }
            });
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(0, invoke(obj, "bar"));
    }

    public void testRemove() throws Exception {
        CtClass cc = sloader.get("test2.Remove");
        testRemove2(cc, "f1");
        testRemove2(cc, "f6");
        testRemove2(cc, "f3");
        CtField p = cc.getField("p");
        try {
            cc.removeField(p);
            fail("non-existing field has been removed");
        }
        catch (NotFoundException e) {}

        testRemove3(cc, "bar");
        testRemove3(cc, "bar2");
        testRemove4(cc, "(I)V");
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(7, invoke(obj, "foo"));
    }

    private void testRemove2(CtClass cc, String fieldName) throws Exception {
        CtField f = cc.getField(fieldName);
        cc.removeField(f);
        try {
            CtField f2 = cc.getField(fieldName);
            fail("the removed field still exists");
        }
        catch (NotFoundException e) {}
    }

    private void testRemove3(CtClass cc, String methodName) throws Exception {
        CtMethod m = cc.getDeclaredMethod(methodName);
        cc.removeMethod(m);
        try {
            CtMethod m2 = cc.getDeclaredMethod(methodName);
            fail("the removed method still exists");
        }
        catch (NotFoundException e) {}
    }

    private void testRemove4(CtClass cc, String desc) throws Exception {
        CtConstructor c = cc.getConstructor(desc);
        cc.removeConstructor(c);
        try {
            CtConstructor c2 = cc.getConstructor(desc);
            fail("the removed method still exists");
        }
        catch (NotFoundException e) {}
    }

    public void testGetAndRename() throws Exception {
        try {
            CtClass cc = sloader.getAndRename("NotExisting", "Existing");
        }
        catch (NotFoundException e) {
            System.out.println(e);
        }
    }

    public void testConstBody() throws Exception {
        CtClass cc = sloader.get("test2.ConstBody");
        CtConstructor cons = new CtConstructor(new CtClass[] {
                sloader.get("java.lang.String"),
                sloader.get("java.lang.Integer") }, cc);
        cons.setBody("super((String)$1, (Integer)$2);");
        cc.addConstructor(cons);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "bar"));
    }

    private String methodCallData = null;

    public void testMethodCall() throws Exception {
        CtClass cc = sloader.get("test2.MethodCall");

        CtMethod m1 = cc.getDeclaredMethod("bar");
        m1.instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    if ("clone".equals(m.getMethodName()))
                        methodCallData = m.getClassName();
                }
            });

        cc.writeFile();
        assertEquals("java.lang.String[]", methodCallData);

        assertEquals("java.lang.String[]",
                     sloader.get("[Ljava/lang/String;").getName());
        assertEquals("int[][]",
                     sloader.get("[[I").getName());
    }

    public void testKmatcha() throws Exception {
        CtClass cc = sloader.makeClass("test2.Kmatcha");
        cc.addMethod(CtNewMethod.make(
"public void display(String [] params){" +
"  if(params == null){" +
"    System.out.println(\"Nothing to display\");" +
"  }else{" +
"    int k = params.length - 1;" +
"    if(k >= 0)" +
"      do " +
"        System.out.println(params[k]);" +
"      while(--k >= 0);" +
"  }}", cc));
    }

    public void testStrict() throws Exception {
        CtClass cc = sloader.makeClass("test2.StrictTest");
        cc.addMethod(CtNewMethod.make(
"public strictfp int foo(){ " +
"  int strict = 1; return strict + 1; }", cc));
    }

    public void testArrayLen() throws Exception {
        CtClass cc = sloader.get("test2.ArrayLenTest");
        cc.addMethod(CtNewMethod.make(
"public int foo(){ return this.length; }", cc));
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "foo"));
    }

    public void testUnicodeIdentifier() throws Exception {
        CtClass cc = sloader.makeClass("test2.UnicodeIdentifier");
        String src = "public int foo(){ int \u5206 = 0; return \u5206; }";
        cc.addMethod(CtNewMethod.make(src, cc));
    }

    public void testBrennan() throws Exception {
        CtClass cc = sloader.get("test2.Brennan");
        cc.addMethod(CtNewMethod.make(
"public int foo(){" +
"  java.text.SimpleDateFormat df;" +
"  if((df = (java.text.SimpleDateFormat)format) == null)" +
"    df = new java.text.SimpleDateFormat(\"yyyyMMdd\");" +
"  return 1;}", cc));
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "foo"));
    }

    public void testArrayAndNull() throws Exception {
        CtClass cc = sloader.get("test2.ArrayAndNull");
        CtMethod m = cc.getDeclaredMethod("test");
        m.insertAfter("if ($_ == null) $_ = new int[0];");
    }

    public void testStaticArrays() throws Exception {
        CtClass cc = sloader.makeClass("StaticArrays");
        CtField f = new CtField(sloader.get("test2.StaticArraysMem[]"),
                                "myStaticField", cc);

        f.setModifiers(Modifier.STATIC);
        cc.addField(f);
        CtConstructor init = cc.makeClassInitializer();
        String body = "{\n";
        body += ("myStaticField = new test2.StaticArraysMem[2];\n");
        body += ("\n}");
        init.setBody(body);
    }

    public void testObjectSuper() throws Exception {
        CtClass cc = sloader.get("java.lang.Object");
        try {
            cc.addMethod(CtNewMethod.make(
		"public int foo(){ return super.hashCode(); }", cc));
            fail("could access the super of java.lang.Object");
        }
        catch (CannotCompileException e) {}
    }

    public void testStaticFinal() throws Exception {
        CtClass cc = sloader.makeClass("test2.StaticFinal");
        CtField f = new CtField(CtClass.intType, "sff1", cc);
        f.setModifiers(Modifier.STATIC | Modifier.FINAL);
        cc.addField(f, "5");
        assertEquals(Integer.valueOf(5), f.getConstantValue());

        f = new CtField(CtClass.longType, "sff2", cc);
        f.setModifiers(Modifier.STATIC | Modifier.FINAL);
        cc.addField(f, "6");
        assertEquals(Long.valueOf(6), f.getConstantValue());

        f = new CtField(CtClass.floatType, "sff3", cc);
        f.setModifiers(Modifier.STATIC | Modifier.FINAL);
        cc.addField(f, "7");
        assertEquals(Float.valueOf(7.0F), f.getConstantValue());

        f = new CtField(CtClass.floatType, "sff4", cc);
        f.setModifiers(Modifier.STATIC | Modifier.FINAL);
        cc.addField(f, "8.0");
        assertEquals(Float.valueOf(8.0F), f.getConstantValue());

        f = new CtField(CtClass.doubleType, "sff5", cc);
        f.setModifiers(Modifier.STATIC | Modifier.FINAL);
        cc.addField(f, "9");
        assertEquals(Double.valueOf(9.0), f.getConstantValue());

        f = new CtField(CtClass.doubleType, "sff6", cc);
        f.setModifiers(Modifier.STATIC | Modifier.FINAL);
        cc.addField(f, "10.0");
        assertEquals(Double.valueOf(10.0), f.getConstantValue());

        f = new CtField(sloader.get("java.lang.String"), "sff7", cc);
        f.setModifiers(Modifier.STATIC | Modifier.FINAL);
        cc.addField(f, "\"test\"");
        assertEquals("test", f.getConstantValue());

        f = new CtField(sloader.get("java.lang.String"), "sff8", cc);
        f.setModifiers(Modifier.STATIC);
        cc.addField(f, "\"static\"");
        assertEquals(null, f.getConstantValue());

        cc.addMethod(CtNewMethod.make(
		"public int foo(){ return sff1 + sff7.length(); }", cc));
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(9, invoke(obj, "foo"));
    }

    public void testLocalVar() throws Exception {
        CtClass cc = sloader.get("test2.LocalVar");
        CtMethod m = cc.getDeclaredMethod("toString");
        m.addLocalVariable("var", CtClass.booleanType);
        m.insertBefore("{var = true; }");
        m.insertAfter("{if (var) hashCode(); }", false);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "foo"));
    }

    public void testImportPackage() throws Exception {
        CtClass cc2 = sloader.makeClass("test2.Imported");
        cc2.writeFile();
        CtClass cc = sloader.makeClass("test2.Importer");
        sloader.importPackage("test2");
        cc.addMethod(CtNewMethod.make(
		"public int foo(){ " + 
                "  Imported obj = new Imported();" +
                "  return obj.getClass().getName().length(); }", cc));
        sloader.clearImportedPackages();
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(14, invoke(obj, "foo"));
    }

    public void testArrayInit() throws Exception {
        CtClass cc = sloader.makeClass("test2.ArrayInit");
        cc.addMethod(CtNewMethod.make(
		"public int foo(){ " + 
                "  int[] i = new int[] { 1, 2 };" +
                "  double[] d = new double[] { 3.0, 4.0 };" +
                "  String[] s = new String[] { \"foo\", \"12345\" };" +
                "  return i[0] + (int)d[0] + s[1].length(); }", cc));
        cc.addMethod(CtNewMethod.make(
		"public int bar(){ " + 
                "  int[] i = { 1, 2.0 };" +
                "  double[] d = { 3.0, 4 };" +
                "  String[] s = { \"foo\", \"12345\" };" +
                "  return i[0] + (int)d[0] + s[1].length(); }", cc));
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(9, invoke(obj, "foo"));
        assertEquals(9, invoke(obj, "bar"));
    }
}
