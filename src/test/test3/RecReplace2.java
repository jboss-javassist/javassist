package test3;

public class RecReplace2 {
    int value = 3;
    int value2 = 9;
    int foo(int k) { return k + 1; }
    public int run() {
        value2 = 0;
        value = 7;
        value2 += 10;
        return value + value2;
    }
    public int run2() {
        value2 = 0;
        value = 7;
        value2 += 10;
        return value + value2;
    }
}
