package test2;

public class RemoveCall {
    int k = 0;
    public int bar() throws Exception {
        foo(3);
        return k;
    }

    public void foo(int k) throws Exception {
        k = 1;
    }
}
