package test4;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class JIRA195 {
    public int run() { return test(3); }

    public int test(int i) {
        try {}
        catch (Throwable t) {}
        finally {
            i = incByOne(i);
        }

        return i;
    }

    private int incByOne(int i) {
        return i + 1;
    }

    public static void main(String[] args) throws Exception {
        ClassPool cp = new ClassPool();
        cp.appendClassPath("./target/test-classes");
        CtClass cc = cp.get("test4.JIRA195");
        CtMethod mth = cc.getDeclaredMethod("test");
        mth.getMethodInfo().rebuildStackMap(cc.getClassPool());
    }
}
