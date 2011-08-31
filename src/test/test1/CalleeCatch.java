package test1;

public class CalleeCatch {
    public int p;

    public CalleeCatch() {
	p = 3;
    }

    public int m1(int i) throws Exception {
	throw new Exception();
    }

    public int test() throws Exception {
	return m1(p);
    }
}
