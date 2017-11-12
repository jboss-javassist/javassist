package test2;

public class Prune extends java.awt.Point implements Cloneable, Runnable {
    /** default serialVersionUID */
    private static final long serialVersionUID = 1L;
    public int value;

    public Prune(int i) {
        value = i;
    }

    public void run() {}

    public int sum() {
        return x + y;
    }
}
