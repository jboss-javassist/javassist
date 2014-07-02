package test4;


import java.util.function.IntConsumer;
import java.util.stream.IntStream;

class JIRA220impl implements IntConsumer {
    @Override
    public void accept(int value) {
        // Do something
    }
}

public class JIRA220 {
    public void foo() {
        JIRA220impl impl = new JIRA220impl();
        IntStream.range(0, 5).forEach(impl);
    }
}
