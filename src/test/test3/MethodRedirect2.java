package test3;

interface MethodRedirect2SupIntf {
    int foo();
    int bar();
    int bar2();
}

interface MethodRedirect2Intf extends MethodRedirect2SupIntf {
    int bar2();
}

class MethodRedirect2SupSup {
    public int bfo() { return 100; }
    public int bfo2() { return 200; }
}

class MethodRedirect2Sup extends MethodRedirect2SupSup {
    public int afo() { return 10; }
    public int afo2() { return 20; }
    public int bfo() { return 300; }
}

public class MethodRedirect2 extends MethodRedirect2Sup implements MethodRedirect2Intf {
    public int foo() { return 1; }
    public int bar() { return 2; }
    public int bar2() { return 3; }

    public int test(MethodRedirect2Intf intf, MethodRedirect2 clazz,
                    MethodRedirect2SupSup sup)
    {
        return intf.bar() + intf.bar2() + clazz.afo() + clazz.bfo() + sup.bfo();
    }

    public int test() {
        MethodRedirect2 obj = new MethodRedirect2();
        return test(obj, obj, obj);
    }

    public static void main(String[] args) {
        System.out.println(new MethodRedirect2().test());
    }
}
