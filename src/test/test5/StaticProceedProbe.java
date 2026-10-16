package test5;

public class StaticProceedProbe {
    public static int helperCalls;
    public static int prefixCalls;
    public static int argumentCalls;
    public static int replacementCalls;
    public static int overloadCalls;
    public static int storedValue;
    public static int[] argumentArray;
    public static int[] seenArray;

    public static void prefix() {
        prefixCalls++;
    }

    public static int argument() {
        argumentCalls++;
        return 41;
    }

    public static long wideArgument() {
        argumentCalls++;
        return 41L;
    }

    public static int replacement() {
        replacementCalls++;
        return 0;
    }

    public static int[] arrayArgument() {
        argumentCalls++;
        argumentArray = new int[] {20, 22};
        return argumentArray;
    }
}
