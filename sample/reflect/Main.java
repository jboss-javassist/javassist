package sample.reflect;

import javassist.tools.reflect.Loader;

/*
  The "verbose metaobject" example (JDK 1.2 or later only).

  Since this program registers class Person as a reflective class
  (in a more realistic demonstration, what classes are reflective
  would be specified by some configuration file), the class loader
  modifies Person.class when loading into the JVM so that the class
  Person is changed into a reflective class and a Person object is
  controlled by a VerboseMetaobj.

  To run,

  % java javassist.tools.reflect.Loader sample.reflect.Main Joe

  Compare this result with that of the regular execution without reflection:

  % java sample.reflect.Person Joe
*/
public class Main {
    public static void main(String[] args) throws Throwable {
        Loader cl = (Loader)Main.class.getClassLoader();
        cl.makeReflective("sample.reflect.Person",
                          "sample.reflect.VerboseMetaobj",
                          "javassist.tools.reflect.ClassMetaobject");

        cl.run("sample.reflect.Person", args);
    }
}
