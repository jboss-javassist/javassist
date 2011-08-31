package test1;

interface InvokeInt2 {
    int k(int i);
}

public class InvokeInt implements InvokeInt2 {
    public int run() {
	return check(this);
    }

    public int check(InvokeInt2 obj) {
	return obj.k(3);
    }

    public int k(int i) {
	return i + 1;
    }
}
