package test1;

public class FieldInit {
    public static int counter = 0;
    public static int loop = 3;

    public static int get(Object obj) {
	System.out.println("FieldInit: get");
	return ++counter;
    }

    public FieldInit() {
	do {
	    --loop;
	} while (loop > 0);
    }

    public static class FI {
        public FieldInit fi;
        public FI(FieldInit fi) {
            this.fi = fi;
        }
    }
}
