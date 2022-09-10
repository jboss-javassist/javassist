package test1;

class CalleeBeforeParent {
    static int counter = 0;
    int r;

    CalleeBeforeParent(int k) {
	System.out.println("CalleeBeforeParent:" + k);
	r = counter;
    }
}

public class CalleeBefore extends CalleeBeforeParent {
    public int p;
    public static int q;

    public CalleeBefore() {
	this(3);
    }

    public CalleeBefore(int k) {
	super(k);
	p = q = 0;
    }

    public int m1(int i) {
	return p + i;
    }

    public static int m2(int i) {
	return q + i;
    }

    public int getr() { return r; }

    public int test() {
	return m1(3) + m2(10);
    }
}
