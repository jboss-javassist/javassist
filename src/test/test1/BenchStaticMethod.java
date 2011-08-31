package test1;

public class BenchStaticMethod {
    public static final int N = 10000000;

    public static int foo(int i) {
        i /= 100000;
        int f = 1;
        while (i > 1)
            f *= i--;

        return f;
    }

    public static void foo2(int i) {}

    public static int num = 0;

    public static int test() {
        long time = System.currentTimeMillis();
        for (int i = N; i > 0; --i)
            foo(i);

        long time2 = System.currentTimeMillis();
        return (int)(time2 - time);
    }

    public static int orgTest() {
        long time = System.currentTimeMillis();
        for (int i = N; i > 0; --i)
            foo(i);

        long time2 = System.currentTimeMillis();
        return (int)(time2 - time);
    }

    public static int handTest() {
        long time = System.currentTimeMillis();
        for (int i = N; i > 0; --i) {
            num += i;
            foo(i);
        }

        long time2 = System.currentTimeMillis();
        return (int)(time2 - time);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("orgTest (msec) " + orgTest());
        System.out.println("handTest (msec) " + handTest());
        System.out.println("test (msec) " + test());
    }
}
