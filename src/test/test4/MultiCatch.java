package test4;

public class MultiCatch {
    public void print() { System.out.println("MultiCatch"); }
    public int test1() { return m1(1); }
    public int m1(int i) {
        // Java 7 syntax
        try {
            return foo(i);
        }
        catch (java.io.IOException | NullPointerException e) {
            return e.getMessage().length();
        }
    }
    public int foo(int i) throws java.io.IOException {
        if (i < 0)
            throw new java.io.IOException("negative");
        else if (i < 10)
            throw new NullPointerException("less than 10");
        else
            return i;
    }
}
