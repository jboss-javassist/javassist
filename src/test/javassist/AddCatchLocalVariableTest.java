package javassist;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javassist.bytecode.*;
import junit.framework.TestCase;

/** Exercises initialized local variables in opt-in catch clauses. */
public class AddCatchLocalVariableTest extends TestCase {
    private static final String HOOK = Hooks.class.getName();
    private ClassPool pool;
    private CtClass target;

    @Override protected void setUp() {
        Hooks.ENTERED.clear();
        Hooks.EXITED.clear();
        Hooks.FAIL_BEFORE = false;
        Hooks.RETURN_NULL = false;
        pool = new ClassPool(true);
        pool.insertClassPath(new ClassClassPath(AddCatchLocalVariableTest.class));
        target = pool.makeClass("testcatch.LocalCatchTarget");
        target.getClassFile().setMajorVersion(ClassFile.JAVA_8);
    }

    public void testStandardAddCatchCannotResolveAddedLocal() throws Exception {
        CtMethod method = method("public static void run() {}");
        method.addLocalVariable("before", pool.get("java.lang.Object"));
        method.insertBefore("before = new Object();");
        try {
            method.addCatch("{ " + HOOK + ".exit(\"test\", before); throw $e; }", pool.get("java.lang.Throwable"));
            fail("The existing overload must retain its local variable visibility");
        } catch (CannotCompileException expected) {
            assertTrue(expected.getMessage().contains("before"));
        }
    }

    public void testCatchReadsExactBeforeAndRethrowsOriginalError() throws Exception {
        CtMethod method = method("public static void run(Throwable error) throws Throwable { throw error; }");
        weave(method, "one");
        Class<?> type = load();
        Throwable error = new AssertionError("original error");
        assertSame(error, invokeFailure(type, "run", new Class<?>[] {Throwable.class}, new Object[] {error}));
        assertEquals(1, Hooks.ENTERED.size());
        assertSame(Hooks.ENTERED.get(0), Hooks.EXITED.get(0));
    }

    public void testNormalReturnsAndVoidPreserveBefore() throws Exception {
        String[] methods = {
            "public static int integer(int n) { if(n > 0) return n; return -1; }",
            "public static long wide(long n) { return n; }",
            "public static double decimal(double n) { return n; }",
            "public static Object reference(Object value) { return value; }",
            "public static void empty() {}"
        };
        for (String source : methods) weave(method(source), "normal");
        Class<?> type = load();
        assertEquals(7, type.getMethod("integer", int.class).invoke(null, 7));
        assertEquals(-1, type.getMethod("integer", int.class).invoke(null, -7));
        assertEquals(9000000000L, type.getMethod("wide", long.class).invoke(null, 9000000000L));
        assertEquals(2.5d, type.getMethod("decimal", double.class).invoke(null, 2.5d));
        Object value = new Object();
        assertSame(value, type.getMethod("reference", Object.class).invoke(null, value));
        assertNull(type.getMethod("empty").invoke(null));
        assertEquals(Hooks.ENTERED, Hooks.EXITED);
    }

    public void testRecursionUsesIndependentJavaFrames() throws Exception {
        CtMethod method = method("public static void run(int depth) { if(depth > 0) run(depth - 1); else throw new IllegalStateException(); }");
        weave(method, "recursive");
        invokeFailure(load(), "run", new Class<?>[] {int.class}, new Object[] {150});
        assertEquals(151, Hooks.ENTERED.size());
        assertEquals(151, Hooks.EXITED.size());
        for (int i = 0; i < 151; i++) assertSame(Hooks.ENTERED.get(i), Hooks.EXITED.get(150 - i));
    }

    public void testNullBeforeAndEarlyBeforeFailureAreVerifierSafe() throws Exception {
        weave(method("public static void run() {}"), "early");
        Class<?> type = load();
        Hooks.RETURN_NULL = true;
        type.getMethod("run").invoke(null);
        assertNull(Hooks.EXITED.get(0));
        Hooks.RETURN_NULL = false;
        Hooks.FAIL_BEFORE = true;
        Throwable error = invokeFailure(type, "run", new Class<?>[0], new Object[0]);
        assertEquals("before failed", error.getMessage());
        assertNull(Hooks.EXITED.get(1));
    }

    public void testMultiplePluginsKeepSeparateSlotsOnBothExits() throws Exception {
        CtMethod method = method("public static void run(boolean fail) { if(fail) throw new IllegalArgumentException(); }");
        weave(method, "inner");
        weave(method, "outer");
        Class<?> type = load();
        type.getMethod("run", boolean.class).invoke(null, false);
        invokeFailure(type, "run", new Class<?>[] {boolean.class}, new Object[] {true});
        assertEquals(4, Hooks.ENTERED.size());
        assertEquals(4, Hooks.EXITED.size());
        for (int i = 0; i < 4; i += 2) {
            assertSame(Hooks.ENTERED.get(i), Hooks.EXITED.get(i + 1));
            assertSame(Hooks.ENTERED.get(i + 1), Hooks.EXITED.get(i));
        }
    }

