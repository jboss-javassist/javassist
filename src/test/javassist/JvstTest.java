package javassist;

import junit.framework.*;
import test1.DefineClassCapability;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import javassist.bytecode.*;
import javassist.expr.*;
import javassist.runtime.*;

@SuppressWarnings({"rawtypes","unused", "resource"})
public class JvstTest extends JvstTestRoot {
    public static boolean java9;

    static {
        //javassist.bytecode.MethodInfo.doPreverify = true;
        java9 = javassist.bytecode.ClassFile.MAJOR_VERSION
                    >= javassist.bytecode.ClassFile.JAVA_9;
    }
    public JvstTest(String name) {
         super(name);
    }

    public void testConfig() {
        // is the value of PATH correct?
        assertTrue("not found " + PATH, new java.io.File(PATH).exists());
    }

    public void testLoader() throws Exception {
        Loader loader = new Loader(sloader);
        loader.delegateLoadingOf("test1.");
        assertEquals(loader.loadClass("test1.Cflow").getClassLoader(),
                     loader.getParent());
        assertEquals(loader.loadClass("javassist.Loader").getClassLoader(),
                     loader.getParent());
        assertEquals(loader.loadClass("javassist.CtClass").getClassLoader(),
                     loader);
    }

    public void testDefreeze() throws Exception {
        CtClass cc = sloader.get("test1.Freeze");
        cc.stopPruning(true);
        cc.addInterface(sloader.get("java.io.Serializable"));
        assertTrue(!cc.isFrozen());
        cc.writeFile();
        assertTrue(cc.isFrozen());
        cc.defrost();
        assertTrue(!cc.isFrozen());
    }

    public void testClassPath() throws Exception {
        ClassPool pool = new ClassPool(null);
        ClassPath cp1 = pool.appendClassPath("d1");
        ClassPath cp2 = pool.appendClassPath("d2");
        ClassPath cp3 = pool.appendClassPath("d3");
        ClassPath cp4 = pool.appendClassPath("d4");
        print(pool.toString());
        pool.removeClassPath(cp3);
        print(pool.toString());
        pool.removeClassPath(cp4);
        print(pool.toString());
        pool.removeClassPath(cp2);
        print(pool.toString());
        pool.removeClassPath(cp1);
        assertTrue("[class path: ]".equals(pool.toString()));
    }

    public void testReleaseJarClassPathFileHandle() throws Exception {
        String jarFileName = "./empty.jar";
        ClassLoader classLoader = getClass().getClassLoader();
        File jarFile = new File(classLoader.getResource(jarFileName).getFile());
        assertTrue(jarFile.exists());

        // Prepare class pool and force it to open the Jar file
        ClassPool pool = ClassPool.getDefault();
        ClassPath cp = pool.appendClassPath(jarFile.getAbsolutePath());
        assertNull(cp.openClassfile("nothere.Dummy"));

        // Assert that it is possible to delete the jar file.
        // On Windows deleting an open file will fail, while on on Mac/Linux this is always possible.
        // This check will thus only fail on Windows if the file is still open.
        assertTrue(jarFile.delete());
    }

    public void testJarClassPath() throws Exception {
        String jarFileName = "./simple.jar";
        ClassLoader classLoader = getClass().getClassLoader();
        File jarFile = new File(classLoader.getResource(jarFileName).getFile());
        assertTrue(jarFile.exists());

        ClassPool pool = ClassPool.getDefault();
        ClassPath cp = pool.appendClassPath(jarFile.getAbsolutePath());
        InputStream is = cp.openClassfile("com.test.Test");
        assertNotNull(is);
        is.close();
    }

    public void testSubtype() throws Exception {
        CtClass cc = sloader.get("test1.Subtype");
        assertTrue(cc.subtypeOf(cc));
        assertTrue(cc.subtypeOf(sloader.get("test1.SubtypeA")));
        assertTrue(cc.subtypeOf(sloader.get("test1.SubtypeB")));
        assertTrue(cc.subtypeOf(sloader.get("test1.SubtypeC")));
        assertTrue(cc.subtypeOf(sloader.get("java.lang.Object")));
        assertTrue(!cc.subtypeOf(sloader.get("java.lang.String")));
    }

    public void testClassPoolGet() throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.makeClass("test1.Point");
        CtClass cc1 = pool.get("test1.Point");   // cc1 is identical to cc.
        cc.setName("test1.Pair");
        CtClass cc2 = pool.get("test1.Pair");    // cc2 is identical to cc.
        CtClass cc3 = pool.get("test1.Point");   // cc3 is not identical to cc.

        assertTrue(cc == cc1);
        assertTrue(cc == cc2);
        assertTrue(cc != cc3);

