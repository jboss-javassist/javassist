package sample.evolve;

import javassist.*;

/**
 * DemoLoader is a class loader for running a program including
 * an updatable class.  This simple loader allows only a single
 * class to be updatable.  (Extending it for supporting multiple
 * updatable classes is easy.)
 *
 * To run, type as follows:
 *
 * % java sample.evolve.DemoLoader <port number>
 *
 * Then DemoLoader launches sample.evolve.DemoServer with <port number>.
 * It also translates sample.evolve.WebPage, which sample.evolve.DemoServer
 * uses, so that it is an updable class.
 *
 * Note: JDK 1.2 or later only.
 */
public class DemoLoader {
    private static final int initialVersion = 0;
    private String updatableClassName = null;
    private CtClass updatableClass = null;

    /* Creates a <code>DemoLoader</code> for making class WebPage
     * updatable.  Then it runs main() in sample.evolve.DemoServer.
     */
    public static void main(String[] args) throws Throwable {
        Evolution translator = new Evolution();
        ClassPool cp = ClassPool.getDefault();
        Loader cl = new Loader();
        cl.addTranslator(cp, translator);

        translator.makeUpdatable("sample.evolve.WebPage");
        cl.run("sample.evolve.DemoServer", args);
    }
}
