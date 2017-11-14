package test5;

public class Issue155 {
    public void bar() {}

    public void foo() throws Throwable {
        try {
            bar();
        } catch (java.lang.IllegalArgumentException e) {
            bar();
        }
    }

    public int test() throws Throwable {
        foo();
        return 1;
    }

    public static void main(String[] args) throws Throwable {
        new Issue155().foo();
    }
}
