package test2;

interface IStaticMember {
    int bar();
}

public class StaticMember implements IStaticMember {
    public static int k = 3;
    public static int foo() { return 7; }
    public int bar() { return 3; }
}
