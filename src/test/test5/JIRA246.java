package test5;

public class JIRA246 {
    public interface Test {
        default void defaultMethod() {
        }
        void test();
    }

    public interface IA {
        default int get() {
            return 0;
        }
    }

    public static class A implements IA {
        public int anotherGet() {
            return 1;
        }
    }
}
