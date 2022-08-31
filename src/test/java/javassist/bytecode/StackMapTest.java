package javassist.bytecode;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.CodeConverter;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.JvstTest;
import javassist.Loader;
import javassist.bytecode.stackmap.MapMaker;
import javassist.bytecode.stackmap.TypeData;
import junit.framework.TestCase;

@SuppressWarnings({"rawtypes","unused"})
public class StackMapTest extends TestCase {
    public static final String PATH = JvstTest.PATH;
    private ClassPool loader, dloader;
    private Loader cloader;

    public StackMapTest(String name) { super(name); }

    protected void setUp() throws Exception {
        loader = ClassPool.getDefault();
        dloader = new ClassPool(null);
        dloader.appendSystemPath();
        dloader.insertClassPath(".");
        cloader = new Loader(dloader);
    }

    protected Object make(String name) throws Exception {
        return cloader.loadClass(name).getConstructor().newInstance();
    }

    protected int invoke(Object target, String method) throws Exception {
        Method m = target.getClass().getMethod(method, new Class[0]);
        Object res = m.invoke(target, new Object[0]);
        return ((Integer)res).intValue();
    }

    protected static void rebuildStackMaps(CtClass cc) throws BadBytecode, IOException {
        ClassPool cp = cc.getClassPool();
        Iterator it = cc.getClassFile().getMethods().iterator();
        while (it.hasNext()) {
            MethodInfo minfo = (MethodInfo)it.next();
            rebuildStackMap(cc, cp, minfo);
        }
    }

    protected static void rebuildStackMap(CtClass cc, ClassPool cp, MethodInfo minfo) throws BadBytecode, IOException {
        CodeAttribute ca = minfo.getCodeAttribute();
        if (ca != null) {
            StackMapTable smt = (StackMapTable)ca.getAttribute(StackMapTable.tag);
            if (smt != null) {
                String data = readSmt(smt);
                StackMapTable smt2 = MapMaker.make(cp, minfo);
                String data2 = readSmt(smt2);
                try {
                    assertEquals(cc.getName() + ":" + minfo.getName() + ":" + minfo.getDescriptor(),
            	                     data, data2);
            	}
                catch (junit.framework.ComparisonFailure e) {
                    System.out.println("*** " + cc.getName() + ":" + minfo.getName() + ":" + minfo.getDescriptor());
                    smt.println(System.out);
                    System.out.println("---");
                    smt2.println(System.out);
                }
            }
        }
    }

    protected static void rebuildStackMaps2(CtClass cc) throws BadBytecode {
    	ClassPool cp = cc.getClassPool();
        Iterator it = cc.getClassFile().getMethods().iterator();
        while (it.hasNext()) {
            MethodInfo minfo = (MethodInfo)it.next();
            minfo.rebuildStackMap(cp);
        }
    }

