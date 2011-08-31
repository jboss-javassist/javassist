package test3;

public class InsertBeforeType {
    String value = "";
    void foo() { value += ":"; }
    public int test() {
        foo();
        System.out.println(value);
        return value.length();
    }
}
