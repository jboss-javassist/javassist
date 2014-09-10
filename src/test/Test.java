import javassist.*;

public class Test {
    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            new Test().bar(3);
            return;
        }

        ClassPool cp = ClassPool.getDefault();
        CtClass str = cp.get("java.lang.String");
        CtClass cc = cp.get("Test");
        cc.getClassFile().setMajorVersion(javassist.bytecode.ClassFile.JAVA_4);
        CtMethod m = cc.getDeclaredMethod("bar");
        m.addLocalVariable("aVar", str);
        m.insertAfter(" dismiss( aVar );" , true);
        cc.getClassFile().setMajorVersion(javassist.bytecode.ClassFile.JAVA_7);
        m.insertBefore("aVar = initVar();");
        cc.writeFile();
    }

    public void bar(int i) { foo(i); }
    public void foo(int i) { System.out.println(i); }
    public String initVar() { return "init"; }
    public void dismiss(String s) { System.out.println(s); }
}
