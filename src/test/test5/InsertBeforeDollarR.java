package test5;

public class InsertBeforeDollarR {
    public int run() {
        if (foo(1, "baz").equals("baz"))
            return 1;
        else
            return 0;
    }

    public String foo(int i, Object obj) {
        return String.valueOf(i);
    }
}
