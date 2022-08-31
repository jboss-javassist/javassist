package test1;

class ExprSuper8 {
    int value;
    ExprSuper8(int i) {
        value = i;
    }
}

public class ExprEdit8 extends ExprSuper8 {
    public ExprEdit8() { super(3); }

    public int k1() {
	return value;
    }
}
