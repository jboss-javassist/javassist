package test1;

public class ExprEdit3 {
    int value;

    private int f() { return 3; }

    public ExprEdit3() { value = 0; }

    public ExprEdit3(ExprEdit3 e, int i) { value = i; }

    public int k1() { return new ExprEdit3(null, f()).value; }
}
