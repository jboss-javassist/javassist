package test.javassist.bytecode.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.bytecode.analysis.Analyzer;
import javassist.bytecode.analysis.Frame;
import javassist.bytecode.analysis.Type;
import junit.framework.TestCase;

/**
 * Tests Analyzer
 *
 * @author Jason T. Greene
 */
public class AnalyzerTest extends TestCase {

    public void testCommonSupperArray() throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass clazz = pool.get(getClass().getName() + "$Dummy");
        CtMethod method = clazz.getDeclaredMethod("commonSuperArray");
        verifyArrayLoad(clazz, method, "java.lang.Number");
    }

    public  void testCommonInterfaceArray() throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass clazz = pool.get(getClass().getName() + "$Dummy");
        CtMethod method = clazz.getDeclaredMethod("commonInterfaceArray");
        verifyArrayLoad(clazz, method, "java.io.Serializable");
    }

    public  void testSharedInterfaceAndSuperClass() throws Exception {
        CtMethod method = ClassPool.getDefault().getMethod(
                getClass().getName() + "$Dummy", "sharedInterfaceAndSuperClass");
        verifyReturn(method, "java.io.Serializable");

        method = ClassPool.getDefault().getMethod(
                getClass().getName() + "$Dummy", "sharedOffsetInterfaceAndSuperClass");
        verifyReturn(method, "java.io.Serializable");

        method = ClassPool.getDefault().getMethod(
                getClass().getName() + "$Dummy", "sharedSuperWithSharedInterface");
        verifyReturn(method, getClass().getName() + "$Dummy$A");
    }

    public  void testArrayDifferentDims() throws Exception {
        CtMethod method = ClassPool.getDefault().getMethod(
                getClass().getName() + "$Dummy", "arrayDifferentDimensions1");
        verifyReturn(method, "java.lang.Cloneable[]");

        method = ClassPool.getDefault().getMethod(
                getClass().getName() + "$Dummy", "arrayDifferentDimensions2");
        verifyReturn(method, "java.lang.Object[][]");
    }

    public  void testReusedLocalMerge() throws Exception {
        CtMethod method = ClassPool.getDefault().getMethod(
                getClass().getName() + "$Dummy", "reusedLocalMerge");

        MethodInfo info = method.getMethodInfo2();
        Analyzer analyzer = new Analyzer();
        Frame[] frames = analyzer.analyze(method.getDeclaringClass(), info);
        assertNotNull(frames);
        int pos = findOpcode(info, Opcode.RETURN);
        Frame frame = frames[pos];
        assertEquals("java.lang.Object", frame.getLocal(2).getCtClass().getName());
    }

    private static int findOpcode(MethodInfo info, int opcode) throws BadBytecode {
        CodeIterator iter = info.getCodeAttribute().iterator();

        // find return
        int pos = 0;
        while (iter.hasNext()) {
            pos = iter.next();
            if (iter.byteAt(pos) == opcode)
                break;
        }
        return pos;
    }


    private static void verifyReturn(CtMethod method, String expected) throws BadBytecode {
        MethodInfo info = method.getMethodInfo2();
        CodeIterator iter = info.getCodeAttribute().iterator();

        // find areturn
        int pos = 0;
        while (iter.hasNext()) {
            pos = iter.next();
            if (iter.byteAt(pos) == Opcode.ARETURN)
                break;
        }

        Analyzer analyzer = new Analyzer();
        Frame[] frames = analyzer.analyze(method.getDeclaringClass(), info);
        assertNotNull(frames);
        Frame frame = frames[pos];
        assertEquals(expected, frame.peek().getCtClass().getName());
    }

    private static void verifyArrayLoad(CtClass clazz, CtMethod method, String component)
            throws BadBytecode {
        MethodInfo info = method.getMethodInfo2();
        CodeIterator iter = info.getCodeAttribute().iterator();

        // find aaload
        int pos = 0;
        while (iter.hasNext()) {
            pos = iter.next();
            if (iter.byteAt(pos) == Opcode.AALOAD)
                break;
        }

        Analyzer analyzer = new Analyzer();
        Frame[] frames = analyzer.analyze(clazz, info);
        assertNotNull(frames);
        Frame frame = frames[pos];
        assertNotNull(frame);

        Type type = frame.getStack(frame.getTopIndex() - 1);
        assertEquals(component + "[]", type.getCtClass().getName());

        pos = iter.next();
        frame = frames[pos];
        assertNotNull(frame);

        type = frame.getStack(frame.getTopIndex());
        assertEquals(component, type.getCtClass().getName());
    }

    private static void addJump(Bytecode code, int opcode, int pos) {
        int current = code.currentPc();
        code.addOpcode(opcode);
        code.addIndex(pos - current);
    }

    public void testDeadCode() throws Exception {
        CtMethod method = generateDeadCode(ClassPool.getDefault());
        Analyzer analyzer = new Analyzer();
        Frame[] frames = analyzer.analyze(method.getDeclaringClass(), method.getMethodInfo2());
        assertNotNull(frames);
        assertNull(frames[4]);
        assertNotNull(frames[5]);
        verifyReturn(method, "java.lang.String");
    }

    public void testInvalidCode() throws Exception {
        CtMethod method = generateInvalidCode(ClassPool.getDefault());
        Analyzer analyzer = new Analyzer();
        try {
            analyzer.analyze(method.getDeclaringClass(), method.getMethodInfo2());
        } catch (BadBytecode e) {
            return;
        }

        fail("Invalid code should have triggered a BadBytecode exception");
    }

    public void testCodeFalloff() throws Exception {
        CtMethod method = generateCodeFalloff(ClassPool.getDefault());
        Analyzer analyzer = new Analyzer();
        try {
            analyzer.analyze(method.getDeclaringClass(), method.getMethodInfo2());
        } catch (BadBytecode e) {
            return;
        }

        fail("Code falloff should have triggered a BadBytecode exception");
    }

    public void testJsrMerge() throws Exception {
        CtMethod method = generateJsrMerge(ClassPool.getDefault());
        Analyzer analyzer = new Analyzer();
        analyzer.analyze(method.getDeclaringClass(), method.getMethodInfo2());
        verifyReturn(method, "java.lang.String");
    }

    public void testJsrMerge2() throws Exception {
        CtMethod method = generateJsrMerge2(ClassPool.getDefault());
        Analyzer analyzer = new Analyzer();
        analyzer.analyze(method.getDeclaringClass(), method.getMethodInfo2());
        verifyReturn(method, "java.lang.String");
    }

    private CtMethod generateDeadCode(ClassPool pool) throws Exception {
        CtClass clazz = pool.makeClass(getClass().getName() + "$Generated0");
        CtClass stringClass = pool.get("java.lang.String");
        CtMethod method = new CtMethod(stringClass, "foo", new CtClass[0], clazz);
        MethodInfo info = method.getMethodInfo2();
        info.setAccessFlags(AccessFlag.PUBLIC | AccessFlag.STATIC);
        Bytecode code = new Bytecode(info.getConstPool(), 1, 2);
        /* 0 */ code.addIconst(1);
        /* 1 */ addJump(code, Opcode.GOTO, 5);
        /* 4 */ code.addIconst(0); // DEAD
        /* 5 */ code.addIconst(1);
        /* 6 */ code.addInvokestatic(stringClass, "valueOf", stringClass, new CtClass[]{CtClass.intType});
        /* 9 */ code.addOpcode(Opcode.ARETURN);
        info.setCodeAttribute(code.toCodeAttribute());
        clazz.addMethod(method);

        return method;
    }

    private CtMethod generateInvalidCode(ClassPool pool) throws Exception {
        CtClass clazz = pool.makeClass(getClass().getName() + "$Generated4");
        CtClass intClass = pool.get("java.lang.Integer");
        CtClass stringClass = pool.get("java.lang.String");
        CtMethod method = new CtMethod(stringClass, "foo", new CtClass[0], clazz);
        MethodInfo info = method.getMethodInfo2();
        info.setAccessFlags(AccessFlag.PUBLIC | AccessFlag.STATIC);
        Bytecode code = new Bytecode(info.getConstPool(), 1, 2);
        /* 0 */ code.addIconst(1);
        /* 1 */ code.addInvokestatic(intClass, "valueOf", intClass, new CtClass[]{CtClass.intType});
        /* 4 */ code.addOpcode(Opcode.ARETURN);
        info.setCodeAttribute(code.toCodeAttribute());
        clazz.addMethod(method);

        return method;
    }


    private CtMethod generateCodeFalloff(ClassPool pool) throws Exception {
        CtClass clazz = pool.makeClass(getClass().getName() + "$Generated3");
        CtClass stringClass = pool.get("java.lang.String");
        CtMethod method = new CtMethod(stringClass, "foo", new CtClass[0], clazz);
        MethodInfo info = method.getMethodInfo2();
        info.setAccessFlags(AccessFlag.PUBLIC | AccessFlag.STATIC);
        Bytecode code = new Bytecode(info.getConstPool(), 1, 2);
        /* 0 */ code.addIconst(1);
        /* 1 */ code.addInvokestatic(stringClass, "valueOf", stringClass, new CtClass[]{CtClass.intType});
        info.setCodeAttribute(code.toCodeAttribute());
        clazz.addMethod(method);

        return method;
    }

    private CtMethod generateJsrMerge(ClassPool pool) throws Exception {
        CtClass clazz = pool.makeClass(getClass().getName() + "$Generated1");
        CtClass stringClass = pool.get("java.lang.String");
        CtMethod method = new CtMethod(stringClass, "foo", new CtClass[0], clazz);
        MethodInfo info = method.getMethodInfo2();
        info.setAccessFlags(AccessFlag.PUBLIC | AccessFlag.STATIC);
        Bytecode code = new Bytecode(info.getConstPool(), 1, 2);
        /* 0 */ code.addIconst(5);
        /* 1 */ code.addIstore(0);
        /* 2 */ addJump(code, Opcode.JSR, 7);
        /* 5 */ code.addAload(0);
        /* 6 */ code.addOpcode(Opcode.ARETURN);
        /* 7 */ code.addAstore(1);
        /* 8 */ code.addIconst(3);
        /* 9 */ code.addInvokestatic(stringClass, "valueOf", stringClass, new CtClass[]{CtClass.intType});
        /* 12 */ code.addAstore(0);
        /* 12 */ code.addRet(1);
        info.setCodeAttribute(code.toCodeAttribute());
        clazz.addMethod(method);
        //System.out.println(clazz.toClass().getMethod("foo", new Class[0]).invoke(null, new Object[0]));

        return method;
    }

    private CtMethod generateJsrMerge2(ClassPool pool) throws Exception {
        CtClass clazz = pool.makeClass(getClass().getName() + "$Generated2");
        CtClass stringClass = pool.get("java.lang.String");
        CtMethod method = new CtMethod(stringClass, "foo", new CtClass[0], clazz);
        MethodInfo info = method.getMethodInfo2();
        info.setAccessFlags(AccessFlag.PUBLIC | AccessFlag.STATIC);
        Bytecode code = new Bytecode(info.getConstPool(), 1, 2);
        /* 0 */ addJump(code, Opcode.JSR, 5);
        /* 3 */ code.addAload(0);
        /* 4 */ code.addOpcode(Opcode.ARETURN);
        /* 5 */ code.addAstore(1);
        /* 6 */ code.addIconst(4);
        /* 7 */ code.addInvokestatic(stringClass, "valueOf", stringClass, new CtClass[]{CtClass.intType});
        /* 10 */ code.addAstore(0);
        /* 11 */ code.addRet(1);
        info.setCodeAttribute(code.toCodeAttribute());
        clazz.addMethod(method);

        return method;
    }

    public static class Dummy {
        public Serializable commonSuperArray(int x) {
            Number[] n;

            if (x > 5) {
                n = new Long[10];
            } else {
                n = new Double[5];
            }

            return n[x];
        }

        public Serializable commonInterfaceArray(int x) {
            Serializable[] n;

            if (x > 5) {
                n = new Long[10];
            } else if (x > 3) {
                n = new Double[5];
            } else {
                n = new String[3];
            }

            return n[x];
        }


        public static class A {};
        public static class B1 extends A implements Serializable {};
        public static class B2 extends A implements Serializable {};
        public static class A2 implements Serializable, Cloneable {};
        public static class A3 implements Serializable, Cloneable {};

        public static class B3 extends A {};
        public static class C31 extends B3 implements Serializable {};


        public void dummy(Serializable s) {}

        public Object sharedInterfaceAndSuperClass(int x) {
            Serializable s;

            if (x > 5) {
                s = new B1();
            } else {
                s = new B2();
            }

            dummy(s);

            return s;
        }

        public A sharedSuperWithSharedInterface(int x) {
            A a;

            if (x > 5) {
                a = new B1();
            } else if (x > 3) {
                a = new B2();
            } else {
                a = new C31();
            }

            return a;
        }


        public void reusedLocalMerge() {
             ArrayList list = new ArrayList();
             try {
               Iterator i = list.iterator();
               i.hasNext();
             } catch (Exception e) {
             }
        }

        public Object sharedOffsetInterfaceAndSuperClass(int x) {
            Serializable s;

            if (x > 5) {
                s = new B1();
            } else {
                s = new C31();
            }

            dummy(s);

            return s;
        }


        public Object arrayDifferentDimensions1(int x) {
            Object[] n;

            if ( x > 5) {
                n = new Number[1][1];
            } else {
                n = new Cloneable[1];
            }


            return n;
        }

        public Object arrayDifferentDimensions2(int x) {
            Object[] n;

            if ( x> 5) {
                n = new String[1][1];
            } else {
                n = new Number[1][1][1][1];
            }

            return n;
        }
    }
}
