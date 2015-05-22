package test1;

import java.io.IOException;

class Howard4 implements HowardHome {
    int n = 0;
    public Object create() throws IOException {
        if (n == 1)
            throw new IOException();
        else
            return "howard4";
    }
}

interface HowardHome {
    Object create() throws IOException;
}

class Howard2 {
    Object lookup(String n) { return new Howard4(); }
}

public class Howard extends Howard2 {
    private Object _remote;

    public int run() {
        return 0;
    }
}
