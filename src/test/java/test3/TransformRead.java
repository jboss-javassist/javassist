package test3;

class TransformRead1 {
    public int value = 1;
    public int value2 = 10;
}

public class TransformRead extends TransformRead1 {
    public int value = 100;
    public int foo() {
        return value + value2 + super.value;
    }
    public static int getValue(Object obj) { return 1000; }
    public static int getValue2(Object obj) { return 10000; }
}
