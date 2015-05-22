package test3;

public class InsParam {
    public int bar(int k) { return k; }

    public int bar2(int k) { return k; }

    public int foo(int i) {
        int k = bar2(i);
        return k;
    }

    public int poi(int i, String s) {
        return i + s.length();
    }
}
