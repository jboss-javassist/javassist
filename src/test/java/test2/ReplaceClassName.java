package test2;

class ReplaceClassName2 {
}

class ReplaceClassName3 {
}

public class ReplaceClassName {
    ReplaceClassName2 field;
    public ReplaceClassName(ReplaceClassName2 a) {
        field = a;
    }

    public void foo(ReplaceClassName2 a) {
        field = a;
    }
}
