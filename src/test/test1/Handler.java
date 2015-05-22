package test1;

public class Handler {
    public int p;

    public Handler() {
	p = 3;
    }

    public int m1(int i) {
	p = 1;
	try {
	    try {
		if (i < 0)
		    throw new IndexOutOfBoundsException();
		else if (i == 0)
		    throw new ClassNotFoundException();
	    }
	    catch (IndexOutOfBoundsException e) {}
	}
	catch (ClassNotFoundException e) {}

	return p;
    }

    public int test() {
	return m1(1) + m1(0) + m1(-1);
    }
}
