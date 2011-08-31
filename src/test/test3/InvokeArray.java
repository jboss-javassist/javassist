package test3;

class InvokeArrayEle {
    String name;
}

public class InvokeArray {
    public int test() {
        return doit(new InvokeArrayEle[3]);
    }
    public int doit(InvokeArrayEle[] ae) {
        Object[] ae2 = (Object[])ae.clone();
        return ae2.length;
    }
}