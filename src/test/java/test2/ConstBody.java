package test2;

class ConstBody2 {
    int value;
    ConstBody2() {
        value = 1;
    }

    ConstBody2(String s, Integer i) {
        value = 2;
    }
}

public class ConstBody extends ConstBody2 {
    public ConstBody() {
        super();
    }

    public int bar() {
        return value;
    }
}
