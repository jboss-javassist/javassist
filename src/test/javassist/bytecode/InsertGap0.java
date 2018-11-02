package javassist.bytecode;

import javassist.*;

@SuppressWarnings("unused")
final class Gap0Example {
    public static int counter = 1;

    public Gap0Example() {}

    public int run(int x) { return counter; }

    private static final int INTVALUE = 100000;
    private int i1, i2, i3, i4, i5, i6, i7, i8, i9, i10;
    private int i11, i12, i13, i14, i15, i16, i17, i18, i19, i20;
    private int i21, i22, i23, i24, i25, i26, i27, i28, i29, i30;
    private int i31, i32, i33, i34, i35, i36, i37, i38, i39, i40;

    public void doit() {

        i1 = INTVALUE;
        i2 = INTVALUE;
        i3 = INTVALUE;
        i4 = INTVALUE;
        i5 = INTVALUE;
        i6 = INTVALUE;
        i7 = INTVALUE;
        i8 = INTVALUE;
        i9 = INTVALUE;
        i10 = INTVALUE;
        i11 = INTVALUE;
        i12 = INTVALUE;
        i13 = INTVALUE;
        i14 = INTVALUE;
        i15 = INTVALUE;
        i16 = INTVALUE;
        i17 = INTVALUE;
        i18 = INTVALUE;
        i19 = INTVALUE;
        i20 = INTVALUE;
        i21 = INTVALUE;
        i22 = INTVALUE;
        i23 = INTVALUE;
        i24 = INTVALUE;
        i25 = INTVALUE;
        i26 = INTVALUE;
        i27 = INTVALUE;
        i28 = INTVALUE;
        i29 = INTVALUE;
        i20 = INTVALUE;
        i21 = INTVALUE;
        i22 = INTVALUE;
        i23 = INTVALUE;
        i24 = INTVALUE;
        i25 = INTVALUE;
        i26 = INTVALUE;
        i27 = INTVALUE;
        i28 = INTVALUE;
        i29 = INTVALUE;
        i30 = INTVALUE;
        i31 = INTVALUE;
        i32 = INTVALUE;
        i33 = INTVALUE;
        i34 = INTVALUE;
        i35 = INTVALUE;
        i36 = INTVALUE;
        i37 = INTVALUE;
        i38 = INTVALUE;
        i39 = INTVALUE;
        i40 = INTVALUE;
    }
}

@SuppressWarnings("unused")
final class Gap0Example2 {
    public static int counter = 1;

    public Gap0Example2() {}

    public int run(int x) { return counter; }

    private static final int INTVALUE = 100000;
    private int i1, i2, i3, i4, i5, i6, i7, i8, i9, i10;
    private int i11, i12, i13, i14, i15, i16, i17, i18, i19, i20;
    private int i21, i22, i23, i24, i25, i26, i27, i28, i29, i30;
    private int i31, i32, i33, i34, i35, i36, i37, i38, i39, i40;

    public int run2(int x) {
        switch (x) {
        case 0:
        i1 = INTVALUE;
        i2 = INTVALUE;
        i3 = INTVALUE;
        i4 = INTVALUE;
        i5 = INTVALUE;
        i6 = INTVALUE;
        i7 = INTVALUE;
        break;
        case 100:
        i8 = INTVALUE;
        i9 = INTVALUE;
        i10 = INTVALUE;
        i11 = INTVALUE;
        i12 = INTVALUE;
        i13 = INTVALUE;
        i14 = INTVALUE;
        break;
        default:
        i15 = INTVALUE;
        i16 = INTVALUE;
        i17 = INTVALUE;
        if (x > 0) {
        i18 = INTVALUE;
        i19 = INTVALUE;
        i20 = INTVALUE;
        i21 = INTVALUE;
        i22 = INTVALUE;
        i23 = INTVALUE;
        i24 = INTVALUE;
        }
        i25 = INTVALUE;
        i26 = INTVALUE;
        i27 = INTVALUE;
        i28 = INTVALUE;
        i29 = INTVALUE;
        i20 = INTVALUE;
        i21 = INTVALUE;
        i22 = INTVALUE;
        i23 = INTVALUE;
        i24 = INTVALUE;
        i25 = INTVALUE;
        i26 = INTVALUE;
        i27 = INTVALUE;
        i28 = INTVALUE;
        i29 = INTVALUE;
        i30 = INTVALUE;
        i31 = INTVALUE;
        i32 = INTVALUE;
        i33 = INTVALUE;
        i34 = INTVALUE;
        i35 = INTVALUE;
        i36 = INTVALUE;
        i37 = INTVALUE;
        i38 = INTVALUE;
        i39 = INTVALUE;
        i40 = INTVALUE;
        break;
        }
        switch (x) {
        case 0:
            break;
        default:
            return x + 1;
        }

        return x;
    }
}

@SuppressWarnings({"rawtypes","unchecked","unused"})
public final class InsertGap0 extends JvstTestRoot {
    public InsertGap0(String name) {
        super(name);
    }

    public void testExample() throws Throwable {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("javassist.bytecode.Gap0Example");
        CtMethod[] ms = cc.getDeclaredMethods();
        for (int i = 0; i < ms.length; i++) {
            addMethod(ms[i], cc);
        }

        cc.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
        cc.addField(new CtField(CtClass.intType, "i", cc), "++counter");
        boolean p = cc.stopPruning(true);
        cc.writeFile();
        Class c = cc.toClass(ClassFile.class);
        cc.stopPruning(p);

        Object obj = c.getConstructor().newInstance();
        assertEquals(2, invoke(obj, "run", 0));
    }

    public void testExample2() throws Throwable {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("javassist.bytecode.Gap0Example2");
        CtMethod[] ms = cc.getDeclaredMethods();
        for (int i = 0; i < ms.length; i++) {
            addMethod(ms[i], cc);
        }

        cc.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
        cc.addField(new CtField(CtClass.intType, "i", cc), "++counter");
        boolean p = cc.stopPruning(true);
        cc.writeFile();
        Class c = cc.toClass(ClassFile.class);
        cc.stopPruning(p);

        Object obj = c.getConstructor().newInstance();
        assertEquals(0, invoke(obj, "run2", 0));
    }

    private void addMethod(CtMethod method, CtClass target)
        throws CannotCompileException, NotFoundException {

        CtClass[] ts = method.getParameterTypes();
        CtClass[] newts = new CtClass[ts.length + 1];
        for (int i = 0; i < ts.length; i++) {
            newts[i] = ts[i];
        }
        ClassPool p = method.getDeclaringClass().getClassPool();
        newts[ts.length] = target;

        CtMethod m =
            CtNewMethod.make(
                method.getModifiers(),
                method.getReturnType(),
                method.getName(),
                newts,
                method.getExceptionTypes(),
                null,
                method.getDeclaringClass());

        m.setBody(method, null);

        CodeAttribute ca = m.getMethodInfo().getCodeAttribute();
        ca.setMaxLocals(ca.getMaxLocals() + 1);
        target.addMethod(m);
    }
}
