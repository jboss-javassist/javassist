package test1;

public class CalleeAfter2 {
    public int p;

    public CalleeAfter2() {
	p = 0;
    }

    public int m1(int i) {
	return 0;
    }

    public void m2(int i) {
    }

    public String m3(int i) {
        return null;
    }

    public String m4(int i) {
	return null;
    }

    public int[] m5(int i) {
	return null;
    }

    public int k1(int i) {
	return 1;
    }

    public void k2(int i) {
        p = 4;
    }

    public String k3(int i) {
        return "ok";
    }

    public int[] k5(int i) {
        return new int[2];
    }

    public int test() {
        m2(0);
        int q = m3(0).equals("ok") ? 10 : 20;
        return m1(0) + p + q + m5(0).length;
    }
}
