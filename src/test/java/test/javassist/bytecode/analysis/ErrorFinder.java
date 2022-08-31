package test.javassist.bytecode.analysis;

import java.io.BufferedReader;
import java.io.FileReader;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.analysis.Analyzer;

/**
 * Simple testing tool that verifies class files can be analyzed.
 *
 * @author Jason T. Greene
 */
public class ErrorFinder {

    public static void main(String[] args) throws Exception {
        ClassPool pool = ClassPool.getDefault();

        String className = args[0];
        if (!className.equals("-file")) {
            analyzeClass(pool, className);
            return;
        }

        FileReader reader = new FileReader(args[1]);
        BufferedReader lineReader = new BufferedReader(reader);


        String line = lineReader.readLine();
        while (line != null) {
            analyzeClass(pool, line);
            line = lineReader.readLine();
        }
    }

    private static void analyzeClass(ClassPool pool, String className) {
        try {

            CtClass clazz = pool.get(className);
            CtMethod[] methods = clazz.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++)
                analyzeMethod(clazz, methods[i]);
        } catch (Throwable e) {
            System.out.println("FAIL: CLASS: " + className + " " + e.getClass() + ":" + e.getMessage());
        }
    }

    private static void analyzeMethod(CtClass clazz, CtMethod method) {
        String methodName = clazz.getName() + "." + method.getName() + method.getSignature();
        System.out.println("START: " + methodName);
        Analyzer analyzer = new Analyzer();

        long time = System.currentTimeMillis();
        try {
            analyzer.analyze(clazz, method.getMethodInfo2());
            System.out.println("SUCCESS: " + methodName + " - " + (System.currentTimeMillis() - time));
        } catch (Exception e) {
            System.out.println("FAIL: " + methodName + " - " + (e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
        }
    }
}
