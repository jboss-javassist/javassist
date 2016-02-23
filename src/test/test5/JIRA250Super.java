package test5;

interface JIRA250BarI {
    int foo();
}

class JIRA250Bar implements JIRA250BarI {
    public int foo() { return 1; }
}

interface JIRA250SuperI {
    JIRA250BarI getBar();
}

public class JIRA250Super extends JIRA250Super2 implements JIRA250SuperI {
}

