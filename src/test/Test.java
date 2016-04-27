import javassist.*;

public class Test {
    public static void main(String[] args) {
        CtClass badClass = ClassPool.getDefault().makeClass("badClass");
        String src = String.join(System.getProperty("line.separator"),
                "public void eval () {",
                "    if (true) {",
                "        double t=0;",
                "    } else {",
                "        double t=0;",
                "    }",
                "    for (int i=0; i < 2; i++) {",
                "        int a=0;",
                "        int b=0;",
                "        int c=0;",
                "        int d=0;",
                "        if (true) {",
                "            int e = 0;",
                "        }",
                "    }",
                "}");
        System.out.println(src);
        try {
            badClass.addMethod(CtMethod.make(src, badClass));
            badClass.debugWriteFile("./bin");
            Class clazzz = badClass.toClass();
            Object obj = clazzz.newInstance(); // <-- falls here
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void eval () {
        if (true) {
            double t=0;
        } else {
            double t=0;
        }
        for (int i=0; i < 2; i++) {
            int a=0;
            int b=0;
           int c=0;
            int d=0;
            if (true) {
                int e = 0;
            }
        }
    }
}