    protected static String readSmt(StackMapTable smt) throws BadBytecode, IOException {
    	if (smt == null)
    		return "";

    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	PrintStream pout = new PrintStream(out);
    	smt.println(pout);
    	pout.close();
    	return out.toString();
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("-c")) { 
            CtClass cc = ClassPool.getDefault().get(args[0].replace('/', '.'));
            rebuildStackMaps(cc);   
        }
        else {
            for (int i = 0; i < args.length; i++) {
                CtClass cc = ClassPool.getDefault().get(getClassName(args[i]));
                System.out.println(cc.getName());
                rebuildStackMaps2(cc);
                cc.writeFile("rebuild");
            }
        }
    }

    public static String getClassName(String fileName) {
        Matcher m = Pattern.compile("(.*)\\.class").matcher(fileName);
        if (m.matches())
            return m.group(1).replace('/', '.');
        else
            return fileName;
    }

    public void testCommonSuper() throws Exception {
        CtClass type = loader.get("javassist.CtClassType[]");
        CtClass base = loader.get("javassist.CtClass[]");
        CtClass array = loader.get("javassist.CtArray[]");
        CtClass array2 = loader.get("javassist.CtArray[][]");
        CtClass str = loader.get("java.lang.String[]");
        CtClass strObj = loader.get("java.lang.String");
        CtClass obj = loader.get("java.lang.Object[]");
        CtClass objObj = loader.get("java.lang.Object");

        assertEquals(base, TypeData.commonSuperClassEx(type, base));
        assertEquals(base, TypeData.commonSuperClassEx(base, type));
        assertEquals(base, TypeData.commonSuperClassEx(array, type));
        assertEquals(obj, TypeData.commonSuperClassEx(base, str));
        assertEquals(objObj, TypeData.commonSuperClassEx(strObj, str));
        assertEquals(obj, TypeData.commonSuperClassEx(array, array2));
        assertEquals(obj, TypeData.commonSuperClassEx(base, array2));
    }

    public void testRebuild() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$T1");
        rebuildStackMaps2(cc);
        //Class c = cc.toClass();
        //Object t1 = c.getConstructor().newInstance();
        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(3, invoke(t1, "test"));
    }

    public static interface Intf {
        int foo();
    }

    public static class C1 implements Intf {
        public int foo() { return 0; }
    }
    
    public static class C2 implements Intf {
        public int foo() { return 3; }
    }

    public static class T1 {
        public int test() {
            return foo(-1);
        }

        public int foo(int i) {
            Intf obj;
            if (i > 0)
                obj = new C1();
            else
                obj = new C2();

            return obj.foo();
        }
    }

    public void testRebuild2() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$C3");
        rebuildStackMaps2(cc);
        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(7, invoke(t1, "test"));
    }

    public static class C3 {
        int value;
        public C3(int i) {
            value = i;
        }
        public C3(boolean b) {
            this(b ? 7 : 10);
        }
        public C3() { this(true); }
        public int test() { return value; }
    }

    public void testRebuild3() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$T3");
        rebuildStackMaps2(cc);
        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(1100, invoke(t1, "test"));
    }

    public static interface Intf2 {
        int bar();
    }

    public static class D1 extends C1 implements Intf2 {
        public int bar() { return 10; }
    }

    public static class D2 extends C1 implements Intf2 {
        public int bar() { return 100; }
    }

    public static class T3 {
        public int bar(Intf2 i) { return 1000; }

        public int test() {
            return foo(-1);
        }

        public int foo(int i) {
            Intf2 obj;
            if (i > 0)
                obj = new D1();
            else
                obj = new D2();

            System.out.println(obj.toString());
            return obj.bar() + bar(obj);
        }
    }

    public void testRebuildArray() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$T4");
        rebuildStackMaps2(cc);
        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(30, invoke(t1, "test"));
    }

    public static class T4 {
        public int test() {
            return foo(3) + foo2(3) + foo3(3) + foo4(3);
        }

        public int foo(int i) {
            C1[] a;
            if (i > 0)
                a = new D1[1];
            else
                a = new D2[1];

            if (i > 0)
                a[0] = new D1();
            else
                a[0] = new D2();

            return a[0].foo();
        }

        public int foo2(int i) {
            Intf2[] a;
            if (i > 0)
                a = new D1[1];
            else
                a = new D2[1];

            if (i > 0)
                a[0] = new D1();
            else
                a[0] = new D2();

            return a[0].bar();
        }

        public int foo3(int i) {
            Intf2[] a = null;
            if (i > 0)
                a = new D1[1];

            a[0] = new D1();
            return a[0].bar();
        }

        public int foo4(int i) {
            Intf2[] a = new Intf2[1];
            if (i > 0)
            	a[0] = new D1();
            return a[0].bar();
        }
    }

    public void testRebuildConstructor() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$T5");
        rebuildStackMaps2(cc);
        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(123, invoke(t1, "test"));
    }

    public static class T5 {
        int count;
        public T5() { count = 0; }
        public T5(int k) {
            if (k > 0) count = 10;
            else count = 100;
            count++;
        }
        public T5(double d) {
            this(d > 0.0 ? 1 : -1);
            if (d > 1.0) count += 10;
            else count += 100;
            count++;
        }
        public int test() {
            return new T5(3).count + new T5(1.0).count;
        }
    }

    public void testRebuildConstructor2() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$T6");
        rebuildStackMaps2(cc);
        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(101, invoke(t1, "test2"));
    }

    public static class T6 {
        public int test2() {
            T5 t0 = new T5();
            T5 t = new T5(t0.count > 0 ? (new T5(t0.count > 0 ? 1 : -1).count) : -1);
            if (t0.count > 0)
                t.count += 1000;
            return t.count;
        }
    }

    public void testSwitchCase() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$T7");
        // CodeConverter conv = new CodeConverter();
        // conv.replaceNew(cc, cc, "make2");
        // cc.instrument(conv);
        StringBuffer sbuf = new StringBuffer("String s;");
        for (int i = 0; i < 130; i++)
            sbuf.append("s =\"" + i + "\";");

        cc.getDeclaredMethod("foo").insertBefore(sbuf.toString());
        cc.getDeclaredMethod("test2").setBody(loader.get("javassist.bytecode.StackMapTest$T8").getDeclaredMethod("test2"), null);
        //rebuildStackMaps2(cc);
        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(110, invoke(t1, "test"));
    }

    public static class T7 {
        int value = 1;
        T7 t7;
        public static T7 make2() { return null; }
        public int foo() { return 1; }
        public int test() { return test2(10); }
        public int test2(int k) { return k; }
    }

    public static class T8 {
        public int test2(int k) {
            String s = "abc";
            T7 t = k > 0 ? new T7() : new T7();
            switch (k) {
            case 0:
                t = new T7();
                k += t.value;
                break;
            case 10:
                k += 100;
                break;
            }
            return k;
        }
    }

    public void testSwitchCase2() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$T7b");
        StringBuffer sbuf = new StringBuffer("String s;");
        for (int i = 0; i < 130; i++)
            sbuf.append("s =\"" + i + "\";");

        cc.getDeclaredMethod("foo").insertBefore(sbuf.toString());
        cc.getDeclaredMethod("test2").setBody(loader.get("javassist.bytecode.StackMapTest$T8b").getDeclaredMethod("test2"), null);
        rebuildStackMaps2(cc);
        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(110, invoke(t1, "test"));
    }

    public static class T7b {
        int value = 1;
        T7b t7;
        public static T7b make2() { return null; }
        public int foo() { return 1; }
        public int test() { return test2(10); }
        public int test2(int k) { return k; }
    }

    public static class T8b {
        public int test2(int k) {
            String s = "abc";
            T7b t = k > 0 ? new T7b() : new T7b();
            switch (k) {
            case 0:
                t = new T7b();
                k += t.value;
                break;
            case 10:
                k += 100;
                break;
            }
            return k;
        }
    }

    public void testSwitchCase3() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$T7c");
        StringBuffer sbuf = new StringBuffer("String s;");
        for (int i = 0; i < 130; i++)
            sbuf.append("s =\"" + i + "\";");

        cc.getDeclaredMethod("foo").insertBefore(sbuf.toString());
        cc.getDeclaredMethod("test2").setBody(loader.get("javassist.bytecode.StackMapTest$T8c").getDeclaredMethod("test2"), null);

        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(100, invoke(t1, "test"));
    }

    public static class T7c {
        int value = 1;
        T7b t7;
        public static T7b make2() { return null; }
        public static void print(String s) {}
        public int foo() { return 1; }
        public int test() { return test2(10); }
        public int test2(int k) { return 0; }
    }

    public static class T8c {
        public int test2(int k) {
            int jj = 50;
            if (k > 0)
                k += "fooo".length();

            int j = 50;
            loop: for (int i = 0; i < 10; i++) {
                int jjj = 1;
                switch (i) {
                case 0:
                    k++;
                    { int poi = 0; }
                    break;
                case 9:
                    break loop;
                case 7:
                    k += jjj;
                    break;
                }
            }
            return j + jj;
        }
    }

    public void testSwitchCase4() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$T8e");
        StringBuffer sbuf = new StringBuffer("String s;");
        for (int i = 0; i < 130; i++)
            sbuf.append("s =\"" + i + "\";");

        cc.getDeclaredMethod("foo").insertBefore(sbuf.toString());
        CtClass orig = loader.get("javassist.bytecode.StackMapTest$T8d");
        CtMethod origM = orig.getDeclaredMethod("test2");
        writeLdcw(origM);
        cc.getDeclaredMethod("test2").setBody(origM, null);

        orig.writeFile();
        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(100, invoke(t1, "test"));
    }

    private void writeLdcw(CtMethod method) {
        CodeAttribute ca = method.getMethodInfo().getCodeAttribute();
        int index = ca.getConstPool().addStringInfo("ldcw");
        CodeIterator ci = ca.iterator();
        ci.writeByte(Opcode.LDC_W, 14);
        ci.write16bit(index, 15);
        ci.writeByte(Opcode.LDC_W, 43);
        ci.write16bit(index, 44);
    }

    public static class T8e {
        static T8dd helper() { return new T8dd(); }
        int integer() { return 9; }
        boolean integer2(String s) { return true; }
        static void print(String s) {}
        private void print2(String s) {}
        private Object func(String s) { return null; }
        I8d func2(String s) { return null; }
        static String ldcw() { return "k"; }
        static boolean debug = false;
        void log(String p1,String p2,String p3,Long p4, Object p5, Object p6) {}

        public int foo() { return 1; }
        public int test() {
            try {
                return test2("", 10) ? 1 : 0;
            } catch (Exception e) {}
            return 100;
        }
        public boolean test2(String s, int k) throws Exception { return false; }
    }

    public static interface I8d {
        String foo(String s, int i);
    }

    public static class T8dd {
        java.util.List foo(String s) { return new java.util.ArrayList(); }
    }

    public static class T8d {
        static T8dd helper() { return new T8dd(); }
        int integer() { return 9; }
        boolean integer2(String s) { return true; }
        static void print(String s) {}
        private void print2(String s) {}
        private Object func(String s) { return null; }
        I8d func2(String s) { return null; }
        static String ldcw() { return "k"; }
        static boolean debug = false;
        void log(String p1,String p2,String p3,Long p4, Object p5, Object p6) {}

        public boolean test2(String s, int i) throws Exception {
            String k = null;
            Object k2 = null;
            boolean v5 = true;
            if (!debug)
                print(ldcw());

            Object k3 = this.func(s);
            if (k3 == null)
                throw new Exception(new StringBuilder().append(ldcw()).append(s).append(",").toString());

            String v7 = k3.toString();
            k2 = this.func2(v7);
            if (k2 != null) {   // 82:
                if (k2 instanceof I8d) {
                    I8d k5 = (I8d)k2;
                    k = k5.foo(s, i);
                }
            }

            java.util.List list = helper().foo(v7);     // local 8
            for (int v9 = 0; v9 < list.size(); v9++) {
                boolean v10 = true;
                T8d v11 = (T8d)list.get(v9);
                switch (v11.integer()) {
                case 1:
                    break;
                case 2:
                    v10 = this.integer2(s);
                    v5 &= v10;
                    break;
                default :
                    throw new Exception(new StringBuilder().append("ldc 189").append(v11.integer()).append("ldc 169").toString());
                }
            }

            if (v5)  // 246:
                this.print2(s);
            if (v5)
                this.log(ldcw(), v7, s, Long.valueOf(Integer.valueOf(i).longValue()), k, null);
            else // 290:
                this.log(ldcw(), v7, s, Long.valueOf(Integer.valueOf(i).longValue()), k, null);

            return v5;
        }
    }

    public void testInnerClass() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$T9");
        CtClass par = loader.get("javassist.bytecode.StackMapTest$T9$Parent");
        CtClass in = loader.get("javassist.bytecode.StackMapTest$T9$In");
        rebuildStackMaps2(cc);
        rebuildStackMaps2(par);
        rebuildStackMaps2(in);
        cc.writeFile();
        in.writeFile();
        par.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(19, invoke(t1, "test"));
    }

    public static class T9 {
        class Parent {
            int f; 
            Parent(int i) { f = i; } 
        }
        class In extends Parent {
            int value;
            public In(int i) {
                super(i > 0 ? 10 : 20);
                value = i;
            }
        }

        public int test() {
            In in = new In(9);
            return in.value + in.f;
        }
    }

    public void testConstructor3() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$C4");
        MethodInfo mi = cc.getDeclaredMethod("foo").getMethodInfo();
        mi.rebuildStackMapForME(loader);
        CodeIterator ci = mi.getCodeAttribute().iterator();
        ci.insertGap(0, 7);
        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(6, invoke(t1, "test"));
    }

    public static class C4 {
        public int test() { return foo(3); }
        public int foo(int i) {
            String s = new String(i > 0 ? "pos" : "negative");
            System.out.println("2nd stage");
            int len = 0;
            if (i > 0) {
                String t =  new String(i > 0 ? "pos" : "negative");
                len = t.length();
            }
            return s.length() + len;
        }
    }

    public void testJIRA175() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$C5");
        cc.getDeclaredMethod("setter").instrument(new javassist.expr.ExprEditor() {
            @Override
            public void edit(javassist.expr.FieldAccess f) throws javassist.CannotCompileException {
                if (!f.where().getMethodInfo().isMethod())
                    return;

                f.replace("{ $_ = $proceed($$); if (false) return $_;}");
            }
        });
        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(3, invoke(t1, "test"));
    }

    public static class C5 {
        String value;
        int ivalue;
        public int test() {
            setter("foo");
            return value.length();
        }

        public void setter(String s) {
            value = s;
            ivalue = s.length();
        }
    }

    public void testJIRA175b() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$C6");
        cc.getDeclaredMethod("setter").instrument(new javassist.expr.ExprEditor() {
            public void edit(javassist.expr.FieldAccess f) throws javassist.CannotCompileException {
                if (!f.where().getMethodInfo().isMethod())
                    return;

                // this will make a 1-byte dead code.
                f.replace("{ $_ = $proceed($$); return $_;}");
            }
        });
        cc.writeFile();
    }

    public static class C6 {
        String value;
        int ivalue;
        public int test() {
            setter("foo");
            return value.length();
        }

        public void setter(String s) {
            value = s;
            ivalue = s.length();
        }
    }

    public void testForHibernate() throws Exception {
        ClassFile cf = loader.makeClass("javassist.bytecode.StackMapTestHibTest").getClassFile();
        ConstPool cp = cf.getConstPool();
        MethodInfo mi = new MethodInfo(cp, "foo", "()V");
        Bytecode code = new Bytecode(cp, 0, 0);
        code.add(Opcode.RETURN);
        CodeAttribute ca = code.toCodeAttribute(); 
        mi.addAttribute(ca);
        cf.addMethod(mi);

        int pc = 111;
        int throwableType_index = cp.addClassInfo(Throwable.class.getName());

        StackMapTable.Writer writer = new StackMapTable.Writer(32);
        int[] localTags = { StackMapTable.OBJECT, StackMapTable.OBJECT, StackMapTable.OBJECT, StackMapTable.INTEGER };
        int[] localData = { cp.getThisClassInfo(), cp.addClassInfo("java/lang/Object"),
                            cp.addClassInfo("[Ljava/lang/Object;"), 0};
        int[] stackTags = { StackMapTable.OBJECT };
        int[] stackData = { throwableType_index };
        writer.fullFrame(pc, localTags, localData, stackTags, stackData);

        ca.setAttribute(writer.toStackMapTable(cp));
        cf.write(new java.io.DataOutputStream(new java.io.FileOutputStream("./test-hibernate.class")));
    }

    public void testCommonSuperclass() throws Exception {
        CtClass obj = loader.get("java.lang.Object");
        CtClass objarray = loader.get("java.lang.Object[]");

        CtClass one = loader.get("byte[]");
        CtClass two = loader.get("byte[][]");
        assertEquals("java.lang.Object", TypeData.commonSuperClassEx(one, two).getName());
        assertTrue(one.subtypeOf(obj));
        assertTrue(two.subtypeOf(obj));
        assertFalse(one.subtypeOf(objarray));
        assertTrue(two.subtypeOf(objarray));

        one = loader.get("int[][]");
        two = loader.get("java.lang.Object[]");
        assertEquals("java.lang.Object[]", TypeData.commonSuperClassEx(one, two).getName());
        assertTrue(one.subtypeOf(objarray));
        assertTrue(two.subtypeOf(objarray));

        one = loader.get("int[]");
        two = loader.get("java.lang.String");
        assertEquals("java.lang.Object", TypeData.commonSuperClassEx(one, two).getName());
        assertTrue(one.subtypeOf(obj));
        assertTrue(two.subtypeOf(obj));
        assertFalse(one.subtypeOf(objarray));
        assertFalse(two.subtypeOf(objarray));

        one = loader.get("int[]");
        two = loader.get("int[]");
        assertEquals("int[]", TypeData.commonSuperClassEx(one, two).getName());
        assertTrue(one.subtypeOf(obj));
        assertFalse(one.subtypeOf(objarray));

        one = loader.get("int[]");
        two = loader.get("java.lang.String[]");
        assertEquals("java.lang.Object", TypeData.commonSuperClassEx(one, two).getName());
        assertTrue(one.subtypeOf(obj));
        assertTrue(two.subtypeOf(obj));
        assertFalse(one.subtypeOf(objarray));
        assertTrue(two.subtypeOf(objarray));

        one = loader.get("java.lang.String[]");
        two = loader.get("java.lang.Class[]");
        assertEquals("java.lang.Object[]", TypeData.commonSuperClassEx(one, two).getName());
        assertTrue(one.subtypeOf(objarray));
        assertTrue(two.subtypeOf(objarray));
    }

    public void testJsr() throws Exception {
        CtClass cc = loader.makeClass("javassist.bytecode.StackMapTestJsrTest");
        ClassFile cf = cc.getClassFile();
        cf.setMajorVersion(ClassFile.JAVA_5);
        ConstPool cp = cf.getConstPool();
        MethodInfo mi = new MethodInfo(cp, "test", "()I");
        mi.setAccessFlags(AccessFlag.PUBLIC);
        Bytecode code = new Bytecode(cp, 1, 3);
        code.addIconst(3);
        code.addIstore(1);
        code.addIload(1);
        code.add(Opcode.IFEQ);
        code.addIndex(6);
        code.add(Opcode.JSR);
        code.addIndex(5);
        code.addIload(1);
        code.add(Opcode.IRETURN);
        code.addAstore(2);
        code.addRet(2);
        CodeAttribute ca = code.toCodeAttribute(); 
        mi.addAttribute(ca);
        mi.rebuildStackMap(loader);
        cf.addMethod(mi);
        cc.addConstructor(CtNewConstructor.make("public StackMapTestJsrTest() {}", cc));
        cc.addMethod(CtMethod.make("public static void main(String[] args) {"
                                 + "new javassist.bytecode.StackMapTestJsrTest().test();"
                                 + "}",
                                   cc));
        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(3, invoke(t1, "test"));
    }

    public void tstCtClassType() throws Exception {
        ClassPool cp = ClassPool.getDefault();
        CtClass cc = cp.get("javassist.CtClassType");
        MethodInfo minfo = getMethodInfo(cc.getClassFile(), "getFields", "(Ljava/util/ArrayList;Ljavassist/CtClass;)V");
        rebuildStackMap(cc, cp, minfo);
    }

    MethodInfo getMethodInfo(ClassFile cf, String name, String desc) {
        List list = cf.getMethods();
        Iterator it = list.iterator();
        while (it.hasNext()) {
            MethodInfo mi = (MethodInfo)it.next();
            if (mi.getName().equals(name) && mi.getDescriptor().equals(desc))
                return mi;
        }

        return null;
    }

    public static class C7 {
        public int value;
        public static int value2;
        public C7() { this(3); }
        public C7(int i) {
            value = i;
        }
    }

    public void testIssue328() throws Exception {
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$C7");
        CtConstructor cons = cc.getDeclaredConstructor(new CtClass[] { CtClass.intType });
        cons.insertBefore("if ($1 < 0) { super(); if (value2 > 0) { value2++; } return; }");
        cc.writeFile();
        Object t1 = make(cc.getName());
    }

    public static class C8 {
        int value = 0;
        int loop = 0;
        int loop2 = 1;
        public void foo(int i) { value += i; }
        public void bar(int i) { value += i; }
        public void foo2(int i) { value += i; }
        public void bar2(int i) { value += i; }
    }

    public static class C9 extends C8 {
        public void foo(int i) {
            while(true) {
                loop--;
                if (loop < 0)
                    break;
            }
            value += i;
        }
        public void bar(int i) {
            value += i;
            for (int k = 0; i < 10; k++)
                loop += k;
        }
        public void foo2(int i) {
            while(true) {
                loop2--;
                if (loop2 < 0)
                    break;
            }
            value += i;
            for (int k = 0; k < 3; k++)
                loop2--;
        }
        public void bar2(int i) {
            value += i;
            for (int k = 0; i < 10; k++)
                loop += k;
        }
        public int run() {
            foo(1);
            bar(10);
            foo2(100);
            bar2(1000);
            return value;
        }
    }

    public void testIssue339() throws Exception {
        CtClass cc0 = loader.get("javassist.bytecode.StackMapTest$C8");
        CtClass cc = loader.get("javassist.bytecode.StackMapTest$C9");

        testIssue339b(cc, cc0, "foo", true);
        testIssue339b(cc, cc0, "bar", true);
        testIssue339b(cc, cc0, "foo2", false);
        testIssue339b(cc, cc0, "bar2", false);

        cc.writeFile();
        Object t1 = make(cc.getName());
        assertEquals(2322, invoke(t1, "run"));
    }

    public void testIssue339b(CtClass cc, CtClass cc0, String name, boolean exclusive) throws Exception {
        Bytecode newCode = new Bytecode(cc.getClassFile().constPool);
        newCode.addAload(0); // Loads 'this'
        newCode.addIload(1); // Loads method param 1 (int)
        newCode.addInvokespecial(cc0.getName(), name, "(I)V");
        CodeAttribute ca = cc.getDeclaredMethod(name).getMethodInfo().getCodeAttribute();
        CodeIterator ci = ca.iterator();
        if (exclusive)
            ci.insertEx(newCode.get());
        else
            ci.insert(newCode.get());
    }

    public void testIssue350() throws Exception {
        byte sameLocals1StackItemFrameExtended = 247 - 256;
        byte sameFrameExtended = 251 - 256;
        byte appendFrame = 252 - 256;
        ConstPool cp = new ConstPool("Test");
        StackMapTable stmt;
        int originalLength;

        stmt = new StackMapTable(cp, new byte[] {
            0, 1,
            sameLocals1StackItemFrameExtended, 0, 63, 1
        });
        originalLength = stmt.info.length;
        assertEquals(63, stmt.info[4]);
        stmt.shiftPc(0, 2, false);
        assertEquals(originalLength, stmt.info.length);
        assertEquals(65, stmt.info[4]);

        stmt = new StackMapTable(cp, new byte[] {
            0, 1,
            sameFrameExtended, 0, 63
        });
        originalLength = stmt.info.length;
        assertEquals(63, stmt.info[4]);
        stmt.shiftPc(0, 2, false);
        assertEquals(originalLength, stmt.info.length);
        assertEquals(65, stmt.info[4]);

        stmt = new StackMapTable(cp, new byte[] {
            0, 2,
            sameLocals1StackItemFrameExtended, 0, 63, 1,
            sameFrameExtended, 0, 63
        });
        originalLength = stmt.info.length;
        assertEquals(63, stmt.info[4]);
        assertEquals(63, stmt.info[8]);
        stmt.shiftPc(0, 2, false);
        assertEquals(originalLength, stmt.info.length);
        assertEquals(65, stmt.info[4]);
        assertEquals(63, stmt.info[8]);
        stmt.shiftPc(100, 2, false);
        assertEquals(65, stmt.info[4]);
        assertEquals(65, stmt.info[8]);

        // Actual StackMapTable reported in https://github.com/jboss-javassist/javassist/issues/350.
        stmt = new StackMapTable(cp, new byte[] {
            0, 7, // size
            sameLocals1StackItemFrameExtended, 0, 76, 7, 2, 206 - 256,
            sameLocals1StackItemFrameExtended, 0, 63, 7, 2, 221 - 256,
            appendFrame, 0, 63, 7, 0, 14,
            appendFrame, 0, 43, 7, 2, 225 - 256, 1,
            74, 7, 0, 19,  // same_locals_1_stack_item_frame (not extended)
            appendFrame, 0, 23, 7, 0, 19,
            66, 7, 2, 225 - 256 // same_locals_1_stack_item_frame (not extended)
        });
        assertEquals(63, stmt.info[10]);
        originalLength = stmt.info.length;

        stmt.shiftPc(100, 2, false);
        assertEquals(originalLength, stmt.info.length);
        assertEquals(65, stmt.info[10]);
    }

    public static void dump(byte[] content) {
        final int bytesPerLine = 16;
        for (int i = 0; i < content.length; i += bytesPerLine) {
            for (int j = 0; j < bytesPerLine && i + j < content.length; j++) {
                int unsignedByte = content[i + j];
                if (unsignedByte < 0) {
                    unsignedByte = 256 + unsignedByte;
                }
                System.out.print(unsignedByte + ", ");
            }
            System.out.println();
        }
    }
}
