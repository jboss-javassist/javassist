package test3;

class CopyAnnoBase {
    int x = 3;
    @VisibleAnno public int getX() { return x; }
}

public class CopyAnno extends CopyAnnoBase {
    public int getX() { return super.getX(); }
}
