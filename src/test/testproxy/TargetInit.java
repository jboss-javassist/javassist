package testproxy;

public class TargetInit {
    static {
        System.out.println("TargetInit <clinit>");
        ProxyTester.testInitFlag = true;
    }

    public String m() { return "OK"; }
}
