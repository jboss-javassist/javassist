package annotation;

@interface Id {
    int id();
}

enum EnumTest {
    A, B, C
}

@interface Tag {
    boolean z();
    byte b();
    char c();
    short s();
    int i();
    long j();
    float f();
    double d();
    String string();
    Class<? extends Object> integer();
    EnumTest enumtest();
    String[] array();
    Id annotation();
}

@Tag(z = true, b = 1, c = 'a', s = 2, i = 3, j = 4L, f = 5.0F, d = 5.0,
     string = "abc",
     enumtest = EnumTest.A,
     integer = Integer.class,
     array = { "p", "q", "r" },
     annotation = @Id(id = 20))
public class Test {
    public int test() { return 0; }
}
