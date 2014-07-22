package test4;


import java.util.function.IntConsumer;
import java.util.stream.IntStream;

interface JIRA220intf {
    static void bar() {
        // Do something
    }
}

public class JIRA220 implements JIRA220intf {
    public static void foo() {
        JIRA220intf.bar();
    }
}
