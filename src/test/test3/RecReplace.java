package test3;

public class RecReplace {
    public int run() {
        double d = 4.0;
        int i = foo(3.0);
        int j = bar(d);
        return i + j;
    }

    public int run2() {
        double d = 4.0;
        int i = foo(3.0);
        int j = bar(d);
        return i + j;
    }

    int foo(double d) { return (int)d; }
    int bar(double d) { return (int)d + 1; }
}
