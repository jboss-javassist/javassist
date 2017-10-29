package javassist.tools.reflect;

import org.junit.Assert;

public class SubClass extends SuperClass {
    public String f() { return "f2"; }	// override
    public String i() { return "i"; }
    public final String j() { return "j"; }

    public static void main(String[] args) {
        SuperClass sup = new SuperClass();
        SubClass sub = new SubClass();
        String s = sup.f() + sup.g() + sup.h();
        String t = sub.f() + sub.g() + sub.h() + sub.i() + sub.j();
        System.out.println(s);
        System.out.println(t);
        Assert.assertEquals("fgh", s);
        Assert.assertEquals("f2ghij", t);
    }
}
