package test;

import javassist.*;

public class Test {
    public static void main(String[] args) throws Exception {
        CtClass ctClass = ClassPool.getDefault().get("JavassistTarget");
        ctClass.getMethod("method", "(Ljava/lang/String;)V").insertAfter("");
    }
}
