package test5;

public class NestHost2 {
    private int value;
    public class Foo {
        int foo() { return value++; } 
    }
    public class Bar {
        int bar() { return value++; }
    }
}
