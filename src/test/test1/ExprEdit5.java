package test1;

public class ExprEdit5 {
    int value;

    public ExprEdit5() { value = 0; }

    public ExprEdit5(String s) { value = 1; }

    public int k1() {
	ExprEdit5 e = new ExprEdit5();
	return e.value;
    }
}
