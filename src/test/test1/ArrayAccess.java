package test1;

public class ArrayAccess {
    int[] ia;
    int[][] iaa;

    public ArrayAccess() {
        ia = new int[3];
        iaa = new int[2][];
        ia[0] = 3;
        iaa[0] = ia;
    }

    public int test() {
        return ia[0] + iaa[1][0];
    }
}
