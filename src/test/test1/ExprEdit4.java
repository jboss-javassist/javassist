package test1;

public class ExprEdit4 {
    int value;

    void f() { value = 3; }

    public ExprEdit4() { value = 0; }

    public int k1() {
	ExprEdit4 e = null;
	e.f();
	e = null;
	return new ExprEdit4() == null ? 1 : 0;
    }
}
