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
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.JvstTest;
import javassist.Loader;
import javassist.bytecode.stackmap.MapMaker;
import javassist.bytecode.stackmap.TypeData;
import junit.framework.TestCase;

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
        return cloader.loadClass(name).newInstance();
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
        //Object t1 = c.newInstance();
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
}
