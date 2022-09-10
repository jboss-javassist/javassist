package test5;

interface DefaultMethodSupIntf {
    default int foo() { return 0; }
}

interface DefaultMethodIntf extends DefaultMethodSupIntf {
    default int foo() { return 1; }
    static int baz() { return 10; }
}

public class DefaultMethod implements DefaultMethodIntf {
    public int bar() { return DefaultMethodIntf.super.foo(); }

    public static void main(String[] args) {
        int i = new DefaultMethod().bar() + new DefaultMethod().foo() + DefaultMethodIntf.baz();
        System.out.println(i);
    }
}
