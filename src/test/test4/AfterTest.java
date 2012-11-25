package test4;

public class AfterTest {
    public void print() { System.out.println("test4.AfterTest"); }

    public int test1() { return m1(10) + m1(-10); }

    public int m1(int i) {
        if (i > 0)
            i = i + 10;
        else
            return -i;

        i = i + 100;
        return i + 1;
    }

    public int test2() throws Exception { return m2(1); }

    public int m2(int i) throws Exception {
        if (i > 10)
            throw new Exception();
        else if (i > 0)
            i = i + 10;
        else
            return -i;

        i = i + 100;
        return i + 1;
    }

    public int test3() throws Exception { return m3(-10); }

    public int m3(int i) throws Exception {
        if (i > 10)
            throw new Exception();
        else if (i > 0)
            i = i + 10;
        else
            return -i;

        i = i + 100;
        throw new Exception();
    }

    public int test4() throws Exception {
        try {
            return m4(-10);
        }
        catch (Exception e) {
            return 100;
        }
    }

    public int m4(int i) throws Exception {
        if (i > 0)
            i = i + 10;

        i = i + 100;
        throw new Exception();
    }

    public int test11() { return mm1(10) + mm1(-10); }

    public int mm1(int i) {
        if (i > 0)
            i = i + 10;
        else
            return -i;

        i = i + 100;
        return i + 1;
    }

    public int test22() throws Exception { return mm2(1); }

    public int mm2(int i) throws Exception {
        if (i > 10)
            throw new Exception();
        else if (i > 0)
            i = i + 10;
        else
            return -i;

        i = i + 100;
        return i + 1;
    }

    public int test33() throws Exception { return mm3(-10); }

    public int mm3(int i) throws Exception {
        if (i > 10)
            throw new Exception();
        else if (i > 0)
            i = i + 10;
        else
            return -i;

        i = i + 100;
        throw new Exception();
    }

    public int test44() throws Exception {
        try {
            return mm4(-10);
        }
        catch (Exception e) {
            return 100;
        }
    }

    public int mm4(int i) throws Exception {
        if (i > 0)
            i = i + 10;

        i = i + 100;
        throw new Exception();
    }
}
