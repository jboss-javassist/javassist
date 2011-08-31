package test4;

interface IRename {
    Rename foo(Rename r);
}

public class Rename implements IRename {
    int value = 3;
    Rename next = null;

    public Rename foo(Rename r) {
        Rename k = r;
        if (k == null)
            return null;
        else
            return k.next;
    }

    public int run() {
        next = new Rename();
        next.value = 4;
        return foo(this).value;
    }
}
