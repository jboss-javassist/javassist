package test1;

class Pac2 {
    int p, q;
}

public class Pac {
    int x;
    public int run() {
        Class c = Pac2.class;
        Package p = c.getPackage();
        System.out.println(p);
        return p == null ? 0 : 1;
    }
}
