package test1;

public class SetBody {
    int value;
    public int m1(int i, int j) { return i + j; }
    public void m2(int k) {}
    public void m3(int k) {}
    public int run() { m2(3); return m1(3, 4); }
}
