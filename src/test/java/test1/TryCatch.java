package test1;

public class TryCatch {
    int a = 0;
    String s = null;

    public void init() {
	s = "test";
    }

    public void doit() {
	a = s.length();
    }

    public void m2() {}

    public int m1() {
	m2();
	return a;
    }

    public int p1() {
	try {
	    return s.length();
	}
	catch (NullPointerException e) {
	    throw e;
	}
    }

    public int run() {
	return m1();
    }
}
