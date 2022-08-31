package javassist;

import javassist.util.HotSwapAgent;
import junit.framework.TestCase;

public class HotswapTest extends TestCase {
    public static void main(String[] args) throws Exception {
        // run java -javaagent:hotswap.jar javassist.HotswapTest
        new HotswapTest(HotswapTest.class.getName()).testHotswap();
    }

    public HotswapTest(String name) {
        super(name);
    }

    public static class Foo {
        public int foo() { return 1; }
    }

    public void testHotswap() throws Exception {
        if (javassist.bytecode.ClassFile.MAJOR_VERSION
            >= javassist.bytecode.ClassFile.JAVA_9)
            return;

        Foo f = new Foo();
        assertEquals(1, f.foo());

        ClassPool cp = ClassPool.getDefault();
        CtClass clazz = cp.get(Foo.class.getName());
        CtMethod m = clazz.getDeclaredMethod("foo");
        clazz.removeMethod(m);
        clazz.addMethod(CtNewMethod.make("public int foo() { return 2; }", clazz));
        HotSwapAgent.redefine(Foo.class, clazz);
        Foo g = new Foo();
        assertEquals(2, g.foo());
        System.out.println("Foo#foo() = " + g.foo());
    }
}
