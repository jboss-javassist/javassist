package test1;

import java.io.*;

/**
 * 
 * @author Bob Lee
 */
public class MySerializableClass implements Serializable, Cloneable {
    String fieldA;
    String fieldB;

    public MySerializableClass() { fieldA = null; }

    public MySerializableClass(String k) { fieldA = k; }

    public MySerializableClass(int k) { fieldA = null; }

    public String getFieldA() {
        return fieldB;
    }

    public String getFieldA(int i) { return fieldB; }

    public String getFieldA(int i, int j) { return fieldB; }

    public String getFieldB() {
        return fieldB;
    }

    public void doSomething(Object o) {
    }
    
}
