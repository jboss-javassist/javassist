package test1;

public class Proceed2 {
    public Proceed2() { i = 3; }

    public int i;

    public void p() {}

    public int k1() {
	Proceed2 p2 = new Proceed2();
	boolean b = p2 instanceof Proceed2;
	Object obj = p2;
	Proceed2 q2 = (Proceed2)obj;
	p2.p();
	i = 2;
	return i;
    }

    public int k2() {
	Proceed2 p2 = new Proceed2();
	p2.p();
	i = 2;
	return i;
    }
}
