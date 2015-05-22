package test.javassist.proxy;

public class Foo {
    public String doSomething() {
         return "I'm doing something";
    }

    public Object getHandler() {
         return "This is a secret handler";
    }
}

class Foo2 {
    public String doSomething() { return "do something"; }
    public String getHandler() { return "return a string"; }
}