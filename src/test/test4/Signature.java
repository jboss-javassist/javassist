package test4;

public class Signature<T> {
    public static class Foo {
        int value;
    }

    public int run() {
        Signature<String> s = new Signature<String>();
        return s.foo(Integer.valueOf(3), "foo", s, null).length();
    }

    <S> T foo(S s, T t, Signature<T> signature, Signature<Signature<String>> v) {
        return t;
    }
}
