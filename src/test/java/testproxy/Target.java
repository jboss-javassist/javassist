package testproxy;

public class Target {
    public void m() {}
    public boolean m(boolean b) { return b; }
    public byte m1(byte b) { return b; }
    public char m2(char c) { return c; }
    public short m3(short s) { return s; }
    public int m(int i) { return i; }
    public long m5(long j) { return j; }
    public float m6(float f) { return f; }
    public double m7(double d) { return d; }
    public String m(String s) { return s; }
    public int[] m7(int[] i) { return i; }
    public String[] m8(String[] s) { return s; }
    public Target m9(int i, Target t, Target[][] t2) { return t; }
}
