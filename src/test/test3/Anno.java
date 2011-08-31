package test3;

public @interface Anno {
    char c() default '0';
    boolean bool() default true;
    byte b() default 1;
    short s() default 2;
    int i() default 3;
    long j() default 4L;
    float f() default 5.0F;
    double d() default 6.0;
    String str() default "7";
    Class clazz() default AnnoTest.class;
    Anno2 anno2() default @Anno2();
}
