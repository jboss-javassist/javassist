package test2;

/**
 * Used by test2.Inner
 */
public class Inner2 {
    public static class Child {
        public int value;
    }

    private java.util.Properties p;
    private Child c;

    public Inner2(java.util.Properties props, Child child) {
        p = props;
        c = child;
    }

    public void print() {
        System.out.println(p);
        System.out.println(c);
    }
}
