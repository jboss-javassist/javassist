package test4;

public class AnnoArg {
    public static @interface AnnoArgAt {
        Class<? extends AnnoArg.A> value();
    }

    public static class A {
        int baz() { return 1; }
    }

    public static class B extends A {
        int baz() { return 2; }
    }

    @AnnoArgAt(B.class)
    public int foo(int i) { return i; }
}
