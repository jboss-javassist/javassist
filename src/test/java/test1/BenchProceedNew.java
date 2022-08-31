package test1;

class BenchProceedNew2 {
}

class BenchProceedNew3 {
    int p, q;
    BenchProceedNew3(int i, int j) {
        p = i; q = j;
    }
}

public class BenchProceedNew {
    public static final int N = 10000000;
    Object result0;

    public int org0() {
        long time = System.currentTimeMillis();
        Object obj = null;
        for (int i = N; i > 0; --i)
            obj = new BenchProceedNew2();

        long time2 = System.currentTimeMillis();
        result0 = obj;
        return (int)(time2 - time);
    }

    public int jvst0() {
        long time = System.currentTimeMillis();
        Object obj = null;
        for (int i = N; i > 0; --i)
            obj = new BenchProceedNew2();

        long time2 = System.currentTimeMillis();
        result0 = obj;
        return (int)(time2 - time);
    }

    public int org2() {
        long time = System.currentTimeMillis();
        Object obj = null;
        for (int i = N; i > 0; --i)
            obj = new BenchProceedNew3(i, i);

        long time2 = System.currentTimeMillis();
        result0 = obj;
        return (int)(time2 - time);
    }

    public int jvst2() {
        long time = System.currentTimeMillis();
        Object obj = null;
        for (int i = N; i > 0; --i)
            obj = new BenchProceedNew3(i, i);

        long time2 = System.currentTimeMillis();
        result0 = obj;
        return (int)(time2 - time);
    }

    public static void main(String[] args) throws Exception {
        BenchProceedNew bp = new BenchProceedNew();
        System.out.println("iteration " + N);
        System.out.println("org0 (msec) " + bp.org0());
        System.out.println("jvst0 (msec) " + bp.jvst0());
        System.out.println("org2 (msec) " + bp.org2());
        System.out.println("jvst2 (msec) " + bp.jvst2());

        System.out.println("org0 (msec) " + bp.org0());
        System.out.println("jvst0 (msec) " + bp.jvst0());
        System.out.println("org2 (msec) " + bp.org2());
        System.out.println("jvst2 (msec) " + bp.jvst2());
    }
}
