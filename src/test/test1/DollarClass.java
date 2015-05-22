package test1;

public class DollarClass {
    int f = 1;

    public int run() {
	return k1(4) + k2(3);
    }

    public int k1(int i) { 
	return i;
    }

    public static int k2(int i) {
	return i + k3(i);
    }

    public static int k3(int i) {
	return i;
    }
}
