package test4;

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
