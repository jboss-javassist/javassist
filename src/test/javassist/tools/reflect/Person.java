package javassist.tools.reflect;

import java.lang.reflect.Method;
import java.util.Arrays;

import junit.framework.Assert;

/**
 * Work with ClassMetaobjectTest.java
 *
 * @author Brett Randall
 */
public class Person {
    public String name;
    public static int birth = 3;
    public static final String defaultName = "John";

    public Person(String name, int birthYear) {
        if (name == null)
            this.name = defaultName;
        else
            this.name = name;

        Person.birth = birthYear;
    }

    public String getName() {
	return name;
    }

    public int getAge(int year) {
	return year - birth;
    }

    public static void main(String[] args) throws Exception {
        Person person = new Person("Bob", 10);
        
        Metalevel metalevel = (Metalevel) person;
        ClassMetaobject cmo = metalevel._getClass();
        
        // this should return the index of the original getAge method
        int methodIndex = cmo.getMethodIndex("getAge",
                                             new Class[] {Integer.TYPE});
        
        System.out.println(methodIndex);

        // and the name verified by getMethodName
        String methodName = cmo.getMethodName(methodIndex);
        System.out.println(methodName);
        
        // check the name
        Assert.assertEquals("Incorrect method was found",
                            "getAge", methodName);

        // check the arguments
        Method method = cmo.getReflectiveMethods()[methodIndex];
        Assert.assertTrue("Method signature did not match",
                          Arrays.equals(method.getParameterTypes(),
                                        new Class[] {Integer.TYPE}));
        System.out.println(method);
        System.out.println("OK");
    }
}

