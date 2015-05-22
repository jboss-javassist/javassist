package test1;

public class ExprEdit7 {
    int value;
    Class c1, c2;

    public ExprEdit7() { value = 0; }

    public boolean k2(Object obj) {
        return obj instanceof ExprEdit7;
    }

    public ExprEdit7 k3(Object obj) {
        return (ExprEdit7)obj;
    }

    public int k1() {
        ExprEdit7 e = new ExprEdit7();
        if (k2(e))
            k3(e).value = 3;
        else
            k3(e).value = 7;

        System.out.println("ExprEdit7: " + c1.getName());
        if (c1 == c2 && c1.getName().equals("test1.ExprEdit7"))
            return e.value;
        else
            return e.value - 1;
    }
}
