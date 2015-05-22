package test3;

public class NestedClass {
    public class Inner {
        int i;
    }

    public static class StaticNested {
        int k;
    }

    public Object foo() {
        return new Object() {
            public String toString() { return "OK"; }
        };
    }

    public Object bar() {
        class Local {
            int j;
        }
        return new Local();
    }
}
