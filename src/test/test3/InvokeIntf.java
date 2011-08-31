package test3;

interface InvokedIntf {
    Object clone();
}

interface InvokedIntf2 extends InvokedIntf {
}

class InvokedIntf3 implements InvokedIntf2 {
    public Object clone() {
        try {
            return super.clone();
        }
        catch (Exception e) {
            return null;
        }
    }
}

public class InvokeIntf {
    public int test() {
        doit(new InvokedIntf3());
        return 7;
    }
    public void doit(InvokedIntf2 ii) {
        ii.clone();
        ii.toString();
    }
}
