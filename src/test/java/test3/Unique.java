package test3;

interface Unique5 {
    void bar100();
    void bar101b();
}

interface Unique4 extends Unique5 {
    void bar();
}

abstract class Unique3 implements Unique4 {
    abstract void foo();
}

class Unique2 {
    void foo() {}
    void foo100() {}
    void foo101() {}
}

public class Unique extends Unique2 {
    void foo() {}
}
