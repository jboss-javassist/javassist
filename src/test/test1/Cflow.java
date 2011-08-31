package test1;

public class Cflow {
    public int run() {
	return k1(4);
    }

    public int run2() {
	return fact(5);
    }

    public int fact(int n) {
	if (n <= 1)
	    return n;
	else
	    return n * fact(n - 1);
    }

    public int k1(int i) { 
	if (i > 1)
	    return k2(i - 1);
	else if (i == 1)
	    return i;
	else if (i == 0)
	    throw new RuntimeException();
	else
	    return -i;
    }

    public int k2(int i) { 
	if (i > 1)
	    return k1(i - 1);
	else if (i == 1)
	    return i;
	else if (i == 0)
	    throw new RuntimeException();
	else
	    return -i;
    }
}
