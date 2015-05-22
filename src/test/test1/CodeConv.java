package test1;

class CodeConvP {
    private int a1 = 7;
    protected int a2 = 11;
    protected int a3 = 13;
    protected int a4 = 17;

    protected int b1 = 3;

    public static int getA1(Object t) { return 23; }

    public static int getA2(Object t) { return 27; }

    public static void putB1(Object t, int v) { ((CodeConvP)t).b1 = 5; }
}

public class CodeConv extends CodeConvP {
    private String a1 = "a1";

    public int run() {
	b1 = 0;
	return b1 + a1.length() + a2 + a3;
    }
}
