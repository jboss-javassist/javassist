package sample.vector;

public class Sample extends java.util.Vector {
    public void add(X e) {
	super.addElement(e);
    }

    public X at(int i) {
	return (X)super.elementAt(i);
    }
}

class X {
}
