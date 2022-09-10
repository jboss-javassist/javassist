package test4;

public class JIRA207 {
    public int run() {
        int i = 3;
        return foo(i);
    }

    public int foo(int i) {
        int k = i + 3;
        if (k > 0)
            return k * k;
        else
            return k;
    }

    public int run2() {
        int i = 0;
        int p = i;
        int q = p;
        int r = q;
        for (int k = 1; k < 3; ++k)
            p += k;

        for (int k = 3; k > 0; --k)
            try {
                foo(k);
                p++;
            }
            finally {
                p++;
            }

        try {
            foo(p);
        }
        catch (RuntimeException e) {
            if (p > 0)
                throw e;
        }

        switch (p) {
        case 1:
            p = 100;
            break;
        default :
            ++p;
        }
        return p + r;
    }
}
