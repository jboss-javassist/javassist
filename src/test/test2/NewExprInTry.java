package test2;

import java.util.HashMap;

@SuppressWarnings("rawtypes")
class HashMapWrapper extends HashMap {
    /** default serialVersionUID */
    private static final long serialVersionUID = 1L;

    HashMapWrapper(int size, int args) {
        super(size);
    }
}

@SuppressWarnings({"rawtypes","unused"})
public class NewExprInTry {
    public int run() {
        return foo(6);
    }

    public int foo(int size) {
        HashMap h;
        try {
            h = new HashMap(size);
        }
        catch (Exception e) {}
        return 1;
    }
}
