package test5;

interface ProceedDefaultI {
    default int foo() { return 13; }
}

public class ProceedDefault implements ProceedDefaultI {
    public int run() { return bar(); }
    public int foo() { return 1700; }
    public int bar() {
        return foo() + ProceedDefaultI.super.foo();
    }
}
