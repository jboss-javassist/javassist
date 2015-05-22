package test3;

interface MethodRedirectIntf {
    int afo();
}

public class MethodRedirect implements MethodRedirectIntf {
    private int foo() { return 0; }
    public static int poi() { return 1; } 
    public int bar() { return 2; }
    public int afo() { return 3; }

    public int test() {
        return bar();
    }

    public static void main(String[] args) {
        System.out.println(new MethodRedirect().test());
    }
}
