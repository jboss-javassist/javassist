package test5;

interface JIRA248Intf {
    default int foo() { return 1; }
}

class JIRA248Sup2 {
    public int bar() { return 200; }
}

class JIRA248Sup extends JIRA248Sup2 implements JIRA248Intf {
}

public class JIRA248 extends JIRA248Sup {
    public int foo() { return 70; }
    public int bar() { return 3000; }
}
