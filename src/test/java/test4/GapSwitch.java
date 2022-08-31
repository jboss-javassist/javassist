package test4;

public class GapSwitch {
    public int value;
    public int foo(int i) { return i + 1; }
    public int run() {
        value = 0;
        int k = 0;
        k = foo(k);
        switch (value) {
        case 0:
            k++;
            break;
        case 1:
            k *= 10;
            break;
        default:
            k *= 100;
            break;
        }

        return k + value * 1000;
    }

    public int run2() {
        value = 0;
        int k = 0;
        k = foo(k);
        switch (value) {
        case 10:
            k++;
            break;
        case 1300:
            k *= 10;
            break;
        default:
            k *= 100;
            break;
        }

        return k + value * 1000;
    }

    public int run3() {
        value = 1;
        int k = 0;
        for (int i = 0; i < 2; i++) {
            k = foo(k);
            switch (value) {
            case 10:
                k++;
                k = foo(k);
                break;
            case 1300:
                k *= 100;
                k = foo(k);
                break;
            default:
                k *= 10;
                k = foo(k);
                break;
            }

            k = foo(k);
            switch (value) {
            case 10:
                k++;
                k = foo(k);
                break;
            case 13:
                k *= 100;
                k = foo(k);
                break;
            default:
                k *= 10;
                k = foo(k);
                break;
            }
        }

        return k + value;
    }
}
