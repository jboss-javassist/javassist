package test5;

public class BoolTest {
    static boolean i = false;
    public boolean test() {
        return i;
    }
    public boolean foo(boolean b) { return b; }
    public int run() {
        if (test())
            return 1;
        else
            return 0;
    }
}
