package test3;

class NewExprTryCatch2 {
}

public class NewExprTryCatch {
    public void instrumentMe() {
        // we need a 'new' expression to instrument, the exact type is not important
        new Object();
        // if the try/catch block below is removed, the error does not occur
        try {
            System.out.println();
        } catch (Throwable t) {
        }
    }

    public void me2() throws Exception {
        // the error is somehow related to the string concatenation and local variables,
        // when the code below is replaced with something else, the error does not occur.
        String s1 = "a";
        @SuppressWarnings("unused")
        String s2 = s1 + "b";
    }

    public int test() throws Exception {
        instrumentMe();
        me2();
        return 0;
    }
}
