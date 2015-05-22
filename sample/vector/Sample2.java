package sample.vector;

public class Sample2 extends java.util.Vector {
    public Object add(Object[] args) {
	super.addElement(args[0]);
	return null;
    }

    public Object at(Object[] args) {
	int i = ((Integer)args[0]).intValue();
	return super.elementAt(i);
    }
}
