import java.util.ArrayList;
import java.util.List;
import javassist.*;

class InvalidStackMapFrame {

	public void bytecodeVerifyError1() {
        String[] newLine = new String[10];
        for (int i = 0; i < 5; i++) {
        	String a = newLine[1];
        	newLine[4] = a;
        }
	}

	public void bytecodeVerifyError() {
        // javassist bug : invalid stack map frame
        List<Integer> test = new ArrayList<Integer>();
        String[] newLine = new String[10];
        for (Integer idx : test) {
            // invalid stackMapFrame
            // FRAME FULL [bug_regression_jdk7/javassist/InvalidStackMapFrame java/util/ArrayList java/lang/Object java/util/Iterator T T T I] []
            // java/lang/Object is wrong ->  [Ljava/lang/String; is correct
            String address = newLine[1];
            int tabPos = -1;
            if (tabPos != -1) {
                address = address.substring(tabPos + 1);
            }
            newLine[4] = address;
        }

    }
}

public class Test {
    private static final String INVALID_STACK_MAP_FRAME = "InvalidStackMapFrame";

    public static void main(String[] args) throws Exception {

        // CustomURLClassLoader classLoader = new CustomURLClassLoader(new URL[]{}, Thread.currentThread().getContextClassLoader());

        ClassPool classPool = ClassPool.getDefault();
        // classPool.appendClassPath(new LoaderClassPath(classLoader));

        final CtClass ctClass = classPool.get(INVALID_STACK_MAP_FRAME);
        final CtMethod method = ctClass.getDeclaredMethod("bytecodeVerifyError");
        method.addLocalVariable("test_localVariable", CtClass.intType);
        method.insertBefore("{ test_localVariable = 1; }");
        ctClass.debugWriteFile();
        Class<?> cc = ctClass.toClass();
        System.out.println(cc.getName());
        InvalidStackMapFrame obj = (InvalidStackMapFrame)cc.getDeclaredConstructor().newInstance();
        obj.bytecodeVerifyError();
    }
}
