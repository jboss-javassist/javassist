package test2;

public class NewExprTry {
    public NewExprTry() { this(3); }

    public NewExprTry(int i) {}

    public int run() {
        return foo(3).getClass().getName().length();
    }

    public Object foo(int i) {
        NewExprTry obj = new NewExprTry(i);
        return obj;
    }

    public static void main(String[] args) {
        @SuppressWarnings("unused")
        NewExprTry obj = new NewExprTry(3);
    }
}
