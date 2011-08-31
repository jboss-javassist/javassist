package test3;

class FieldCopy2 {
    int bar;
}

public class FieldCopy {
    public @interface Test {
    }

    @Test private static int foo;
}
