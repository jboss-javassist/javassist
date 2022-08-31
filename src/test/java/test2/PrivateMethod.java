package test2;

class PrivateMethod2 {
    private int f() { return 0; }

    int g() { return f(); }
}

public class PrivateMethod {
    public int i;
}
