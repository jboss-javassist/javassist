package test2;

public class Nested {
    private int i = 3;
    private int geti() { return i; }
    private static int getj(int k) { return k; }
    public void seti(int i) { this.i = i; }

    public class Inner {
        public int geti() { return i; }
        public void foo(Inner2 in) {}
    }

    public class Inner2 {
        public int geti() { return i; }
    }

    public static class Inner3 {
        public int f() { return 1; }
        public static int g() { return 1; }
    }
}
