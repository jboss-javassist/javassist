package test5;

public class NestHostStaticMethod {
    private static int helper(int value) {
        StaticProceedProbe.helperCalls++;
        return value + 1;
    }

    public static int helper(String value) {
        StaticProceedProbe.overloadCalls++;
        return value.length() + 38;
    }

    private static int noArguments() {
        StaticProceedProbe.helperCalls++;
        return 42;
    }

    private static long wide(long first, double second) {
        StaticProceedProbe.helperCalls++;
        return first + (long)second;
    }

    private static void store(int value) {
        StaticProceedProbe.helperCalls++;
        StaticProceedProbe.storedValue = value + 1;
    }

    private static int values(int... values) {
        StaticProceedProbe.helperCalls++;
        StaticProceedProbe.seenArray = values;
        return values[0] + values[1];
    }

    public class Inner {
        public int run() {
            return helper(StaticProceedProbe.argument());
        }

        public int runNoArguments() {
            return noArguments();
        }

        public long runWide() {
            return wide(StaticProceedProbe.wideArgument(), 1.0);
        }

        public int runVoid() {
            store(StaticProceedProbe.argument());
            return StaticProceedProbe.storedValue;
        }

        public int runVarargs() {
            return values(StaticProceedProbe.arrayArgument());
        }

        public int runInterface() {
            return StaticProceedInterface.run();
        }
    }
}
