import javassist.*;

public class Test {
    public static void main(String[] args) throws Exception {
        ClassPool cp = ClassPool.getDefault();
        CtClass newClass = cp.makeClass("test4.TestDeadcode");
        addDeadCode(newClass, "public void evaluate5(){ boolean b = !false; b = false && b; b = true && true;"
                            + "  b = true || b; b = b || false; }");

        newClass.debugWriteFile();
        Class<?> cClass = newClass.toClass();
        Object o = cClass.newInstance();
        java.lang.reflect.Method m = cClass.getMethod("evaluate5");
        m.invoke(o);
    }
    private static void addDeadCode(CtClass cc, String meth) throws Exception {
        CtMethod m = CtNewMethod.make(meth, cc);
        cc.addMethod(m);
    }
}
