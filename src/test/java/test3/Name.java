package test3;

public class Name {
    static int k;
    static { k = 3; } 

    public Name() {}
    public Name(int i) {}
    public Name(Name n) {}
    public Name(Name n, String s) {}

    public void foo() {}
    public void foo2(int i) {}
    public void foo3(String s) {}
    public void foo4(String[] s) {}
    public void foo5(int i, String s) {}
}
