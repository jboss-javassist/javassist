package test1;

public class StaticConsBody {
    public static int i;
    public int j;

    static {
        i = 3;
    }

    public StaticConsBody() {
        j = 2;
    }

    public int run() {
        return i + j;
    }
}
