package test4;

import java.util.function.Supplier;

public class InvokeDynCopySrc {
    public InvokeDynCopySrc() {
        System.out.println("source class:" + getString());
    }

    public String getString() {
        Supplier<String> stringSupplier = () -> {
            return "hello";
        };

        return stringSupplier.get();
    }
}
