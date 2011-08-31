package test3;

public class EmptyConstructor {
    static {}
    public int value;
}

class EmptyConstructor2 extends EmptyConstructor {
    static {}
    public int value2;
}

class EmptyConstructor3 extends EmptyConstructor {
    public int value3;
    public EmptyConstructor3() {}
    public EmptyConstructor3(int x) { super(); }
}

class EmptyConstructor4 extends EmptyConstructor3 {
    public static int sv = 3;
    public int value3;
    EmptyConstructor4(int x) {
        super(x);
    }

    EmptyConstructor4(double x) {
        this();
    }

    EmptyConstructor4() {
        value3 = 7;
    }
}
