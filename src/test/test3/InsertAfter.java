package test3;

public class InsertAfter {
    int k;

    public InsertAfter() {
        k = 3;
    }

    public int test() {
        foo();
        return k;
    }
    public void foo() {
        ++k;
    }
}
