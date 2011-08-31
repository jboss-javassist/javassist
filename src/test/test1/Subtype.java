package test1;

public class Subtype extends SubtypeA implements SubtypeC {
    int i;
}

class SubtypeA {
    int a;
}

interface SubtypeB {
    final int b = 3;
}

interface SubtypeC extends SubtypeB {
    final int c = 4;
}
