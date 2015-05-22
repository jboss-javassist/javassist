/*
 * A base-level class controlled by VerboseMetaobj.
 */

package sample.reflect;

import javassist.tools.reflect.Metalevel;
import javassist.tools.reflect.Metaobject;

public class Person {
    public String name;

    public static int birth = 3;

    public static final String defaultName = "John";

    public Person(String name, int birthYear) {
        if (name == null)
            this.name = defaultName;
        else
            this.name = name;

        birth = birthYear;
    }

    public String getName() {
        return name;
    }

    public int getAge(int year) {
        return year - birth;
    }

    public static void main(String[] args) {
        String name;
        if (args.length > 0)
            name = args[0];
        else
            name = "Bill";

        Person p = new Person(name, 1960);
        System.out.println("name: " + p.getName());
        System.out.println("object: " + p.toString());

        // change the metaobject of p.
        if (p instanceof Metalevel) {
            ((Metalevel)p)._setMetaobject(new Metaobject(p, null));
            System.out.println("<< the metaobject was changed.>>");
        }

        System.out.println("age: " + p.getAge(1999));
    }
}
