package test1;

class GetThrow1 extends Exception {
}

class GetThrow2 extends Exception {
}

public class GetThrowables {
    int k = 0;

    public void m1() throws GetThrow1, GetThrow2 {
	if (k < 0)
	    throw new GetThrow1();
	else if (k == 1)
	    throw new GetThrow2();

	k = 1;
    }

    public int run() throws GetThrow2 {
	int i = 0;
	try {
	    try {
		m1();
	    }
	    catch (GetThrow1 e) {
		i = 1;
		throw e;
	    }
	    finally {
		i += 3;
	    }
	}
	catch (GetThrow1 e2) {
	    ++i;
	}

	return i;
    }
}
