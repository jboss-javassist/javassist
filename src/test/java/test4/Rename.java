package test4;

interface IRename {
    Rename foo(Rename r);
}

class RenameB {
    int foo() { return 10; }
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
        RenameB rb = new RenameB();
        return foo(this).value + rb.foo();
    }
}

