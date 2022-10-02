package test2;

@SuppressWarnings("unused")
public class Anon {
    public Object make() {
        return new Object() { int k; };
    }

    public static class Anon2 {
        Object obj;
        public Anon2() {
            obj = new Object() { int k; };
        }
    }

    public static class Anon3 {
        public static Object sobj = new Object() { int p; };
    }
}
