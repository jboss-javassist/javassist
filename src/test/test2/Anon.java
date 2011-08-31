package test2;

public class Anon {
    public Object make() {
        return new Object() { int k; };
    }
}
