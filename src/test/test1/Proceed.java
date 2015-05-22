package test1;

public class Proceed {
    public int p(int i, int j) { return i + j; }

    public int k1(int j) { return j; }

    private int k2(int j) { return j + 1; }

    private static Proceed another = new Proceed();
}
