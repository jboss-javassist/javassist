package test4;

public class NestedClass {
    public S.S2 s2;
    public class N {
        public String toString() { return "N"; }
    }
    public static class S {
        public String toString() { return "S"; }
        public static class S2 {
            public String toString() { return "S2"; }
        }
    }
    public Object foo() {
        class In {
            public String toString() { return "S"; }
            public String toString2() { return new S().toString(); }
        }
        return new Object() {
            public String toString() {
                return new Object() {
                    public String toString() {
                        return "ok";
                    }
                }.toString();
            }
        };
    }
}
