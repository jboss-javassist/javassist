package test1;

public class LocalVars {
    public int poi(String str) {
        Object obj = str;
        int hash = obj.hashCode();
        return hash;
    }

    public int foo(int i) {
        int j = 3;
        if (i == 0) {
            String s = "zero";
            j += s.length();
        }
        return j + 3;
    }
}
