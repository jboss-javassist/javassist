package test1;

class BenchProceed2 {
    public void calc2() {
        for (long i = 0; i < 10000000; ++i)
            Math.sqrt(i);
    }
}

public class BenchProceed {
    public double d;
    public java.lang.reflect.Method calcM;

    public static final int N = 1000000;

    public BenchProceed() throws Exception {
        calcM = this.getClass().getDeclaredMethod("calc", new Class[0]);
    }

    public void calc() {
        d = Math.sqrt(3.0);
    }

    public int p() {
        long time = System.currentTimeMillis();
        for (int i = N; i > 0; --i)
            calc();

        long time2 = System.currentTimeMillis();
        return (int)(time2 - time);
    }

    public int q() {
        long time = System.currentTimeMillis();
        for (int i = N; i > 0; --i)
            calc();

        long time2 = System.currentTimeMillis();
        return (int)(time2 - time);
    }

    public int s() {
        BenchProceed2 bp = new BenchProceed2();
        for (int i = 0; i < 5; ++i)
            bp.calc2();

        return 0;
    }

    public int t() {
        BenchProceed2 bp = new BenchProceed2();
        for (int i = 0; i < 5; ++i)
            bp.calc2();

        return 0;
    }

    public void before(Object[] args) {
    }

    public Object replace(Object[] args) {
        try {
            return calcM.invoke(this, args);
        }
        catch (Exception e) {
            System.out.println(e);
        }

        return null;
    }

    public static void main(String[] args) throws Exception {
        BenchProceed bp = new BenchProceed();
        System.out.println("iteration " + N);
        System.out.println("p (msec) " + bp.p());
        System.out.println("q (msec) " + bp.q());
        System.out.println("p (msec) " + bp.p());
        System.out.println("q (msec) " + bp.q());

        bp.s();
        bp.t();
    }
}
