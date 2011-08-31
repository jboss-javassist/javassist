package test4;

public class CodeConv {
    int k = 1;
    public int m1() {
        return k + 10;
    }
    public int m2() {
        return k + 100;
    }
    public static void m3(CodeConv cc) {
        cc.k++;
    }

    public int run() {
        int i = m1() * 1000 + m2();
        return k + i * 10;
    }
}
