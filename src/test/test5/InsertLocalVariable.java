package test5;

public class InsertLocalVariable {
    int k;

    public InsertLocalVariable() {
	k = 1;
    }

    public int test() {
	foo(0, 0, 0);
	return k;
    }

    public void foo(int i /* 0 */, int j /* 1 */, int k /* 2 */) {
	boolean bI = false; /* 0 */
	int l = 0; /* 3 */
	if (l == 0) {
	    this.k = -1;
	    return;
	}

	try {
	    boolean bJ = true; /* 1 */
	    if (!bJ) {
		throw new Exception();
	    }
	} catch (Exception e /* 0 */) {
	    int m = 0; /* 4 */
	    l += m;
	    if (bI) {
		int n = 0; /* 5 */
		l += n;
	    }

	    for (boolean bK = false /* 2 */; l == 3;) {
		if (bK) {
		    int o = 0; /* 6 */
		    l += o;
		}
	    }

	    while (l == 4) {
		int p = 0; /* 7 */
		l += p;
	    }

	    do {
		int q = 0; /* 8 */
		l += q;
	    } while (false);

	    switch (l) {
	    case 6:
		int r = 0; /* 9 */
		l += r;
	    }
	}

	int s = 0; /* 10 */
	this.k += i;
	this.k += j;
	this.k += k;
	this.k += l;
	this.k += s;
    }
}
