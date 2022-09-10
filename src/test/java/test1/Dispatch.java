package test1;

public class Dispatch {
    public int run() {
	return 5;
    }

    public int f(Object obj) {
	return 1;
    }

    public int f(Object[] obj) {
	return 2;
    }
}
