package test2;

@SuppressWarnings("unused")
public class Nested3 {
    private int i = 0;
    private int geti() { return i; }

    Nested3(int j) { i = 1; }

    private Nested3() { i = 2; }

    private Nested3(String s) { i = 3; }

    public static class Inner {
        public int g() { return 1; }
    }
}
