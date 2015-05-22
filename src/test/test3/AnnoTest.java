package test3;

@Anno()
public class AnnoTest {}

@Anno(c='a', bool=false, b=11, s=12, i=13, j=14L, f=15.0F, d=16.0,
      str="17", clazz=String.class, anno2=@Anno2(i={11, 12, 13}))
class AnnoTest2 {}

@Anno() @Anno2(i={1})
class AnnoTest3 {}

@Anno() @Anno2() @Anno3()
class AnnoTest4 {}

class AnnoTest5 {
    @Anno()
    void foo() {}

    @Anno2() int bar;
}