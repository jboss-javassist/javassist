package javassist;

import junit.framework.TestCase;

public class LineNumberTest extends TestCase {
    private final ClassPool loader = ClassPool.getDefault();
    private static int classNumber = 0;

    public void testComma() {
        doTestCompile(String.join("\n",
                "public void run() {",
                "    return",
                "}"), "line 3: syntax error near \"  return\n}\"");
    }

    public void testException() {
        doTestRuntime(String.join("\n",
                "public void run() {",
                "    throw new java.lang.RuntimeException();",
                "}"), 0, 5);
    }

    public void testIf() {
        doTestRuntime(String.join("\n",
                "public void run() {",
                "    if (throwException()) {",
                "    }",
                "}"), 1, 5);
    }

    public void testWhile() {
        doTestRuntime(String.join("\n",
                "public void run() {",
                "    while (throwException()) {",
                "    }",
                "}"), 1, 5);
    }

    public void testFor() {
        doTestRuntime(String.join("\n",
                "public void run() {",
                "    for (; throwException(); ) {",
                "        ",
                "    }",
                "}"), 1, 5);
    }

    private void doTestCompile(String src, String msg) {
        CtClass testClass = loader.makeClass("javassist.LineNumberCompileTest" + classNumber++);
        try {
            testClass.addMethod(CtMethod.make(src, testClass));
        } catch (CannotCompileException e) {
            assertEquals(msg, e.getCause().getMessage());
            return;
        }
        fail("should not happen");
    }

    private void doTestRuntime(String src, int stackOffset, int lineNumber) {
        CtClass testClass = loader.makeClass("javassist.LineNumberRuntimeTest" + classNumber++);
        String test = String.join("\n",
                "private boolean throwException() {",
                "    throw new java.lang.RuntimeException();",
                "}");
        try {
            testClass.addInterface(loader.get("java.lang.Runnable"));
            testClass.addMethod(CtMethod.make(test, testClass));
            testClass.addMethod(CtMethod.make(src, testClass));
            Class cls = testClass.toClass(LineNumberTest.class);
            var runnable = (Runnable) cls.getConstructor().newInstance();
            runnable.run();
        } catch (Exception e) {
            var lineNum = e.getStackTrace()[stackOffset].getLineNumber();
            assertEquals("Line number should be right", lineNumber, lineNum);
            return;
        }
        fail("should not happen");
    }
}
