package test3;

public class InheritCons {
    int value;
    public InheritCons() { this(2); }
    private InheritCons(int i) { value = i; }
    protected InheritCons(String s) { this(0); }
    InheritCons(char c) { this(1); }
}
