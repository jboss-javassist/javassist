package test1;

public class SigType {
    int f = 1;

    public int run() {
	return k1(4, "test") + k2(3);
    }

    public int k1(int i, String s) { 
	return i + s.length();
    }

    public int k2(int i) {
	return f + k3(i);
    }

    public int k3(int i) {
	return i + 1;
    }
}
