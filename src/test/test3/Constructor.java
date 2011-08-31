package test3;

class SuperConsturctor {
    SuperConsturctor() {}
    SuperConsturctor(int p, int q) {}
    SuperConsturctor(int p, double r, long[] s, String t) {}
}

public class Constructor extends SuperConsturctor {
    static String str = "ok?";
    int i;

    public Constructor() {
        this(3);
        ++i;
    }

    public Constructor(int k) {
        super(0, 1.0, null, "test");
        i += k;
    }

    public Constructor(String s) {
        this();
        i += 10;
    }

    public Constructor(double d) {
        super(1, 2);
        i += 100;
    }

    public int run() {
        str = null;
        return 0;
    }
}
