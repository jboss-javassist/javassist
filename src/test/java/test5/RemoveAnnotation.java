package test5;

@interface RemoveAnno1 {}

@interface RemoveAnno2 {
    int foo() default 3;
}

@RemoveAnno1 public class RemoveAnnotation {
    @RemoveAnno1 @RemoveAnno2(foo=4)
    int foo() { return 1; }

    @RemoveAnno2
    int bar() { return 2; }

    @RemoveAnno1
    int baz = 10;

    public int run() { return foo() + bar(); }
}
