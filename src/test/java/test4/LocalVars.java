package test4;

public class LocalVars {
    public int run() {
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

        switch (p) {
        case 1:
            p = 100;
            break;
        default :
            ++p;
        }
        return p + r;
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

        switch (p) {
        case 1:
            p = 100;
            break;
        default :
            ++p;
        }
 
        return p + r;
    }

    public int foo(int i) { return i; }
}
