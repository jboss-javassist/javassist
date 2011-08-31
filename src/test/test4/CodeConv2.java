package test4;

public class CodeConv2 {
    int field = 3;
    static int sf = 1;

    public int run() {
        field = 7;
        sf = 8;
        switch (field) {
        case 0:
            field = 1;
            break;
        default:
        }
        int r = field * 10000 + sf;
        switch (field) {
        case 0:
            field = 1;
            break;
        default:
        }
        return r;
    }

    public static void write(Object target, int value) {
        if (target == null)
            sf = value * 2;
        else
            ((CodeConv2)target).field = value * 2;
    }

    public static int read(Object target) {
        if (target == null)
            return sf * 100;
        else
            return ((CodeConv2)target).field * 100;
    }
}
