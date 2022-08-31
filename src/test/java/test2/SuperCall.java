package test2;

class SuperClass {
    public void foo() throws Exception {}
}

public class SuperCall extends SuperClass {
    int i = 0;
    public int bar() throws Exception {
        foo();
        return 1;
    }

    public void foo() throws Exception {
        if (++i > 5)
            throw new Exception("infinite regression?");

        super.foo();
    }
}
