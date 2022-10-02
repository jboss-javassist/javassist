package test1;

public class CalleeAfter {
    public int p;

    public CalleeAfter() {
	p = 3;
    }

    public int m1(int i) {
	return p + i;
    }

    public char m2(char c) {
	return c;
    }

    public int test() {
	if (m2('a') == 'b')
	    return m1(10);
	else
	    return -1;
    }
}
