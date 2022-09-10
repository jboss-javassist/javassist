package test3;

interface ErasureGet<T> {
    T get();
}

public class Erasure<T> {
    T value;
    public Erasure(T t) { value = t; }
    public Erasure() { value = null; }
    public int run() {
        @SuppressWarnings("unchecked")
        ErasureGet<String> obj = (ErasureGet<String>)new Erasure<String>("1234");
        return obj.get().length();
    }
}
