package test2;

public class CodeGen {
    public String msg = "";
    public int seven() { return 7; }

    public String seven(String s) { return s + 7; }

    public String six(String s) { return s + 6; }

    public int run() {
        System.out.println("done.");
        return msg.length();
    }
}