    public void testConstructorDelegationAndBodyFailureKeepSeparateBefore() throws Exception {
        CtConstructor root = CtNewConstructor.make("public LocalCatchTarget(int n) { if(n < 0) throw new IllegalArgumentException(); }", target);
        target.addConstructor(root);
        CtConstructor delegating = CtNewConstructor.make("public LocalCatchTarget() { this(1); }", target);
        target.addConstructor(delegating);
        weave(root, "root");
        weave(delegating, "delegating");
        Class<?> type = load();
        type.getConstructor().newInstance();
        assertSame(Hooks.ENTERED.get(0), Hooks.EXITED.get(1));
        assertSame(Hooks.ENTERED.get(1), Hooks.EXITED.get(0));
        try {
            type.getConstructor(int.class).newInstance(-1);
            fail("constructor exception must propagate");
        } catch (InvocationTargetException expected) {
            assertTrue(expected.getCause() instanceof IllegalArgumentException);
        }
        assertSame(Hooks.ENTERED.get(2), Hooks.EXITED.get(2));
    }

    public void testWideLocalSlotWorksWithoutOriginalDebugInfo() throws Exception {
        CtMethod method = method("public static void run() { throw new IllegalStateException(); }");
        CodeAttribute code = method.getMethodInfo().getCodeAttribute();
        code.getAttributes().clear();
        code.setMaxLocals(300);
        weave(method, "wide-slot");
        invokeFailure(load(), "run", new Class<?>[0], new Object[0]);
        assertSame(Hooks.ENTERED.get(0), Hooks.EXITED.get(0));
    }

    public void testExistingCatchFinallyAndSynchronizedMethodRemainIntact() throws Exception {
        CtMethod method = method("public static synchronized int run(int value) { try { if(value == 0) throw new IllegalArgumentException(); if(value < 0) throw new IllegalStateException(); return value; } catch(IllegalArgumentException e) { return 17; } finally { int ignored = 1; } }");
        weave(method, "existing-handlers");
        Class<?> type = load();
        assertEquals(17, type.getMethod("run", int.class).invoke(null, 0));
        assertEquals(3, type.getMethod("run", int.class).invoke(null, 3));
        assertTrue(invokeFailure(type, "run", new Class<?>[] {int.class}, new Object[] {-1}) instanceof IllegalStateException);
        assertEquals(Hooks.ENTERED, Hooks.EXITED);
    }

