package test4;

public class VarArgs {
    public int test() {
        return goo(1, 2, 3) + goo(1, "a", "b", "c");
    }

    public int goo(int i, int... k) {
        return k.length;
    }

    public int goo(int i, String... k) {
        return k.length;
    }
}
