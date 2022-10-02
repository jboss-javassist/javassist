package test2;

@SuppressWarnings("unused")
public class Inner {
    public void sample() throws Exception {
        java.util.Properties props = new java.util.Properties();
        test2.Inner2.Child ace = null;
        test2.Inner2 agd = new test2.Inner2(props, ace);
    }
    public static void main(String args[]) {
        System.out.println("Inner");
    }
}
