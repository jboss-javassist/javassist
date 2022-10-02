package test2;

class RemoveParent {
    int p;
}

public class Remove extends RemoveParent {
    public int f1;
    public int f2;
    public String f3;
    public Remove f4;
    public int[] f5;
    public int f6;
    int g = 3;

    public void bar() {}
    public Remove() { g = 7; }
    public Remove(int i) { g = i; }

    public int foo() {
        return g;
    }

    public void bar2() {}
}
