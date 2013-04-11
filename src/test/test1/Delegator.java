package test1;

class SuperDelegator {
	public int f(int p) { return p + 1; }
    public static int g(int p) { return p + 1; }
}

public class Delegator extends SuperDelegator {
    public int run() {
        return f(3) + g(10);
    }
}
