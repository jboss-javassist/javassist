package test3;

public class ReplaceNew {
    public ReplaceNew() {}
    public ReplaceNew(String s) {}
    int i = 0;
    public int run() {
        i = 3;
        @SuppressWarnings("unused")
        ReplaceNew s = new ReplaceNew();
        new ReplaceNew();
        return i;
    }

    static int j = 0;
    public int run2() {
        new ReplaceNew(new String("test"));
        return j;
    }
}
