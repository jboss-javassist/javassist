package test1;

public class ExprEdit6 {
    int value;

    public ExprEdit6() { value = 0; }

    public int k2() {
	return value;
    }

    public int k1() {
	return k2();
    }
}