        assertEquals("test1.Pair", cc.getName());
        assertEquals("test1.Point", cc3.getName());
    }

    public static long testFieldInitHash;

    /* test CodeIterator.insertExGap().
     * The result of this test is checked again by JvstTest3#testFieldInitAgain().
     */
    public void testFieldInit() throws Exception {
        CtClass cc = sloader.get("test1.FieldInit");
        CtField f1 = new CtField(CtClass.intType, "f1", cc);
        cc.addField(f1, CtField.Initializer.byCall(cc, "get"));
        CtField f2 = CtField.make("public int f2 = 3;", cc);
        cc.addField(f2);
        CtField f3 = CtField.make("public int f3;", cc);
        cc.addField(f3);
        CtField f4 = CtField.make("public int f4 = this.f2 + 3;", cc);
        cc.addField(f4);
        CtField fi = CtField.make("public test1.FieldInit.FI fi = new test1.FieldInit.FI(this);", cc);
        cc.addField(fi);
        testFieldInitHash = f1.hashCode();
        cc.writeFile();
        Object obj = make(cc.getName());
        int value = obj.getClass().getField("counter").getInt(obj);
        assertEquals(1, value);
        int value2 = obj.getClass().getField("f2").getInt(obj);
        assertEquals(3, value2);
        int value3 = obj.getClass().getField("f3").getInt(obj);
        assertEquals(0, value3);
        int value4 = obj.getClass().getField("f4").getInt(obj);
        assertEquals(6, value4);
        Object obfi = obj.getClass().getField("fi").get(obj);
        assertTrue(obfi.getClass().getField("fi").get(obfi) == obj);
    }

    /* test CodeIterator.insertExGap().
     */
    public void testFieldInit2() throws Exception {
        CtClass cc = sloader.get("test1.FieldInit2");
        CtField f = new CtField(CtClass.intType, "f1", cc);
        cc.addField(f, CtField.Initializer.byCall(cc, "get"));
        cc.writeFile();
        try {
            Object obj = make(cc.getName());
            fail();
        }
        catch (Exception e) {
            print("testFieldInit2: catch");
        }
    }

    public static CtMethod testCalleeBeforeMethod;
    public static long testCalleeBeforeMethod2;

    /* The test result is checked again by JvstTest3#testCalleeBeforeAgain().
     */
    public void testCalleeBefore() throws Exception {
        CtClass cc = sloader.get("test1.CalleeBefore");

        CtMethod m1 = cc.getDeclaredMethod("m1");
        m1.insertBefore("{ int k = 1; p = k; }");
        CtMethod m2 = cc.getDeclaredMethod("m2");
        testCalleeBeforeMethod = m1;
        testCalleeBeforeMethod2 = m2.getMethodInfo2().hashCode();
        m2.insertBefore("{ int k = 3; q = k; }");
        CtConstructor[] cons = cc.getDeclaredConstructors();

        for (int i = 0; i < cons.length; ++i) {
            MethodInfo minfo = cons[i].getMethodInfo();
            CodeAttribute ca = minfo.getCodeAttribute();
            CodeIterator iterator = ca.iterator();
            if (cons[i].getParameterTypes().length == 0) {
                assertTrue(iterator.skipThisConstructor() >= 0);
                assertTrue(iterator.skipSuperConstructor() < 0);
                assertTrue(iterator.skipConstructor() >= 0);
            }
            else {
                assertTrue(iterator.skipThisConstructor() < 0);
                assertTrue(iterator.skipSuperConstructor() >= 0);
                assertTrue(iterator.skipConstructor() >= 0);
            }

            cons[i].insertBeforeBody("{ int k = 1; counter += k; }");
        }

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(0, invoke(obj, "getr"));
        assertEquals(17, invoke(obj, "test"));
    }

    public void testCalleeAfter() throws Exception {
        CtClass cc = sloader.get("test1.CalleeAfter");

        CtMethod m1 = cc.getDeclaredMethod("m1");
        m1.insertAfter("{ int k = 1; $_ = $_ + k; }", false);

        CtMethod m2 = cc.getDeclaredMethod("m2");
        m2.insertAfter("{ char k = 1; $_ = $_ + k; }", false);

        CtConstructor[] cons = cc.getDeclaredConstructors();
        cons[0].insertAfter("{ ++p; $_ = ($r)null; }", false);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(15, invoke(obj, "test"));
    }

    public void testCalleeAfter2() throws Exception {
        CtClass cc = sloader.get("test1.CalleeAfter2");

        CtMethod m1 = cc.getDeclaredMethod("m1");
        m1.insertAfter("$_ = 7; $_ = ($r)k1(0);", false);

        CtMethod m2 = cc.getDeclaredMethod("m2");
        m2.insertAfter("$_ = ($r)k2(0);", false);

        CtMethod m3 = cc.getDeclaredMethod("m3");
        m3.insertAfter("$_ = ($r)k3(0);", false);

        CtMethod m4 = cc.getDeclaredMethod("m4");
        try {
            m4.insertAfter("$_ = ($r)1;", false);
            assertTrue(false);
        }
        catch (CannotCompileException e) {
        }

        CtMethod m5 = cc.getDeclaredMethod("m5");
        m5.insertAfter("$_ = ($r)k5(0);", false);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(17, invoke(obj, "test"));
    }

    public void testCalleeAfter3() throws Exception {
        CtClass cc = sloader.get("test1.CalleeAfter3");
        CtMethod m1 = cc.getDeclaredMethod("m1");
        m1.insertAfter("value++;", true);
        CtMethod m2 = cc.getDeclaredMethod("m2");
        m2.insertAfter("value++;", true);
        CtMethod m3 = cc.getDeclaredMethod("m3");
        m3.insertAfter("value++;", true);
        CtMethod m4 = cc.getDeclaredMethod("m4");
        m4.insertAfter("value++;", true);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(22, invoke(obj, "test"));
    }

    public void testCalleeCatch() throws Exception {
        CtClass cc = sloader.get("test1.CalleeCatch");

        CtMethod m1 = cc.getDeclaredMethod("m1");
        m1.addCatch("{ System.out.println($e); return p; }",
                    sloader.get("java.lang.Exception"));

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "test"));
    }

    public void testSuperclass() throws Exception {
        CtClass cc = sloader.get("java.lang.Object");
        assertEquals(null, cc.getSuperclass());
    }

    public void testProceed() throws Exception {
        CtClass cc = sloader.get("test1.Proceed");

        CtMethod m1 = CtNewMethod.make(
                       "public int m1() { return $proceed(3); }",
                       cc, "this", "k1");
        CtMethod m2 = CtNewMethod.make(
                       "public int m2() { return $proceed(3); }",
                       cc, "another", "k2");
        CtMethod m3 = CtNewMethod.make(
                        "public int q(int i) { return p($1 + 1, $$); }", cc);
        cc.addMethod(m1);
        cc.addMethod(m2);
        cc.addMethod(m3);
        CtMethod m4 = CtNewMethod.make(
                        "public int q2() { return q(4); }", cc);
        cc.addMethod(m4);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "m1"));
        assertEquals(4, invoke(obj, "m2"));
        assertEquals(9, invoke(obj, "q2"));
    }

    public void testProceed2() throws Exception {
        CtClass cc = sloader.get("test1.Proceed2");
        CtMethod m1 = cc.getDeclaredMethod("k1");
        m1.instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    m.replace("{ $_ = $proceed($$); }");
                }
                public void edit(NewExpr m) throws CannotCompileException {
                    m.replace("{ $_ = $proceed($$); }");
                }
                public void edit(FieldAccess m) throws CannotCompileException {
                    m.replace("{ $_ = $proceed($$); }");
                }
                public void edit(Instanceof i) throws CannotCompileException {
                    i.replace("{ $_ = $proceed($$); }");
                }
                public void edit(Cast c) throws CannotCompileException {
                    c.replace("{ $_ = $proceed($$); }");
                }
            });
        
        CtMethod m2 = cc.getDeclaredMethod("k2");
        m2.instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    m.replace("{ $proceed(); }");
                }
                public void edit(NewExpr m) throws CannotCompileException {
                    m.replace("{ $_ = $proceed(); }");
                }
                public void edit(FieldAccess m) throws CannotCompileException {
                    if (m.isReader())
                        m.replace("{ $_ = $proceed(); }");
                    else
                        m.replace("{ $proceed($$); }");
                }
            });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(2, invoke(obj, "k1"));
    }

    public void testProceed3() throws Exception {
        CtClass cc = sloader.get("test1.Proceed3");
        CtMethod m1 = cc.getDeclaredMethod("p");
        CtMethod m2 = CtNewMethod.copy(m1, cc, null);
        m1.setName(m1.getName() + "_orig");
        m2.setBody("{ return $proceed($1 + 1); }", "this", m1.getName());
        cc.addMethod(m2);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(4, invoke(obj, "k1"));
    }

    public void testSetBody() throws Exception {
        CtClass cc = sloader.get("test1.SetBody");
        CtMethod m1 = cc.getDeclaredMethod("m1");
        m1.setBody("{ int i = $1 * $2; return i; }");
        CtMethod m2 = cc.getDeclaredMethod("m2");
        m2.setBody("System.out.println(\"setbody: \" + $1);");

        CtMethod m3 = cc.getDeclaredMethod("m3");
        try {
            m3.setBody("value = 1; System.out.println(\"setbody: \" + $1);");
            fail();
        }
        catch (CannotCompileException e) {
            // System.err.println(e);
        }

        CtConstructor cons
            = new CtConstructor(new CtClass[] { CtClass.intType }, cc);
        cons.setBody(null);
        cc.addConstructor(cons);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(12, invoke(obj, "run"));
    }

    public void testSetStaticConsBody() throws Exception {
        CtClass cc = sloader.get("test1.StaticConsBody");
        CtConstructor cons = cc.getClassInitializer();
        cons.setBody(null);

        cons = cc.getConstructors()[0];
        cons.setBody(null);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(0, invoke(obj, "run"));
    }

    public void testSetConsBody() throws Exception {
        CtClass superClazz = sloader.get("java.io.File");
        CtClass cc = sloader.makeClass("test1.SetConsBody");
        cc.setSuperclass(superClazz);
        CtConstructor constructor = new CtConstructor(new CtClass[0], cc);
        constructor.setBody("super(\"MyFile\");");
        cc.addConstructor(constructor);

        constructor = new CtConstructor(new CtClass[] { CtClass.intType },
                                        cc);
        constructor.setBody("{ super(\"MyFile\"); }");
        cc.addConstructor(constructor);

        cc.addMethod(CtNewMethod.make(CtClass.voidType, "m1",
                                      null, null, null, cc));
        cc.addMethod(CtNewMethod.make(CtClass.intType, "m2",
                                      null, null, null, cc));
        cc.addMethod(CtNewMethod.make(CtClass.byteType, "m3",
                                      null, null, null, cc));
        cc.addMethod(CtNewMethod.make(CtClass.longType, "m4",
                                      null, null, null, cc));
        cc.addMethod(CtNewMethod.make(CtClass.floatType, "m5",
                                      null, null, null, cc));
        cc.addMethod(CtNewMethod.make(CtClass.doubleType, "m6",
                                      null, null, null, cc));
        cc.addMethod(CtNewMethod.make(sloader.get("int[]"), "m7",
                                      null, null, null, cc));

        cc.addMethod(CtNewMethod.make(
            "public int run() {"
          + "  return (int)(m2() + m3() + m4() + m5() + m6() + 3); }", cc));
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "run"));
    }

    public void testEmptyBody() throws Exception {
        String[] methods = { "m1", "m2", "m3", "m4" };
        boolean[] results = { true, false, false, false, true };
        boolean[] cResults = { true, true, false, false, false, true };

        CtClass cc = sloader.get("test1.EmptyBody");
        for (int i = 0; i < methods.length; ++i) {
            CtMethod m = cc.getDeclaredMethod(methods[i]);
            assertEquals(results[i], m.isEmpty());
        }

        CtConstructor[] cons = cc.getDeclaredConstructors();
        for (int j = 0; j < cons.length; ++j)
            assertEquals(cResults[j], cons[j].isEmpty());
    }

    public void testExprEditor() throws Exception {
        CtClass cc = sloader.get("test1.ExprEdit");

        CtMethod m1 = cc.getDeclaredMethod("k0");
        m1.instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getClassName().equals("test1.ExprEdit")) {
                        String name = m.getMethodName();
                        if (name.equals("k1") || name.equals("k2")) {
                            try {
                                CtMethod cm = m.getMethod();
                                print(cm.getParameterTypes()[0].getName());
                                print(cm.getReturnType().getName());
                            }
                            catch (NotFoundException e) {
                                throw new CannotCompileException(e);
                            }
                            m.replace("{ ++$1; $_ = $proceed($$); }");
                        }
                        else if (name.equals("k3"))
                            m.replace("{ ++$1; $proceed($$); }");
                    }
                }
            });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(12, invoke(obj, "k0"));
    }

    public void testExprEditor2() throws Exception {
        CtClass cc = sloader.get("test1.ExprEdit2");

        CtMethod m1 = cc.getDeclaredMethod("k1");
        m1.instrument(new ExprEditor() {
            public void edit(FieldAccess m) throws CannotCompileException {
                if (m.getClassName().equals("test1.ExprEdit2")) {
                    String name = m.getFieldName();
                    try {
                        CtField cf = m.getField();
                        print(cf.getType().getName());
                        print("file: " + m.getFileName());
                        print("line: " + m.getLineNumber());
                    }
                    catch (NotFoundException e) {
                        throw new CannotCompileException(e);
                    }
                    if (name.equals("df"))
                        if (m.isReader())
                            m.replace("{ $_ = $proceed() + 1; }");
                        else
                            m.replace("{ $proceed($1 + 1); }");
                      else if (name.equals("sf"))
                        if (m.isReader())
                            m.replace("{ $_ = $proceed() + 2; }");
                        else
                            m.replace("{ $proceed($1 + 2); }");
                }
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(16, invoke(obj, "k1"));
    }

    public void testExprEditor3() throws Exception {
        CtClass cc = sloader.get("test1.ExprEdit3");

        CtMethod m1 = cc.getDeclaredMethod("k1");
        m1.instrument(new ExprEditor() {
            public void edit(NewExpr m) throws CannotCompileException {
                System.out.println("new " + m.getClassName());
                try {
                    CtConstructor cc = m.getConstructor();
                    print(cc.getParameterTypes()[0].getName());
                }
                catch (NotFoundException e) {
                    throw new CannotCompileException(e);
                }
                if (m.getClassName().equals("test1.ExprEdit3")) {
                    m.replace("{ ++$2; $_ = $proceed($$); }");
                }
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(4, invoke(obj, "k1"));
    }

    public void testExprEditor4() throws Exception {
        CtClass cc = sloader.get("test1.ExprEdit4");

        CtMethod m1 = cc.getDeclaredMethod("k1");
        m1.instrument(new ExprEditor() {
            public void edit(NewExpr m) throws CannotCompileException {
                System.out.println("new " + m.getClassName());
                if (m.getClassName().equals("test1.ExprEdit4"))
                    m.replace("$_ = null;");
            }

            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getClassName().equals("test1.ExprEdit4"))
                    m.replace("{}");
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "k1"));
    }

    public void testExprEditor5() throws Exception {
        CtClass cc = sloader.get("test1.ExprEdit5");

        CtMethod m1 = cc.getDeclaredMethod("k1");
        m1.instrument(new ExprEditor() {
            public void edit(NewExpr m) throws CannotCompileException {
                m.replace("{ $_ = $proceed($$, \"test\"); }");
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "k1"));
    }

    public void testExprEditor6() throws Exception {
        CtClass cc = sloader.get("test1.ExprEdit6");
        cc.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                assertTrue(m.where().getName().equals("k1"));
                m.replace("$_ = 3;");
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "k1"));
    }

    public void testExprEditor7() throws Exception {
        CtClass cc = sloader.get("test1.ExprEdit7");
        cc.instrument(new ExprEditor() {
            public void edit(Instanceof i) throws CannotCompileException {
                i.replace("{ this.c1 = $type; $_ = !$proceed($1); }");
            }
            public void edit(Cast c) throws CannotCompileException {
                c.replace("{ this.c2 = $type; $_ = ($r)$1; }");
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(7, invoke(obj, "k1"));
    }

    public void testExprEditor8() throws Exception {
        CtClass cc = sloader.get("test1.ExprEdit8");
        cc.instrument(new ExprEditor() {
            public void edit(ConstructorCall c) throws CannotCompileException {
                assertTrue(c.isSuper());
                c.replace("{ $_ = $proceed($$); value = 7; }");
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(7, invoke(obj, "k1"));
    }

    public void testCflow() throws Exception {
        CtClass cc = sloader.get("test1.Cflow");
        CtMethod m1 = cc.getDeclaredMethod("k1");
        m1.useCflow("cflow1");
        m1.insertBefore("System.out.println(\"$cflow1: \" + $cflow(cflow1));");
        m1.insertAfter("System.out.println(\"*$cflow1: \" + $cflow(cflow1));",
                       true);
        CtMethod m2 = cc.getDeclaredMethod("k2");
        m2.useCflow("test1.t.cflow2");
        m2.insertBefore(
                "System.out.println(\"$cflow2: \" + $cflow(test1.t.cflow2));");
        CtMethod m3 = cc.getDeclaredMethod("fact");
        m3.useCflow("fact");
        m3.insertBefore("if ($cflow(fact) == 0)"
                      + "    System.out.println(\"fact \" + $1);");

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "run"));
        assertEquals(120, invoke(obj, "run2"));
    }

    public void testSigType() throws Exception {
        CtClass cc = sloader.get("test1.SigType");

        CtMethod m1 = cc.getDeclaredMethod("k1");
        m1.insertBefore("{ Class[] p = $sig; $1 += p.length; }");
        m1.insertAfter("System.out.println(\"testSigType: \""
                       + " + $type.getName());", false);

        CtMethod m2 = cc.getDeclaredMethod("k2");
        m2.instrument(new ExprEditor() {
            public void edit(FieldAccess m) throws CannotCompileException {
                m.replace("{ $_ = $proceed($$) + $type.getName().length(); }");
            }

            public void edit(MethodCall m) throws CannotCompileException {
                m.replace("{ $_ = $proceed($$) + $sig.length; }");
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(19, invoke(obj, "run"));
    }

    public void testDollarClass() throws Exception {
        CtClass cc = sloader.get("test1.DollarClass");

        CtMethod m1 = cc.getDeclaredMethod("k1");
        m1.insertBefore("{ $1 += $class.getName().length(); }");

        CtMethod m2 = cc.getDeclaredMethod("k2");
        m2.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                m.replace("{ $_ = $class.getName().length(); }");
            }
        });
        m2.insertBefore("{ $1 += $class.getName().length(); }");

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(58, invoke(obj, "run"));
    }

    public void testHandler() throws Exception {
        CtClass cc = sloader.get("test1.Handler");

        CtMethod m1 = cc.getDeclaredMethod("m1");
        m1.instrument(new ExprEditor() {
            public void edit(Handler h) throws CannotCompileException {
                try {
                    print(h.getType().getName());
                    h.insertBefore(
                        "{ p = (($r)$1).getClass().getName().length()"
                        + "+ $type.getName().length(); }");
                }
                catch (NotFoundException e) {
                    throw new CannotCompileException(e);
                }
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(("java.lang.IndexOutOfBoundsException".length()
                      + "java.lang.ClassNotFoundException".length())
                     * 2 + 1,
                     invoke(obj, "test"));
    }

    public void testInterface() throws Exception {
        String className = "test1.NewInterface";
        ClassPool pool = ClassPool.getDefault();
        CtClass targetCtClass = pool.get(className);
        CtClass ctInterface
            = pool.makeInterface(className + "2");
        CtMethod[] ctMethods = targetCtClass.getDeclaredMethods();
        for (int i = 0;i < ctMethods.length; i++) {
            String code = Modifier.toString(ctMethods[i].getModifiers())
                + " " + ctMethods[i].getReturnType().getName()
                + " " + ctMethods[i].getName() + "();";

            System.out.println(code);
            CtMethod m = CtNewMethod.make(code, ctInterface);
            ctInterface.addMethod(m);
        }
 
        targetCtClass.addInterface(ctInterface);
        targetCtClass.stopPruning(true);
        targetCtClass.writeFile();

        ctInterface.stopPruning(true);
        ctInterface.writeFile();
        ctInterface.toClass(DefineClassCapability.class);
        targetCtClass.toClass(DefineClassCapability.class);
    }

    public void testDispatch() throws Exception {
        CtClass cc = sloader.get("test1.Dispatch");

        CtMethod m1 = cc.getDeclaredMethod("run");
        m1.insertAfter("$_ += f(new Object[1]);");
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(7, invoke(obj, "run"));
    }

    public void testMakeClass()throws Exception {
        CtClass cc = sloader.makeClass(
                new FileInputStream(PATH + "test1/MakeClass.class"));
        assertEquals("test1.MakeClass", cc.getName());
        assertEquals(cc, sloader.get(cc.getName()));
        cc.toBytecode();
        assertTrue(cc.isFrozen());
        try {
            cc = sloader.makeClass(
                        new FileInputStream(PATH + "test1/MakeClass.class"));
            assertTrue(false);
        }
        catch (RuntimeException e) {
            print(e.getMessage());
        }
    }

    public void testMakeMethod() throws Exception {
        CtClass cc = sloader.makeClass("test1.MakeMethod");
        cc.addField(new CtField(CtClass.intType, "i", cc));
        String cons_body = "{ i = 3; }";
        CtConstructor cons = CtNewConstructor.make(null, null,
                                                   cons_body, cc);
        cc.addConstructor(cons);
        CtMethod m = CtNewMethod.make(CtClass.intType, "run", null, null,
                                "{ return i; }", cc);
        cc.addMethod(m);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "run"));
    }

    public void testDesc() throws Exception {
        Class[] sig;

        assertEquals(int.class, Desc.getType("I"));
        assertEquals(String.class, Desc.getType("Ljava/lang/String;"));
        assertEquals(String[].class, Desc.getType("[Ljava/lang/String;"));
        assertEquals(int[].class, Desc.getType("[I"));

        sig = Desc.getParams("()V");
        assertEquals(0, sig.length);

        sig = Desc.getParams("(I)V");
        assertEquals(int.class, sig[0]);
        assertEquals(1, sig.length);

        sig = Desc.getParams("(IJ)V");
        assertEquals(long.class, sig[1]);
        assertEquals(2, sig.length);

        sig = Desc.getParams("(Ljava/lang/String;)V");
        assertEquals(String.class, sig[0]);
        assertEquals(1, sig.length);

        sig = Desc.getParams("([Ljava/lang/String;I)V");
        assertEquals(String[].class, sig[0]);
        assertEquals(2, sig.length);

        sig = Desc.getParams("(Ljava/lang/String;[Ljava/lang/String;)V");
        assertEquals(String[].class, sig[1]);
        assertEquals(2, sig.length);
    }

    public void testCast() throws Exception {
        CtClass cc = sloader.makeClass("test1.CastTest");

        StringBuffer src = new StringBuffer();
        src.append("public void test(java.lang.String[] strValues)\n");
        src.append("{\n");
        src.append("\tObject[] values = new Object[2];");
        src.append("\tvalues[0] = strValues;");
        src.append("\tvalues[1] = strValues;");
        src.append("\tstrValues = (String[])values[0];");
        src.append("}\n");

        CtMethod m = CtNewMethod.make(src.toString(), cc);
    }

    static final long svUID = 6006955401253799668L;

    public void testSerialVUID() throws Exception {
        CtClass cc = sloader.get("test1.MySerializableClass");
        assertEquals(svUID, SerialVersionUID.calculateDefault(cc));
        SerialVersionUID.setSerialVersionUID(cc);
        cc.writeFile();
    }

    public void testInvokeInt() throws Exception {
        CtClass cc = sloader.get("test1.InvokeInt");
        CtMethod m1 = cc.getDeclaredMethod("check");

        m1.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                m.replace("$_ = $proceed($$) + k(1);");
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(6, invoke(obj, "run"));
    }

    public void testSubtypeOf() throws Exception {
        testSubtypeOf2("java.lang.Object", "int", false);
        testSubtypeOf2("int[]", "java.lang.Object", true);
        testSubtypeOf2("int[]", "java.lang.Cloneable", true);
        testSubtypeOf2("java.lang.Object", "int[]", false);
        testSubtypeOf2("java.lang.Integer", "java.lang.Number", true);
        testSubtypeOf2("java.lang.Number", "java.lang.Integer", false);
        testSubtypeOf2("java.lang.Integer[]", "java.lang.Number[]", true);
        testSubtypeOf2("java.lang.Number[]", "java.lang.Integer[]", false);
        testSubtypeOf2("java.lang.Integer", "java.io.Serializable", true);
        testSubtypeOf2("java.lang.Integer", "java.lang.Object", true);
    }

    private void testSubtypeOf2(String s, String t, boolean b)
        throws Exception
    {
        assertTrue(sloader.get(s).subtypeOf(sloader.get(t)) == b);
    }

    public void testMakeInterface() throws Exception {
        CtClass cc = sloader.makeInterface("test1.MkInterface");
        CtMethod m = CtNewMethod.make("public abstract void ready();", cc);
        cc.addMethod(m);
        cc.writeFile();
        // cloader.loadClass(cc.getName());
        java.io.File genDir = new java.io.File(".");
        java.net.URLClassLoader ucl = new java.net.URLClassLoader(
                        new java.net.URL[] { genDir.toURI().toURL() }, null);
        Class intf = ucl.loadClass("test1.MkInterface");
    }

    public void testCodeConv() throws Exception {
        CtClass cc = sloader.get("test1.CodeConv");
        CtClass pc = sloader.get("test1.CodeConvP");

        CodeConverter conv = new CodeConverter();
        conv.replaceFieldRead(pc.getDeclaredField("a1"), cc, "getA1");
        conv.replaceFieldRead(pc.getDeclaredField("a2"), cc, "getA2");
        conv.redirectFieldAccess(pc.getDeclaredField("a3"), cc, "a4");
        conv.replaceFieldWrite(pc.getDeclaredField("b1"), cc, "putB1");

        cc.instrument(conv);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(51, invoke(obj, "run"));
    }

    public void testTryCatch() throws Exception {
        CtClass cc = sloader.get("test1.TryCatch");
        CtMethod m1 = cc.getDeclaredMethod("m1");

        m1.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                m.replace(
                        "try { doit(); }"
                      + "catch(NullPointerException e){ init(); doit(); }");
            }
        });

        final String src =
              "try { doit(); }"
            + "catch(NullPointerException e){ init(); doit(); return a; }";

        CtMethod p1 = cc.getDeclaredMethod("p1");
        p1.insertAfter(src, true);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(4, invoke(obj, "run"));
        Object obj2 = make(cc.getName());
        assertEquals(4, invoke(obj2, "p1"));
    }

    private CtClass[] throwablesList = null;

    public void testGetThrowables() throws Exception {
        CtClass cc = sloader.get("test1.GetThrowables");
        CtMethod m1 = cc.getDeclaredMethod("run");
        m1.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                throwablesList = m.mayThrow();
            }
        });

        System.out.println(throwablesList[0].getName());
        System.out.println(throwablesList[1].getName());
        assertEquals(2, throwablesList.length);
    }

    public void testArrayAccess() throws Exception {
        CtClass cc = sloader.get("test1.ArrayAccess");
        CtMethod m1 = cc.getDeclaredMethod("test");
        m1.insertBefore("{ ia[0] += 1; iaa[1] = iaa[0]; }");
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(8, invoke(obj, "test"));
    }

    public void testClinit() throws Exception {
        CtClass cc = sloader.get("test1.Clinit");
        CtConstructor clinit = cc.getClassInitializer();
        assertTrue(clinit != null);
        try {
            clinit.insertBeforeBody(";");
            assertTrue(false);
        }
        catch (CannotCompileException e) {
            print(e.toString());
            assertEquals("class initializer", e.getReason());
        }

        CtConstructor[] init = cc.getConstructors();
        assertEquals(1, init.length);
        clinit.insertAfter("j += 1;");
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(457, invoke(obj, "run"));
    }

    public void testClinit2() throws Exception {
        CtClass cc = sloader.get("test1.Clinit2");
        CtConstructor clinit = cc.makeClassInitializer();
        clinit.insertAfter("j = 7;");
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(7, invoke(obj, "run"));
    }

    // by yamazaki
    public void testCondExpr() throws Exception {
        CtClass cc = sloader.makeClass("test1.CondExpr");
        CtMethod methodM = new CtMethod(CtClass.intType, "m",
                               new CtClass[]{ CtClass.intType }, cc);
        methodM.setModifiers(methodM.getModifiers() | Modifier.STATIC);
        methodM.setBody("{if($1 <= 0) return 1; else return 0;}");
        cc.addMethod(methodM);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(0, invoke(obj, "m", 3));
    }

    // by yamazaki
    public void testCondExpr2() throws Exception {
        CtClass cc = sloader.makeClass("test1.CondExpr2");
        CtMethod methodM = new CtMethod(CtClass.intType, "m",
                               new CtClass[]{ CtClass.intType }, cc);
        methodM.setModifiers(methodM.getModifiers() | Modifier.STATIC);
        methodM.setBody("{return ($1 <= 0) ? 1 : (m($1 - 1) * $1);}");
        cc.addMethod(methodM);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(6, invoke(obj, "m", 3));
    }

    // by yamazaki
    public void testCondExpr3() throws Exception {
        CtClass cc = sloader.makeClass("test1.CondExpr3");

        CtMethod methodM = CtNewMethod.make(
                                    "public abstract int m(int i);", cc);
        CtMethod methodN = CtNewMethod.make(
                                    "public abstract int n(int i);", cc);
        cc.addMethod(methodM);
        cc.addMethod(methodN);

        methodM.setBody("{return ($1 <= 0) ? 1 : (n($1 - 1) * $1);}");
        methodN.setBody("{return m($1);}");

        cc.setModifiers(cc.getModifiers() & ~Modifier.ABSTRACT);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(6, invoke(obj, "m", 3));
    }

    public void testDelegator() throws Exception {
        CtClass cc = sloader.get("test1.Delegator");

        assertEquals("test1.SuperDelegator", cc.getSuperclass().getName());

        CtMethod f = sloader.getMethod("test1.SuperDelegator", "f");
        CtMethod g = sloader.getMethod("test1.SuperDelegator", "g");

        cc.addMethod(CtNewMethod.delegator(f, cc));
        cc.addMethod(CtNewMethod.delegator(g, cc));

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(15, invoke(obj, "run"));
    }

    public void testSetName() throws Exception {
        CtClass cc = sloader.get("test1.SetName");
        CtMethod m0 = cc.getDeclaredMethod("foo");
        cc.setName("test1.SetName2");
        assertEquals(cc, sloader.get("test1.SetName2"));
        assertEquals("foo(Ltest1/SetName2;)", m0.getStringRep());
        CtClass cc2 = sloader.makeClass("test1.SetName3");
        CtMethod m = CtNewMethod.make(
            "public int m(test1.SetName2 obj) { return ((test1.SetName2)obj).i; }",
            cc2);
        cc2.addMethod(m);
        cc.writeFile();
        cc2.writeFile();
    }

    public void testFieldModifier() throws Exception {
        CtClass cc = sloader.get("test1.FieldMod");
        CtField f = cc.getField("text");
        f.setModifiers(Modifier.PUBLIC); 
        f = cc.getField("i");
        f.setName("j");
        cc.writeFile();

        Object obj = make(cc.getName());
        assertEquals(java.lang.reflect.Modifier.PUBLIC,
                     obj.getClass().getField("text").getModifiers());
        assertTrue(obj.getClass().getField("j") != null);
    }

    public void testToString() throws Exception {
        System.out.println(sloader.get("test1.FieldMod"));
        System.out.println(sloader.get("java.lang.Object"));
    }

    public void testPackage() throws Exception {
        Object obj = new Loader().loadClass("test1.Pac").getConstructor().newInstance();
        assertEquals(1, invoke(obj, "run"));
    }

    public void testHoward() throws Exception {
        String head =
            "public Object lookup() throws java.rmi.RemoteException ";
        String src =
            "{   if (_remote != null) return _remote;"
          + "    test1.HowardHome h = (test1.HowardHome)lookup(\"Howard\");"
          + "    try { _remote = h.create(); }"
          + "    catch (java.io.IOException e) { throw new java.rmi.RemoteException(e.getMessage(), e); }"
          + "    return _remote; }";

        String src2 =
            "public void lookup2() {"
          + "    try {}"
          + "    catch (java.io.IOException e) { throw new Exception(e); }"
          + "}";

        CtClass cc = sloader.get("test1.Howard");
        CtMethod m = CtNewMethod.make(head + src, cc);
        cc.addMethod(m);
        try {
            CtMethod m2 = CtNewMethod.make(src2, cc);
            cc.addMethod(m2);
            assertTrue(false);
        }
        catch (CannotCompileException e) {}

        m = new CtMethod(sloader.get("java.lang.Object"),
                         "lookup3", null, cc);
        m.setBody(src);
        m.setModifiers(Modifier.PUBLIC);
        m.setExceptionTypes(new CtClass[] {
            sloader.get("java.rmi.RemoteException") });
        cc.addMethod(m);

        cc.writeFile();
        Object target = make(cc.getName());
        Method mth = target.getClass().getMethod("lookup", new Class[0]);
        Object res = mth.invoke(target, new Object[0]);
        assertEquals("howard4", res);

        mth = target.getClass().getMethod("lookup3", new Class[0]);
        res = mth.invoke(target, new Object[0]);
        assertEquals("howard4", res);
    }

    public void testLoop() throws Exception {
        CtClass cc = sloader.makeClass("test1.Loop");
        CtMethod m = CtNewMethod.make(
            "public int run(int i) { int k = 0;"
            + "while (true) { if (k++ > 10) return i; } }",
            cc);
        cc.addMethod(m);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "run", 3));
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Javassist Tests");
        suite.addTestSuite(JvstTest.class);
        suite.addTestSuite(JvstTest2.class);
        suite.addTestSuite(JvstTest3.class);
        suite.addTestSuite(JvstTest4.class);
        suite.addTestSuite(JvstTest5.class);
        suite.addTestSuite(LoaderTestByRandall.class);
        suite.addTestSuite(javassist.bytecode.BytecodeTest.class);
        suite.addTestSuite(javassist.bytecode.StackMapTest.class);
        suite.addTestSuite(javassist.compiler.CompTest.class);
        suite.addTestSuite(javassist.SetterTest.class);
        suite.addTestSuite(javassist.bytecode.InsertGap0.class);
        suite.addTestSuite(javassist.tools.reflect.LoaderTest.class);
        suite.addTestSuite(javassist.tools.CallbackTest.class);
        suite.addTestSuite(testproxy.ProxyTester.class);
        suite.addTestSuite(testproxy.ProxyFactoryPerformanceTest.class); // remove?
        suite.addTestSuite(javassist.proxyfactory.ProxyFactoryTest.class);
        suite.addTestSuite(javassist.proxyfactory.Tester.class);
        suite.addTestSuite(javassist.HotswapTest.class);
        suite.addTestSuite(test.javassist.proxy.ProxySerializationTest.class);
        suite.addTestSuite(test.javassist.convert.ArrayAccessReplaceTest.class);
        suite.addTestSuite(test.javassist.proxy.JASSIST113RegressionTest.class);
        suite.addTestSuite(test.javassist.proxy.JBPAPP9257Test.class);
        suite.addTestSuite(test.javassist.proxy.ProxyCacheGCTest.class);  // remvoe?
        suite.addTestSuite(test.javassist.proxy.ProxyFactoryCompatibilityTest.class);
        suite.addTestSuite(test.javassist.proxy.ProxySerializationTest.class);
        suite.addTestSuite(test.javassist.proxy.ProxySimpleTest.class);
        suite.addTestSuite(test.javassist.bytecode.analysis.AnalyzerTest.class);
        suite.addTestSuite(test.javassist.convert.ArrayAccessReplaceTest.class);
        suite.addTestSuite(test.javassist.convert.ArrayAccessReplaceTest2.class);
        suite.addTestSuite(test.javassist.bytecode.analysis.DomTreeTest.class);
        suite.addTestSuite(javassist.bytecode.SignatureAttributeTest.class);
        return suite;
    }
}
