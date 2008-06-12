package test.javassist.bytecode.analysis;

import java.io.IOException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Bytecode;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.bytecode.analysis.Subroutine;
import javassist.bytecode.analysis.SubroutineScanner;
import junit.framework.TestCase;

/**
 * Tests Subroutine Scanner
 *
 * @author Jason T. Greene
 */
public class ScannerTest extends TestCase {

    public void testNestedFinally() throws Exception {
        ClassPool pool = ClassPool.getDefault();
        generate(pool);
        CtClass clazz = pool.get("test.ScannerTest$GeneratedTest");
        CtMethod method = clazz.getDeclaredMethod("doit");

        SubroutineScanner scanner = new SubroutineScanner();
        Subroutine[] subs = scanner.scan(method.getMethodInfo2());

        verifySubroutine(subs, 31, 31, new int[]{125, 25});
        verifySubroutine(subs, 32, 31, new int[]{125, 25});
        verifySubroutine(subs, 33, 31, new int[]{125, 25});
        verifySubroutine(subs, 60, 31, new int[]{125, 25});
        verifySubroutine(subs, 61, 31, new int[]{125, 25});
        verifySubroutine(subs, 63, 31, new int[]{125, 25});
        verifySubroutine(subs, 66, 31, new int[]{125, 25});
        verifySubroutine(subs, 69, 31, new int[]{125, 25});
        verifySubroutine(subs, 71, 31, new int[]{125, 25});
        verifySubroutine(subs, 74, 31, new int[]{125, 25});
        verifySubroutine(subs, 76, 31, new int[]{125, 25});
        verifySubroutine(subs, 77, 77, new int[]{111, 71});
        verifySubroutine(subs, 79, 77, new int[]{111, 71});
        verifySubroutine(subs, 80, 77, new int[]{111, 71});
        verifySubroutine(subs, 82, 77, new int[]{111, 71});
        verifySubroutine(subs, 85, 77, new int[]{111, 71});
        verifySubroutine(subs, 88, 77, new int[]{111, 71});
        verifySubroutine(subs, 90, 77, new int[]{111, 71});
        verifySubroutine(subs, 93, 77, new int[]{111, 71});
        verifySubroutine(subs, 95, 77, new int[]{111, 71});
        verifySubroutine(subs, 96, 96, new int[]{106, 90});
        verifySubroutine(subs, 98, 96, new int[]{106, 90});
        verifySubroutine(subs, 99, 96, new int[]{106, 90});
        verifySubroutine(subs, 101, 96, new int[]{106, 90});
        verifySubroutine(subs, 104, 96, new int[]{106, 90});
        verifySubroutine(subs, 106, 77, new int[]{111, 71});
        verifySubroutine(subs, 109, 77, new int[]{111, 71});
        verifySubroutine(subs, 111, 31, new int[]{125, 25});
        verifySubroutine(subs, 114, 31, new int[]{125, 25});
        verifySubroutine(subs, 117, 31, new int[]{125, 25});
        verifySubroutine(subs, 118, 31, new int[]{125, 25});
        verifySubroutine(subs, 120, 31, new int[]{125, 25});
        verifySubroutine(subs, 123, 31, new int[]{125, 25});
    }

    private static void verifySubroutine(Subroutine[] subs, int pos, int start,
            int[] callers) {
        Subroutine sub = subs[pos];
        assertNotNull(sub);
        assertEquals(sub.start(), start);
        for (int i = 0; i < callers.length; i++)
            assertTrue(sub.callers().contains(new Integer(callers[i])));
    }

