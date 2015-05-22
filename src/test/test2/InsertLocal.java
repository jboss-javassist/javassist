package test2;

public class InsertLocal {
    public int run() {
        return (int)(foo(4, 3.14, "pai") * 100) + field;
    }

    private int field = 0;

    public double foo(int i, double d, String s) {
        int k;

        for (k = 0; k < i; k++)
            d++;

        return d;
    }

    public int run2() {
        String s = ".";
        int k = 0;
        return k + s.length();
    }

    public int run3() {
        int i = 0;
        int j = field;
        int k = run2();
        InsertLocal obj = new InsertLocal();
        return i;
    }
}
