package test2;

public class MethodCall {
    public Object bar() {
        String[] str = new String[] { "one", "two" };
        return str.clone();
    }
}
