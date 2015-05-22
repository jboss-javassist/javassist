package testproxy;

interface BridgeMethodInf {
    public Long getId();
    public void setId(Long id);
    public Number m1();
}

abstract class BridgeMethodSuper<T> {
    public abstract T id(T t);
}

public class BridgeMethod extends BridgeMethodSuper<String> implements BridgeMethodInf {
    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer m1() { return 7; }
    public String id(String s) { return s; }
}
