package test2;

public class InsertAt {
    public int counter = 0;

    public int foo() {
        for (int i = 0; i < 3; i++)
            counter++;

        return counter;
    }

    public int bar() {
        return bar2(7);
    }

    public int bar2(int k) {
        int i = 1;
        int j = i + 3;
        k += i + j;
        return k;
    }
}
