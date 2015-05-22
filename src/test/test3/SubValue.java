package test3;

class SuperValue {
    int i;
}

public class SubValue extends SuperValue {
    public SubValue after(SuperValue ret, SuperValue sup, SuperValue sup2) {
       return null;
    }

    public SubValue after(SuperValue ret, SubValue sup, SuperValue sub) {
       return new SubValue();
    }
}