    private static void generate(ClassPool pool) throws CannotCompileException, IOException, NotFoundException {
        // Generated from eclipse JDK4 compiler:
        // public void doit(int x) {
        //    println("null");
        //    try {
        //        println("try");
        //    } catch (RuntimeException e) {
        //        e.printStackTrace();
        //    } finally {
        //        switch (x) {
        //        default:
        //        case 15:
        //        try {
        //            println("inner-try");
        //        } finally {
        //            try {
        //                println("inner-inner-try");
        //            } finally {
        //                println("inner-finally");
        //            }
        //        }
        //        break;
        //        case 1789:
        //        println("switch -17");
        //        }
        //    }
        //}

        CtClass clazz = pool.makeClass("test.ScannerTest$GeneratedTest");
        CtMethod method = new CtMethod(CtClass.voidType, "doit", new CtClass[] {CtClass.intType}, clazz);
        MethodInfo info = method.getMethodInfo2();
        info.setAccessFlags(AccessFlag.PUBLIC);
        CtClass stringClass = pool.get("java.lang.String");
        Bytecode code = new Bytecode(info.getConstPool(), 2, 9);
        /* 0   */ code.addAload(0);
        /* 1   */ code.addLdc("start");
        /* 3   */ code.addInvokevirtual(clazz, "println", CtClass.voidType, new CtClass[] {stringClass});
        /* 6   */ code.addAload(0);
        /* 7   */ code.addLdc("try");
        /* 9   */ code.addInvokevirtual(clazz, "println", CtClass.voidType, new CtClass[] {stringClass});
        /* 12  */ addJump(code, Opcode.GOTO, 125);
        /* 14  */ code.addAstore(2);
        /* 16  */ code.addAload(2);
        /* 17  */ code.addInvokevirtual("java.lang.Exception", "printStackTrace", "()V");
        /* 20  */ addJump(code, Opcode.GOTO, 125);
        /* 23  */ code.addAstore(4);
        /* 25  */ addJump(code, Opcode.JSR, 31);
        /* 28  */ code.addAload(4);
        /* 30  */ code.addOpcode(Opcode.ATHROW);
        /* 31  */ code.addAstore(3);
        /* 32  */ code.addIload(1);
        int spos = code.currentPc();
        /* 33  */ code.addOpcode(Opcode.LOOKUPSWITCH);
                  code.addIndex(0); // 2 bytes pad - gets us to 36
                  code.add32bit(60 - spos); // default
                  code.add32bit(2); // 2 pairs
                  code.add32bit(15); code.add32bit(60 - spos);
                  code.add32bit(1789); code.add32bit(117 - spos);
        /* 60  */ code.addAload(0);
        /* 61  */ code.addLdc("inner-try");
        /* 63  */ code.addInvokevirtual(clazz, "println", CtClass.voidType, new CtClass[] {stringClass});
        /* 66  */ addJump(code, Opcode.GOTO, 111);
        /* 69  */ code.addAstore(6);
        /* 71  */ addJump(code, Opcode.JSR, 77);
        /* 74  */ code.addAload(6);
        /* 76  */ code.add(Opcode.ATHROW);
        /* 77  */ code.addAstore(5);
        /* 79  */ code.addAload(0);
        /* 80  */ code.addLdc("inner-inner-try");
        /* 82  */ code.addInvokevirtual(clazz, "println", CtClass.voidType, new CtClass[] {stringClass});
        /* 85  */ addJump(code, Opcode.GOTO, 106);
        /* 88  */ code.addAstore(8);
        /* 90  */ addJump(code, Opcode.JSR, 96);
        /* 93  */ code.addAload(8);
        /* 95  */ code.add(Opcode.ATHROW);
        /* 96  */ code.addAstore(7);
        /* 98  */ code.addAload(0);
        /* 99  */ code.addLdc("inner-finally");
        /* 101 */ code.addInvokevirtual(clazz, "println", CtClass.voidType, new CtClass[] {stringClass});
        /* 104 */ code.addRet(7);
        /* 106 */ addJump(code, Opcode.JSR, 96);
        /* 109 */ code.addRet(5);
        /* 111 */ addJump(code, Opcode.JSR, 77);
        /* 114 */ addJump(code, Opcode.GOTO, 123);
        /* 117 */ code.addAload(0);
        /* 118 */ code.addLdc("switch - 1789");
        /* 120 */ code.addInvokevirtual(clazz, "println", CtClass.voidType, new CtClass[] {stringClass});
        /* 123 */ code.addRet(3);
        /* 125 */ addJump(code, Opcode.JSR, 31);
        /* 128 */ code.addOpcode(Opcode.RETURN);
        code.addExceptionHandler(6, 12, 15, "java.lang.RuntimeException");
        code.addExceptionHandler(6, 20, 23, 0);
        code.addExceptionHandler(125, 128, 23, 0);
        code.addExceptionHandler(60, 69, 69, 0);
        code.addExceptionHandler(111, 114, 69, 0);
        code.addExceptionHandler(79, 88, 88, 0);
        code.addExceptionHandler(106, 109, 88, 0);
        info.setCodeAttribute(code.toCodeAttribute());
        clazz.addMethod(method);
        clazz.writeFile("/tmp");
    }

    private static void addJump(Bytecode code, int opcode, int pos) {
        int current = code.currentPc();
        code.addOpcode(opcode);
        code.addIndex(pos - current);
    }
}
