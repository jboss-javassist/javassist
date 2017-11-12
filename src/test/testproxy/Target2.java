package testproxy;

import java.io.IOException;

@SuppressWarnings("unused")
public class Target2 {
    private int value;
    public Target2(int i) { value = 1; }
    protected Target2(int i, int j) { value = 2; }
    private Target2(int i, double j) { value = 3; }
    Target2(int i, long k) { value = 4; }

    public int get() { return value; }
    public int foo() throws IOException { return ++value; }
    public int _dfoo() { value += 2; return value; }
    private int _d100() { value += 3; return value; }
    private int _d1003foo() { value += 3; return value; }
}
