package test5;

public class InsertAfter {
    public int run() {
        return foo(7) + bar(20);
    }

    public int foo(int k) {
        if (k > 0)
            if (k > 10)
                return k + 1;
            else
                return k * 10;
        else
            return k * 100;
    }

    public int bar(int k) {
        if (k > 0)
            try {
                if (k > 10)
                    return k + 1;
                else
                    return k * 10;
            }
            catch (Exception e) {
                if (k > 0)
                    return k * 1000;
                else
                    throw e;
            }
        else
            return k * 100;
    }
}
