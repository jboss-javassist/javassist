package test3;

@SuppressWarnings("unused")
class SuperGetMethods {
    public int f0;
    protected double d0;
    String s0;
    private char c0;

    public void mpub0() {}
    protected void mpro0() {}
    void mpack0() {}
    private void mpri0() {}
}

@SuppressWarnings("unused")
public class GetMethods extends SuperGetMethods {
    public GetMethods(int i) {}
    protected GetMethods(String i, int j) {}
    GetMethods() {}
    private GetMethods(int i, int j) {}

    public int f;
    protected double d;
    String s;
    private char c;

    public void mpub() {}
    protected void mpro() {}
    void mpack() {}
    private void mpri() {}
}
