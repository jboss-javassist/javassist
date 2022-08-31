package test2;

@SuppressWarnings("unused")
public class Nested2 {
    private int i = 3;
    private double d = 3.0;
    private String s = "OK";
    private int geti() { return i; }
    private static int getj(int k) { return k; }
    public void seti(int i) { this.i = i; }

    public static class Inner {
        public int f() { return 1; }
        public static int g() { return 1; }
    }
}
