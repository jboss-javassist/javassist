package test2;

import java.util.HashMap;

class HashMapWrapper extends HashMap {
    HashMapWrapper(int size, int args) {
        super(size);
    }
}

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
