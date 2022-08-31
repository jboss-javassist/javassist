package test3;

public class EmptyCatch {
    public int test(int i) {
        try {
            i += 200;
        }
        finally {
            i += 10;
        }

        return i;
    }
}
