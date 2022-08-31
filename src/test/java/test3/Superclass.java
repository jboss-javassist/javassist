package test3;

class Superclass2 {
    public int foo() { return 1; }
    public int bar() { return 10; }
}

class Superclass3 extends Superclass2 {
    public int test() { return foo(); }
    public int bar() { return 20; }
}

public class Superclass extends Superclass2 {
    public int foo() { return super.foo() + super.bar(); }
}
