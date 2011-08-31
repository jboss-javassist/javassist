package testproxy;

interface Target4Super {
    int foo4(); 
}

public interface Target4 extends Target4Super {
    int bar4();
}
