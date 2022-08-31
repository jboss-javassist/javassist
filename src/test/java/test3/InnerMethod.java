package test3;

public class InnerMethod {
    static int f = 0;
    static int counter = 3;
    private static void test() {}
    static Inner inner = new Inner();

    static class Inner {
        protected static void test() { f = 1; }
    }

    public int foo() {
        test();
        return f;
    }
}
