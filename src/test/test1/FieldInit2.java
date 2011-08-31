package test1;

public class FieldInit2 {
    public static int counter = 0;
    public static int loop = 3;

    public static int get(Object obj) throws Exception {
	throw new Exception();
    }

    public FieldInit2() {
	try {
	    do {
		--loop;
	    } while (loop > 0);
	}
	catch (Exception e) {
	    System.out.println("FieldInit2: catch");
	}
    }
}
