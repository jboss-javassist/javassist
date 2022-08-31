package test4;

interface JIRA158Intf {
    int foo(long[] j, double[] d);
    int bar(long j, double d);
}

class JIRA158Impl implements JIRA158Intf {
    public int foo(long[] j, double[] d) {
        return 7;
    }
    public int bar(long j, double d) {
        return 8;
    }
}

public class JIRA158 {
    long[] jj;
    double[] dd;
    long j;
    double d;
    JIRA158Intf obj;
    public JIRA158() {
        jj = new long[1];
        dd = new double[1];
        j = 3L;
        d = 3.0;
        obj = new JIRA158Impl();
    }
}
