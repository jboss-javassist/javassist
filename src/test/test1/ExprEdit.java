package test1;

public class ExprEdit {
    public int k1(int j) { return j; }

    public static int k2(int j) { return j; }

    public void k3(int j) { System.out.println("k3: " + j); }

    public int k0() { k3(1); return k1(3) + k2(7); }
}
