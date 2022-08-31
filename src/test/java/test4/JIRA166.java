package test4;

public class JIRA166 {
    String length = "jira166";
    JIRA166 self() { return this; }
    void print() { System.out.println(length); }
    public int run() {
        print();
        System.out.println(length);
        return 1;
    }
}
