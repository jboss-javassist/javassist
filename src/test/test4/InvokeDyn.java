package test4;

import java.lang.invoke.*;

public class InvokeDyn {
    public static int test9(int i, String s) { return 9; }

    public static CallSite boot(MethodHandles.Lookup caller, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class thisClass = lookup.lookupClass();
        MethodHandle method = lookup.findStatic(thisClass, "test9", MethodType.methodType(int.class, int.class, String.class));
        return new ConstantCallSite(method);
    }
}
