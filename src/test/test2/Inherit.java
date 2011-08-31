package test2;

interface Inherit1 {
    void foo1();
}

interface Inherit2 extends Inherit1 {
    void foo2();
}

abstract class Inherit3 implements Inherit2 {
    abstract void foo3();
}

public class Inherit extends Inherit3 {
    public void foo1() { System.out.println("foo1"); }
    public void foo2() { System.out.println("foo2"); }
    public void foo3() { System.out.println("foo3"); }

    public static void main(String args[]) {
        Inherit i = new Inherit();
        Inherit2 i2 = i;
        i.foo2();
        i2.foo1();
    }
}
