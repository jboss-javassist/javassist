package test5;

import java.util.ArrayList;

class TypeAnnoSuper {}
interface TypeAnnoI {}

@TypeAnnoA
public class TypeAnno<@TypeAnnoA TT extends @TypeAnnoA String> extends @TypeAnnoA TypeAnnoSuper implements @TypeAnnoA TypeAnnoI {
    public @TypeAnnoA String foo(@TypeAnnoA int i) throws @TypeAnnoA Exception {
        @TypeAnnoA String s = new @TypeAnnoA String("bar ");
        Object t = s;
        String ss = (@TypeAnnoA String)t;
        ArrayList<@TypeAnnoA String> list = new ArrayList<@TypeAnnoA String>();
        if (list instanceof  /* @TypeAnnoA */ java.util.List)
            System.out.println("ok");

        try {
            list.add(ss);
        } catch (@TypeAnnoA RuntimeException e) {}
        return "foo" + list.get(0) + i;
    }

    @TypeAnnoA double dvalue;
    @TypeAnnoA int ivalue @TypeAnnoA [] @TypeAnnoA [] @TypeAnnoA [];

    <@TypeAnnoA T extends @TypeAnnoA String> T bar(T t) { return t; }

    @TypeAnnoA TT getNull() { return null; }
}
