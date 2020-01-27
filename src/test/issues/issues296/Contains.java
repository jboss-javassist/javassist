package issues.issues296;

import javassist.ClassPool;
import javassist.CtClass;
import org.junit.Test;

/**
 * issues 120 test
 *
 * Create by 2BKeyboard on 2019/12/28 17:43
 */
public class Contains {

    private static ClassPool pool = ClassPool.getDefault();

    /**
     * make a ctClass
     */
    public static byte[] makeClass(String name) throws Exception {

        CtClass ctClass;

        if (pool.contains(name)) {
            ctClass = pool.get(name);
        } else {
            ctClass = pool.makeClass(name);
        }

        //
        // Omitted other code.
        //

        return ctClass.toBytecode();

    }

    public static void main(String[] args) throws Exception {

        //
        // this class path not exist.
        // use ClassPool#makeClass create.
        //
        String classname = "com.javassist.NiuBi";

        makeClass(classname);

        //
        // if 'com.javassist.NiuBi' need update.
        //
        makeClass(classname);

    }

}
