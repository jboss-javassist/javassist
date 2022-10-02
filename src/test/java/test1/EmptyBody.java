package test1;

public abstract class EmptyBody {
    public EmptyBody() {}
    public EmptyBody(int i) {}
    public EmptyBody(String i) { System.out.println(i); }
    public EmptyBody(double d) { this(3); }

    public void m1(int i, int j) {}
    public void m2(int i, int j) {
	try { return; }
	catch (Exception e) {}
    }

    public int m3(int k) { return 0; }

    public native int m4();

    public abstract int m5();
}
