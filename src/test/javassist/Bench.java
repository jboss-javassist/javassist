package javassist;

import junit.framework.*;
import javassist.expr.*;
import javassist.compiler.*;

public class Bench extends JvstTestRoot {
    public Bench(String name) {
        super(name);
    }

    public void testProceed() throws Exception {
        CtClass cc = sloader.get("test.BenchProceed");
        CtMethod m1 = cc.getDeclaredMethod("p");
        m1.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("calc"))
                    m.replace("{ before($args); $_ = $proceed($$); }");
            }
        });

        CtMethod m2 = cc.getDeclaredMethod("q");
        m2.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("calc"))
                    m.replace("{ $_ = ($r)replace($args); }");
            }
        });

        CtMethod m3 = cc.getDeclaredMethod("s");
        m3.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("calc2"))
                    m.replace(
                        "{ long start = System.currentTimeMillis();"
                      + "$_ = $proceed($$);"
                      + "long elapsed = System.currentTimeMillis() - start;"
                      + "System.out.println(elapsed); }");
            }
        });

        CtMethod m4 = cc.getDeclaredMethod("t");
        m4.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("calc2"))
                    m.replace(
                        "{ long start = System.currentTimeMillis();"
                      + "$_ = $proceed($$);"
                      + "System.out.println(System.currentTimeMillis() - start);"
                      + "}");
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        int ptime = invoke(obj, "p");
        int qtime = invoke(obj, "q");
        System.out.println("time: (p) " + ptime + ", (q) " + qtime);
        System.out.println("s:");
        invoke(obj, "s");
        System.out.println("t:");
        invoke(obj, "t");
        assertTrue(ptime < qtime);
    }

    public void testProceedNew() throws Exception {
        CtClass cc = sloader.get("test.BenchProceedNew");
        CtMethod m1 = cc.getDeclaredMethod("jvst0");
        m1.instrument(new ExprEditor() {
            public void edit(NewExpr m) throws CannotCompileException {
                m.replace("{ $_ = $proceed($$); }");
            }
        });

        CtMethod m2 = cc.getDeclaredMethod("jvst2");
        m2.instrument(new ExprEditor() {
            public void edit(NewExpr m) throws CannotCompileException {
                m.replace("{ $_ = $proceed($$); }");
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        int qtime = invoke(obj, "jvst0");
        int ptime = invoke(obj, "org0");
        System.out.println("time: (org0) " + ptime + ", (jvst0) " + qtime);
        qtime = invoke(obj, "jvst2");
        ptime = invoke(obj, "org2");
        System.out.println("time: (org2) " + ptime + ", (jvst2) " + qtime);
    }

    public void testStaticMethod() throws Exception {
        CtClass cc = sloader.get("test.BenchStaticMethod");
        CtMethod m1 = cc.getDeclaredMethod("test");
        m1.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("foo"))
                    m.replace("{ num += $1; $_ = $proceed($$); }");
            }
        });

        cc.writeFile();
        Object obj = make(cc.getName());
        int qtime = invoke(obj, "test");
        int ptime = invoke(obj, "orgTest");
        System.out.println(
            "BenchStaticMethod time: (org) " + ptime + ", (jvst) " + qtime);
    }

    public void testStaticField() throws Exception {
        System.out.println(sloader);
        Javac jc = new Javac(sloader.get("test.StaticField"));
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 100; i++)
            jc.compileStmnt("{ int counter = 0; counter++; }");

        t0 = System.currentTimeMillis() - t0;
        System.out.println("local variable: " + (t0 * 10) + " usec");

        long t = System.currentTimeMillis();
        for (int i = 0; i < 100; i++)
            jc.compileStmnt("{ test.StaticField.counter++; }");

        t = System.currentTimeMillis() - t;
        System.out.println("StaticField: " + (t * 10) + " usec");

        long t2 = System.currentTimeMillis();
        for (int i = 0; i < 100; i++)
            jc.compileStmnt("{ test.StaticField#counter++; }");

        t2 = System.currentTimeMillis() - t2;
        System.out.println("StaticField with #: " + (t2 * 10) + " usec");

        long t3 = System.currentTimeMillis();
        for (int i = 0; i < 100; i++)
            jc.compileStmnt("{ StaticField.counter2++; }");

        t3 = System.currentTimeMillis() - t3;
        System.out.println("StaticField without package: " + (t3 * 10) + " usec");

        long t4 = System.currentTimeMillis();
        for (int i = 0; i < 100; i++)
            jc.compileStmnt("{ test.StaticField.counter++; }");

        t4 = System.currentTimeMillis() - t4;
        System.out.println("StaticField: " + (t4 * 10) + " usec");

        long t5 = System.currentTimeMillis();
        for (int i = 0; i < 100; i++)
            jc.compileStmnt("{ System.out.println(); }");

        t5 = System.currentTimeMillis() - t5;
        System.out.println("println: " + (t5 * 10) + " usec");
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Benchmark Tests");
        suite.addTestSuite(Bench.class);
        suite.addTestSuite(testproxy.ProxyFactoryPerformanceTest.class);
        return suite;
    }
}
