package test4;

import java.util.List;

enum GetAllRefEnum { A, B };

@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@interface GetAllRefAnno {
    GetAllRefEnum getA();
    Class getC();
}

@interface GetAllRefAnnoC {}

@interface GetAllRefAnnoC2 {}

@interface GetAllRefAnnoC3 {}

@interface GetAllRefAnnoC4 {}

@GetAllRefAnno(getA = GetAllRefEnum.A, getC = String.class)
public class GetAllRef {
}

@GetAllRefAnno(getA = GetAllRefEnum.A, getC = String.class)
class GetAllRefB {
}

@GetAllRefAnno(getA = GetAllRefEnum.A, getC = String.class)
class GetAllRefC {
    void bar(@GetAllRefAnnoC3 int i, int j,
             @GetAllRefAnnoC2 @GetAllRefAnnoC4 boolean b) {}
    @GetAllRefAnnoC void foo() {}
    @GetAllRefAnnoC2 int value;
}

@GetAllRefAnno(getA = GetAllRefEnum.A, getC = String.class)
interface GetAllRefD {
    void bar(@GetAllRefAnnoC3 int i, int j,
             @GetAllRefAnnoC2 @GetAllRefAnnoC4 boolean b);
    @GetAllRefAnnoC
    List<GetAllRefC> foo();
}
