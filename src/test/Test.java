import javassist.*;

public class Test {
    public static void main(String[] args) throws Exception {
        ClassPool cp = ClassPool.getDefault();
        // ClassPool cp = new ClassPool();
        cp.insertClassPath("./target/test-classes");
        CtClass cc = cp.get("test4.JIRA207");
        // cc.getClassFile().setMajorVersion(javassist.bytecode.ClassFile.JAVA_4);
        CtMethod cm = cc.getDeclaredMethod("foo");
        cm.insertBefore("throw new Exception();");
        CtMethod cm2 = cc.getDeclaredMethod("run2");
        cm2.insertBefore("throw new Exception();");
        cc.writeFile();
    }
}
