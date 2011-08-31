package test2;

public class AddCatchForConstructor {
    int i;
    public AddCatchForConstructor() {
        this(3);
        i++;
    }

    public AddCatchForConstructor(int k) {
        i = k; 
    }
}
