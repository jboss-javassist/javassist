package test4;

public class InvokeDynCopyDest {
    public InvokeDynCopyDest() {
        System.out.println("my output:" + getString());
    }

    public String getString() {
        return "dest";
    }
}
