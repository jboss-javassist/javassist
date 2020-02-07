package javassist;

/**
 * Copyright by TianSheng on 2020/2/8 1:50
 * @author TianSheng
 * @version 1.0.0
 * @since 1.8
 */
public class GetCtClassesTest {

    public static void main(String[] args) throws NotFoundException, ClassNotFoundException {
        ClassPool classPool = new ClassPool(true);
        CtClass[] ctClasses = classPool.getCtClassArray("javassist");
        System.out.println();
    }


}
