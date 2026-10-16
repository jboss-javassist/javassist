package javassist;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import javassist.bytecode.NestHostAttribute;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import junit.framework.TestCase;

public class StaticProceedTest extends TestCase {
    private static final String HOST = "test5.NestHostStaticMethod";
    private static final String INNER = HOST + "$Inner";
    private static final String PROBE = "test5.StaticProceedProbe";
    private static final String INTERFACE = "test5.StaticProceedInterface";

    private void replace(CtClass caller, String method, final String statement) throws Exception {
        final int[] replacements = new int[1];
        caller.getDeclaredMethod(method).instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals(HOST) || call.getClassName().equals(INTERFACE)) {
                    call.replace(statement);
                    replacements[0]++;
                }
            }
        });
        assertEquals(1, replacements[0]);
    }

    private Loader runtime(CtClass... classes) throws Exception {
        ClassPool pool = new ClassPool(true);
        for (CtClass type : classes)
            pool.makeClass(new ByteArrayInputStream(type.toBytecode()));
        return new Loader(pool);
    }

    private Object inner(Loader loader) throws Exception {
        Class<?> host = loader.loadClass(HOST);
        return loader.loadClass(INNER).getConstructor(host)
                .newInstance(host.getConstructor().newInstance());
    }

    private int count(Loader loader, String field) throws Exception {
        return loader.loadClass(PROBE).getField(field).getInt(null);
    }

    private void assertHostUnchanged(CtClass host, int methodCount) throws Exception {
        assertFalse(host.isModified());
        assertEquals(methodCount, host.getClassFile2().getMethods().size());
    }

    private void checkRun(String method, String statement, Object expected,
                          int prefixCalls, int argumentCalls, int replacementCalls,
                          int helperCalls, int overloadCalls) throws Exception {
        ClassPool pool = new ClassPool(true);
        CtClass host = pool.get(HOST);
        int methodCount = host.getClassFile2().getMethods().size();
        CtClass caller = pool.get(INNER);
        replace(caller, method, statement);
        Loader loader = runtime(caller);
        Object target = inner(loader);
        assertEquals(expected, target.getClass().getMethod(method).invoke(target));
        assertEquals(prefixCalls, count(loader, "prefixCalls"));
        assertEquals(argumentCalls, count(loader, "argumentCalls"));
        assertEquals(replacementCalls, count(loader, "replacementCalls"));
        assertEquals(helperCalls, count(loader, "helperCalls"));
        assertEquals(overloadCalls, count(loader, "overloadCalls"));
        if (method.equals("runVarargs")) {
            Class<?> probe = loader.loadClass(PROBE);
            assertSame(probe.getField("argumentArray").get(null), probe.getField("seenArray").get(null));
        }
        assertHostUnchanged(host, methodCount);
    }

    public void testUnmodifiedStaticCalls() throws Exception {
        Loader loader = runtime(new ClassPool(true).get(INNER));
        Object target = inner(loader);
        for (String method : new String[] {"run", "runNoArguments", "runWide", "runVoid", "runVarargs"})
            assertEquals(42L, ((Number)target.getClass().getMethod(method).invoke(target)).longValue());
        assertEquals(5, count(loader, "helperCalls"));
        assertEquals(4, count(loader, "argumentCalls"));
        assertEquals(0, count(loader, "prefixCalls"));
    }

    public void testNestPrivateStaticProceed() throws Exception {
        checkRun("run", "{ $_ = $proceed($$); }", Integer.valueOf(42), 0, 1, 0, 1, 0);
    }

    public void testNestPrivateStaticPrefix() throws Exception {
        checkRun("run", "{ " + PROBE + ".prefix(); $_ = $proceed($$); }",
                 Integer.valueOf(42), 1, 1, 0, 1, 0);
    }

    public void testStaticProceedArguments() throws Exception {
        checkRun("run", "{ $_ = $proceed($1 + " + PROBE + ".replacement()); }",
                 Integer.valueOf(42), 0, 1, 1, 1, 0);
        checkRun("run", "{ Object[] values = $args; $args = values; $_ = $proceed($$); }",
                 Integer.valueOf(42), 0, 1, 0, 1, 0);
        checkRun("run", "{ $_ = $proceed((byte)$1); }", Integer.valueOf(42), 0, 1, 0, 1, 0);
        checkRun("run", "{ int first = $proceed($$); $_ = first + $proceed($$); }",
                 Integer.valueOf(84), 0, 1, 0, 2, 0);
    }

    public void testStaticProceedReturnAndVarargs() throws Exception {
        checkRun("runNoArguments", "{ $_ = $proceed($$); }", Integer.valueOf(42), 0, 0, 0, 1, 0);
        checkRun("runWide", "{ $_ = $proceed($$); }", Long.valueOf(42L), 0, 1, 0, 1, 0);
        checkRun("runVoid", "{ " + PROBE + ".prefix(); $_ = $proceed($$); }",
                 Integer.valueOf(42), 1, 1, 0, 1, 0);
        checkRun("runVarargs", "{ $_ = $proceed($1); }", Integer.valueOf(42), 0, 1, 0, 1, 0);
    }

    public void testStaticProceedCanSelectAnotherOverload() throws Exception {
        checkRun("run", "{ $_ = $proceed(\"text\"); }", Integer.valueOf(42), 0, 1, 0, 0, 1);
    }

    public void testStaticProceedRejectsInvalidArguments() throws Exception {
        ClassPool pool = new ClassPool(true);
        CtClass host = pool.get(HOST);
        try {
            replace(pool.get(INNER), "run", "{ $_ = $proceed(new Object()); }");
            fail("An unmatched argument must still fail method lookup");
        }
        catch (CannotCompileException expected) {
            assertTrue(expected.getMessage().contains("not found"));
        }
        assertFalse(host.isModified());
    }

    public void testFrozenPrivateStaticHost() throws Exception {
        ClassPool pool = new ClassPool(true);
        CtClass host = pool.get(HOST);
        int methodCount = host.getClassFile2().getMethods().size();
        host.freeze();
        CtClass caller = pool.get(INNER);
        replace(caller, "run", "{ $_ = $proceed($$); }");
        Loader loader = runtime(caller);
        Object target = inner(loader);
        assertEquals(Integer.valueOf(42), target.getClass().getMethod("run").invoke(target));
        assertEquals(1, count(loader, "helperCalls"));
        assertTrue(host.isFrozen());
        assertHostUnchanged(host, methodCount);
    }

    public void testAlreadyLoadedPrivateStaticHost() throws Exception {
        ClassPool runtimePool = new ClassPool(true);
        Loader loader = new Loader(runtimePool);
        Class<?> loadedHost = loader.loadClass(HOST);
        ClassPool pool = new ClassPool(true);
        CtClass host = pool.get(HOST);
        int methodCount = host.getClassFile2().getMethods().size();
        CtClass caller = pool.get(INNER);
        replace(caller, "run", "{ $_ = $proceed($$); }");
        runtimePool.makeClass(new ByteArrayInputStream(caller.toBytecode()));
        Object target = inner(loader);
        assertSame(loadedHost, loader.loadClass(HOST));
        assertEquals(Integer.valueOf(42), target.getClass().getMethod("run").invoke(target));
        assertEquals(1, count(loader, "helperCalls"));
        assertHostUnchanged(host, methodCount);
    }

    public void testPrivateStaticProceedWithParentPool() throws Exception {
        ClassPool parent = new ClassPool(true);
        CtClass host = parent.get(HOST);
        int methodCount = host.getClassFile2().getMethods().size();
        ClassPool child = new ClassPool(parent);
        child.childFirstLookup = true;
        CtClass caller;
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(INNER.replace('.', '/') + ".class")) {
            assertNotNull(input);
            caller = child.makeClass(input);
        }
        assertSame(host, child.get(HOST));
        assertNotSame(host.getClassPool(), caller.getClassPool());
        replace(caller, "run", "{ $_ = $proceed($$); }");
        Loader loader = runtime(caller);
        Object target = inner(loader);
        assertEquals(Integer.valueOf(42), target.getClass().getMethod("run").invoke(target));
        assertEquals(1, count(loader, "helperCalls"));
        assertHostUnchanged(host, methodCount);
    }

    public void testStaticInterfaceProceed() throws Exception {
        CtClass caller = new ClassPool(true).get(INTERFACE);
        replace(caller, "run", "{ " + PROBE + ".prefix(); $_ = $proceed($$); }");
        Loader loader = runtime(caller);
        assertEquals(Integer.valueOf(42), loader.loadClass(INTERFACE).getMethod("run").invoke(null));
        assertEquals(1, count(loader, "helperCalls"));
        assertEquals(1, count(loader, "prefixCalls"));
        checkRun("runInterface", "{ " + PROBE + ".prefix(); $_ = $proceed($$); }",
                 Integer.valueOf(42), 1, 0, 0, 1, 0);
    }

    public void testStaticProceedDoesNotRepairNestMetadata() throws Exception {
        ClassPool pool = new ClassPool(true);
        CtClass host = pool.get(HOST);
        int methodCount = host.getClassFile2().getMethods().size();
        CtClass caller = pool.get(INNER);
        caller.getClassFile().removeAttribute(NestHostAttribute.tag);
        replace(caller, "run", "{ " + PROBE + ".prefix(); $_ = $proceed($$); }");
        assertNull(caller.getClassFile2().getAttribute(NestHostAttribute.tag));
        Loader loader = runtime(caller);
        Object target = inner(loader);
        try {
            target.getClass().getMethod("run").invoke(target);
            fail("An invalid nest must not gain private access");
        }
        catch (InvocationTargetException expected) {
            assertTrue(expected.getCause() instanceof IllegalAccessError);
        }
        assertEquals(1, count(loader, "prefixCalls"));
        assertEquals(0, count(loader, "helperCalls"));
        assertHostUnchanged(host, methodCount);
    }

    public void testUnusedStaticProceedDoesNotResolveMethod() throws Exception {
        ClassPool pool = new ClassPool(true);
        CtClass host = pool.get(HOST);
        host.removeMethod(host.getDeclaredMethod("helper", new CtClass[] {CtClass.intType}));
        CtClass caller = pool.get(INNER);
        replace(caller, "run", "{ $_ = 42; }");
        Loader loader = runtime(caller);
        Object target = inner(loader);
        assertEquals(Integer.valueOf(42), target.getClass().getMethod("run").invoke(target));
        assertEquals(1, count(loader, "argumentCalls"));
        assertEquals(0, count(loader, "helperCalls"));
    }

    public void testNewPrivateStaticAccessStillUsesAccessor() throws Exception {
        ClassPool pool = new ClassPool(true);
        CtClass host = pool.get(HOST);
        int methodCount = host.getClassFile2().getMethods().size();
        CtClass caller = pool.get(INNER);
        caller.addMethod(CtNewMethod.make("public int added() { return " + HOST + ".helper(41); }", caller));
        assertTrue(host.isModified());
        assertEquals(methodCount + 1, host.getClassFile2().getMethods().size());
        Loader loader = runtime(caller, host);
        Object target = inner(loader);
        assertEquals(Integer.valueOf(42), target.getClass().getMethod("added").invoke(target));
        assertEquals(1, count(loader, "helperCalls"));
    }
}