    public void testStaticInitializerCanReadBeforeOnExceptionalExit() throws Exception {
        CtConstructor initializer = target.makeClassInitializer();
        initializer.setBody("{ if(System.nanoTime() != 0) throw new IllegalStateException(); }");
        weave(initializer, "initializer");
        Class<?> type = load();
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
            fail("initializer exception must propagate");
        } catch (ExceptionInInitializerError expected) {
            assertTrue(expected.getCause() instanceof IllegalStateException);
        }
        assertEquals(1, Hooks.ENTERED.size());
        assertSame(Hooks.ENTERED.get(0), Hooks.EXITED.get(0));
    }

    public void testConstructorFailureBeforeSuperReturnsKeepsOfficialCatchBoundary() throws Exception {
        CtClass base = pool.makeClass("testcatch.ThrowingBase");
        base.addConstructor(CtNewConstructor.make("public ThrowingBase() { throw new IllegalStateException(); }", base));
        target.setSuperclass(base);
        CtConstructor constructor = CtNewConstructor.make("public LocalCatchTarget() { super(); }", target);
        target.addConstructor(constructor);
        weave(constructor, "super-failure");
        final byte[] parentBytes = base.toBytecode();
        final byte[] childBytes = target.toBytecode();
        Class<?> type = new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() {
                defineClass("testcatch.ThrowingBase", parentBytes, 0, parentBytes.length);
                return defineClass("testcatch.LocalCatchTarget", childBytes, 0, childBytes.length);
            }
        }.define();
        try {
            type.getConstructor().newInstance();
            fail("super constructor exception must propagate");
        } catch (InvocationTargetException expected) {
            assertTrue(expected.getCause() instanceof IllegalStateException);
        }
        assertEquals(1, Hooks.ENTERED.size());
        // Retain the existing boundary: no callback before super returns.
        assertTrue(Hooks.EXITED.isEmpty());
    }

    public void testInitializerFailureIsOutsideHandler() throws Exception {
        CtMethod method = method("public static void run() {}");
        method.addLocalVariable("context", pool.get("java.lang.Object"));
        method.addCatch("{ " + HOOK + ".exit(\"unused\", context); throw failure; }",
                        pool.get("java.lang.Throwable"), "failure",
                        "{ context = null; throw new IllegalArgumentException(\"initializer failed\"); }");
        Throwable error = invokeFailure(load(), "run", new Class<?>[0], new Object[0]);
        assertEquals("initializer failed", error.getMessage());
        assertTrue(Hooks.EXITED.isEmpty());
    }

    public void testPrimitiveAndArrayLocals() throws Exception {
        CtMethod method = method("public static void run() { throw new IllegalStateException(); }");
        method.addLocalVariable("number", CtClass.longType);
        method.addLocalVariable("decimal", CtClass.doubleType);
        method.addLocalVariable("values", pool.get("java.lang.String[]"));
        method.addCatch("{ if(number != 42L || decimal != 2.5 || values.length != 2)"
                        + " throw new AssertionError(); throw failure; }",
                        pool.get("java.lang.Throwable"), "failure",
                        "{ number = 42L; decimal = 2.5; values = new String[2]; }");
        assertTrue(invokeFailure(load(), "run", new Class<?>[0], new Object[0]) instanceof IllegalStateException);
    }

    public void testInitializerAndBodySwitchAlignment() throws Exception {
        CtMethod method = method("public static void run(int n) { switch(n) { case 0: return; case 1: throw new IllegalArgumentException(); default: throw new IllegalStateException(); } }");
        method.addLocalVariable("context", pool.get("java.lang.Object"));
        method.addCatch("{ " + HOOK + ".exit(\"switch\", context); throw $e; }",
                        pool.get("java.lang.Throwable"), "$e",
                        "{ switch($1) { case 0: context = null; break; case 1: context = "
                        + HOOK + ".enter(\"switch\"); break; default: context = null; } }");
        Class<?> type = load();
        type.getMethod("run", int.class).invoke(null, 0);
        assertTrue(invokeFailure(type, "run", new Class<?>[] {int.class}, new Object[] {1}) instanceof IllegalArgumentException);
        assertSame(Hooks.ENTERED.get(0), Hooks.EXITED.get(0));
        assertTrue(invokeFailure(type, "run", new Class<?>[] {int.class}, new Object[] {2}) instanceof IllegalStateException);
        assertNull(Hooks.EXITED.get(1));
    }

    public void testFailedHandlerCompilationIsReportedAfterInPlaceInsertion() throws Exception {
        CtMethod method = method("public static int run() { return 7; }");
        method.addLocalVariable("context", pool.get("java.lang.Object"));
        int originalLength = method.getMethodInfo().getCodeAttribute().getCodeLength();
        try {
            method.addCatch("{ missingSymbol(); throw $e; }",
                    pool.get("java.lang.Throwable"), "$e", "context = null;");
            fail("invalid handler must not compile");
        } catch (CannotCompileException expected) {
            assertNotNull(expected.getCause());
            assertTrue(method.getMethodInfo().getCodeAttribute().getCodeLength() > originalLength);
        }
    }

    public void testFailedInitializerCompilationIsReported() throws Exception {
        CtMethod method = method("public static int run() { return 11; }");
        try {
            method.addCatch("{ throw $e; }", pool.get("java.lang.Throwable"), "$e", "missingSymbol();");
            fail("invalid initializer must not compile");
        } catch (CannotCompileException expected) {
            assertNotNull(expected.getCause());
        }
    }

    public void testFailedStackMapRebuildIsReported() throws Exception {
        CtClass parent = pool.makeClass("testcatch.MissingBase");
        CtClass left = pool.makeClass("testcatch.Left", parent);
        CtClass right = pool.makeClass("testcatch.Right", parent);
        left.addConstructor(CtNewConstructor.defaultConstructor(left));
        right.addConstructor(CtNewConstructor.defaultConstructor(right));
        CtMethod method = method("public static Object run(boolean flag) { Object value; if(flag) value = new testcatch.Left(); else value = new testcatch.Right(); return value; }");
        method.addLocalVariable("context", pool.get("java.lang.Object"));
        parent.detach();
        try {
            method.addCatch("{ throw $e; }", pool.get("java.lang.Throwable"), "$e", "context = null;");
            fail("missing merge type must fail stack-map rebuilding");
        } catch (CannotCompileException expected) {
            assertNotNull(expected.getCause());
        }
    }

    public void testExistingOverloadsStillCatchWithoutDebugInfo() throws Exception {
        CtMethod method = method("public static void run() { throw new IllegalStateException(); }");
        method.getMethodInfo().getCodeAttribute().getAttributes().clear();
        method.addCatch("{ throw $e; }", pool.get("java.lang.Throwable"));
        assertTrue(invokeFailure(load(), "run", new Class<?>[0], new Object[0]) instanceof IllegalStateException);
    }

    public void testInstanceMethodPreservesThis() throws Exception {
        target.addConstructor(CtNewConstructor.defaultConstructor(target));
        CtMethod method = method("public Object run(boolean fail) { if(fail) throw new IllegalStateException(); return this; }");
        weave(method, "instance");
        Class<?> type = load();
        Object instance = type.getConstructor().newInstance();
        assertSame(instance, type.getMethod("run", boolean.class).invoke(instance, false));
        try {
            type.getMethod("run", boolean.class).invoke(instance, true);
            fail("original exception must propagate");
        } catch (InvocationTargetException expected) {
            assertTrue(expected.getCause() instanceof IllegalStateException);
        }
        assertEquals(Hooks.ENTERED, Hooks.EXITED);
    }

    public void testIsolatedLambdaAndMethodReferenceKeepResultsAndExceptions() throws Exception {
        isolatedLambdaTarget();
        weave(target.getDeclaredMethod("lambda"), "lambda");
        weave(target.getDeclaredMethod("reference"), "reference");
        Class<?> type = load();
        assertEquals(8, type.getMethod("lambda", int.class).invoke(null, 7));
        assertEquals(9, type.getMethod("reference", int.class).invoke(null, 8));
        assertTrue(invokeFailure(type, "lambda", new Class<?>[] {int.class}, new Object[] {-1})
                instanceof IllegalArgumentException);
        assertTrue(invokeFailure(type, "reference", new Class<?>[] {int.class}, new Object[] {-1})
                instanceof IllegalArgumentException);
        assertEquals(4, Hooks.ENTERED.size());
        assertEquals(Hooks.ENTERED, Hooks.EXITED);
    }

    private void isolatedLambdaTarget() throws Exception {
        target.detach();
        String resource = "/" + LambdaFixture.class.getName().replace('.', '/') + ".class";
        try (java.io.InputStream input = getClass().getResourceAsStream(resource)) {
            target = pool.makeClass(input);
        }
        // The renamed class exists only in this pool, as with an isolated application class.
        target.setName("testcatch.IsolatedLambdaTarget");
        try {
            ClassPool.getDefault().get(target.getName());
            fail("fixture must be absent from the default pool");
        } catch (NotFoundException expected) {
            assertSame(target, pool.get(target.getName()));
        }
    }

    public static class LambdaFixture {
        public static int lambda(int value) {
            java.util.function.IntUnaryOperator operation = n -> increment(n);
            return operation.applyAsInt(value);
        }
        public static int reference(int value) {
            java.util.function.IntUnaryOperator operation = LambdaFixture::increment;
            return operation.applyAsInt(value);
        }
        private static int increment(int value) {
            if (value < 0) throw new IllegalArgumentException("negative value");
            return value + 1;
        }
    }

    private CtMethod method(String source) throws Exception {
        CtMethod method = CtNewMethod.make(source, target);
        target.addMethod(method);
        return method;
    }

    private void weave(CtBehavior behavior, String id) throws Exception {
        int slot = behavior.getMethodInfo().getCodeAttribute().getMaxLocals();
        String name = "context" + slot;
        behavior.addLocalVariable(name, pool.get("java.lang.Object"));
        behavior.insertBefore(name + " = " + HOOK + ".enter(\"" + id + "\");");
        behavior.insertAfter(HOOK + ".exit(\"" + id + "\", " + name + ");", false);
        behavior.addCatch("{ " + HOOK + ".exit(\"" + id + "\", " + name + "); throw $e; }",
                          pool.get("java.lang.Throwable"), "$e", name + " = null;");
    }

    private Class<?> load() throws Exception {
        final byte[] bytes = target.toBytecode();
        final String name = target.getName();
        return new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() { return defineClass(name, bytes, 0, bytes.length); }
        }.define();
    }

    private Throwable invokeFailure(Class<?> type, String name, Class<?>[] types, Object[] args) throws Exception {
        try {
            type.getMethod(name, types).invoke(null, args);
            throw new AssertionError("expected original exception");
        } catch (InvocationTargetException expected) {
            return expected.getCause();
        }
    }

    /** Records identities without any per-invocation context stack. */
    public static class Hooks {
        static final List<Object> ENTERED = new ArrayList<Object>();
        static final List<Object> EXITED = new ArrayList<Object>();
        static boolean FAIL_BEFORE;
        static boolean RETURN_NULL;
        public static Object enter(String id) {
            if (FAIL_BEFORE) throw new IllegalStateException("before failed");
            Object frame = RETURN_NULL ? null : new Frame(id);
            ENTERED.add(frame);
            return frame;
        }
        public static void exit(String id, Object frame) {
            if (frame != null) assertEquals(id, ((Frame) frame).id);
            EXITED.add(frame);
        }
    }

    private static final class Frame {
        final String id;
        Frame(String id) { this.id = id; }
    }
}
