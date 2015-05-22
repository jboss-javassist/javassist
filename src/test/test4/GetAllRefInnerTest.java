package test4;

class GetAllRefInnerTest2<T> {
    Class clazz;
    T value;
    void foo(T t) { value = t; }
    Object poi(T t) {
        return new Object() {
            public String toString(T t) { return this.getClass().toString(); }
        };
    }
}

public class GetAllRefInnerTest<T> {
    public T bar(T b) {
        Object obj = new GetAllRefInnerTest2<java.util.HashMap>() {
            void foo(java.util.HashMap a) { value = null; String s = clazz.toString() + a.toString(); }
        };
        return b;
    }
    public Object foo() {
        return new java.util.HashSet<String>() {
            public String toString() { return this.getClass().toString(); } 
        };
    }
}
