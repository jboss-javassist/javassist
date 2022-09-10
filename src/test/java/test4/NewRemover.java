package test4;

public class NewRemover {
    static NewRemover obj = new NewRemover(7);
    int value;
    static int value2 = 5;

    public NewRemover() {
        this(3);
    }

    public NewRemover(int k) {
        value = k;
    }

    public int run() {
        return make();
    }

    public int make() {
        NewRemover nr = new NewRemover(value2 > 0 ? 3 : 0);
        return nr.value;
    }

    public static NewRemover make2(int z) {
        System.out.println("make2 " + z);
        obj.value += z;
        return obj;
    }
}
