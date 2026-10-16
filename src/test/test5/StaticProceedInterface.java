package test5;

public interface StaticProceedInterface {
    private static int helper(int value) {
        StaticProceedProbe.helperCalls++;
        return value + 1;
    }

    static int run() {
        return helper(41);
    }
}
