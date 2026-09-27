package javassist;

public class ClassPoolConcurrentBench {
    public static void main(String[] args) throws InterruptedException {
        ClassPool parent = new ClassPool(true);
        String method = String.join("\n",
                "public void run() {",
                "    double a = Double.NaN;",
                "    a += Double.NaN;\n".repeat(100),
                "}");
        Thread[] threads = new Thread[16];

        long start = System.nanoTime();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10000; i++) {
                        ClassPool child = new ClassPool(parent);
                        child.childFirstLookup = true;
                        CtClass testClass = child.makeClass("CPConcBench");
                        try {
                            testClass.addMethod(CtMethod.make(method, testClass));
                        } catch (CannotCompileException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
        long end = System.nanoTime();

        System.out.printf("%dms\n", (end - start) / 1000000);
    }
}
