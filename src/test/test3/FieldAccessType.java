package test3;

public class FieldAccessType {
    private int[] k;
    public void access() {
        k = new int[1];
        @SuppressWarnings("unused")
        int i = 3;
        i += k[0];
    }
}
