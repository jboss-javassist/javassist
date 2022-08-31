package test3;

class TransNewClassOld {
    int k = 1;
    TransNewClassOld() {}
    TransNewClassOld(int i) { k = i; }
    TransNewClassOld(TransNewClassOld obj) { k = obj.k; }
}

class TransNewClassNew extends TransNewClassOld {
    TransNewClassNew() { k = 10; }
    TransNewClassNew(int i) { k = i * 10; }
    TransNewClassNew(TransNewClassOld obj) { k = obj.k * 2; }
}

class TransNewClassNot extends TransNewClassOld {
    TransNewClassNot() { k = 100; }
}

public class TransNewClass {
    public static class TransNewClass2 {
        public int test() {
            TransNewClassOld obj = new TransNewClassOld();
            TransNewClassOld obj2 = new TransNewClassOld();
            TransNewClassOld obj3 = new TransNewClassOld(3);
            return obj.k + obj2.k + obj3.k;
        }
    }

    public int test() {
        TransNewClassOld obj = new TransNewClassOld();
        TransNewClassOld obj2 = new TransNewClassOld(4);
        TransNewClassOld obj3 = new TransNewClassNot();
        TransNewClassOld obj4 = new TransNewClassOld(new TransNewClassOld());
        return obj.k + obj2.k + obj3.k + obj4.k;
    }
}
