package test1;

public class CalleeAfter3 {
    int value = 1;
    public int m1(int k) {
        if (k > 3)
            return k;
        else
            return k + 1;
    }

    public String m2(int k) {
        if (k > 3)
            return "value" + k;
        else
            return "value" + value;
    }

    public void m3(int k) {
        if (k > 3)
            value += k;
        else
            value -= k;
    }

    public int m4(String obj) {
        try {
            return obj.length();
        }
        catch (NullPointerException e) {
            return 0;
        }
    }

    public int test() {
        m3(5);
        return m1(1) + m2(5).length() + value + m4("12345");
    }
}
