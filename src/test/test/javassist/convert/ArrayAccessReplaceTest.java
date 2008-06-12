package test.javassist.convert;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import javassist.ClassPool;
import javassist.CodeConverter;
import javassist.CtClass;
import junit.framework.TestCase;

public class ArrayAccessReplaceTest extends TestCase {
    private static SimpleInterface simple;

    public void setUp() throws Exception {
        ClassPool pool = new ClassPool(true);
        CtClass echoClass = pool.get(ArrayAccessReplaceTest.class.getName() + "$Echo");
        CtClass simpleClass = pool.get(ArrayAccessReplaceTest.class.getName() + "$Simple");
        CodeConverter converter = new CodeConverter();
        converter.replaceArrayAccess(echoClass, new CodeConverter.DefaultArrayAccessReplacementMethodNames());
        simpleClass.instrument(converter);
        //simpleClass.writeFile("/tmp");
        simple = (SimpleInterface) simpleClass.toClass(new URLClassLoader(new URL[0], getClass().getClassLoader()), Class.class.getProtectionDomain()).newInstance();
    }

    public void testComplex() throws Exception {
        ClassPool pool = new ClassPool(true);
        CtClass clazz = pool.get(ArrayAccessReplaceTest.class.getName() + "$Complex");

        CodeConverter converter = new CodeConverter();
        converter.replaceArrayAccess(clazz, new CodeConverter.DefaultArrayAccessReplacementMethodNames());
        clazz.instrument(converter);
        ComplexInterface instance = (ComplexInterface) clazz.toClass(new URLClassLoader(new URL[0], getClass().getClassLoader()), Class.class.getProtectionDomain()).newInstance();
        assertEquals(new Integer(5), instance.complexRead(4));
    }

    public void testBoolean() throws Exception {
        for (int i = 0; i < 100; i++) {
            boolean value = i % 5 == 0;
            simple.setBoolean(i, value);
        }

        for (int i = 0; i < 100; i++) {
            boolean value = i % 5 == 0;
            assertEquals(value, simple.getBoolean(i));
        }
    }

    public void testByte() throws Exception {
        for (byte i = 0; i < 100; i++) {
            simple.setByte(i, i);
        }

        for (byte i = 0; i < 100; i++) {
            assertEquals(i, simple.getByte(i));
        }
    }

    public void testShort() throws Exception {
        for (short i = 0; i < 100; i++) {
            simple.setShort(i, i);
        }

        for (short i = 0; i < 100; i++) {
            assertEquals(i, simple.getShort(i));
        }
    }

    public void testChar() throws Exception {
        for (char i = 0; i < 100; i++) {
            simple.setChar(i, i);
        }

        for (char i = 0; i < 100; i++) {
            assertEquals(i, simple.getChar(i));
        }
    }

    public void testInt() throws Exception {
        for (int i = 0; i < 100; i++) {
            simple.setInt(i, i);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(i, simple.getInt(i));
        }
    }

    public void testLong() throws Exception {
        for (int i = 0; i < 100; i++) {
            simple.setLong(i, i);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(i, simple.getLong(i));
        }
    }

    public void testFloat() throws Exception {
        for (int i = 0; i < 100; i++) {
            simple.setFloat(i, i);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals((float)i, simple.getFloat(i), 0);
        }
    }

    public void testDouble() throws Exception {
        for (int i = 0; i < 100; i++) {
            simple.setDouble(i, i);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals((double)i, simple.getDouble(i), 0);
        }
    }

    public void testObject() throws Exception {
        for (int i = 0; i < 100; i++) {
            simple.setObject(i, new Integer(i));
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(new Integer(i), simple.getObject(i));
        }
    }

    public void testFoo() throws Exception {
        for (int i = 0; i < 100; i++) {
            simple.setFoo(i, new Foo(i));
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(new Foo(i), simple.getFoo(i));
        }
    }

    public void testMulti() throws Exception {
        for (int i = 2; i < 100; i++) {
            simple.setMultiFoo(0, 1, i, new Foo(i));
        }

        for (int i = 2; i < 100; i++) {
            assertEquals(new Foo(i), simple.getMultiFoo(0, 1, i));
        }
    }

    public static class Echo {
        public static Map byteMap = new HashMap();
        public static Map charMap = new HashMap();
        public static Map doubleMap = new HashMap();
        public static Map floatMap = new HashMap();
        public static Map intMap = new HashMap();
        public static Map longMap = new HashMap();
        public static Map objectMap = new HashMap();
        public static Map shortMap = new HashMap();

        public static Object arrayReadObject(Object array, int index) {
            return objectMap.get(new Integer(index));
        }

        public static void arrayWriteObject(Object array, int index, Object element) {
            objectMap.put(new Integer(index), element);
        }

        public static byte arrayReadByteOrBoolean(Object array, int index) {
            return ((Byte)byteMap.get(new Integer(index))).byteValue();
        }

        public static void arrayWriteByteOrBoolean(Object array, int index, byte element) {
            byteMap.put(new Integer(index), new Byte(element));
        }

        public static char arrayReadChar(Object array, int index) {
            return ((Character)charMap.get(new Integer(index))).charValue();
        }

        public static void arrayWriteChar(Object array, int index, char element) {
            charMap.put(new Integer(index), new Character(element));
        }

        public static double arrayReadDouble(Object array, int index) {
            return ((Double)doubleMap.get(new Integer(index))).doubleValue();
        }

        public static void arrayWriteDouble(Object array, int index, double element) {
            doubleMap.put(new Integer(index), new Double(element));
        }

        public static float arrayReadFloat(Object array, int index) {
            return ((Float)floatMap.get(new Integer(index))).floatValue();
        }

        public static void arrayWriteFloat(Object array, int index, float element) {
            floatMap.put(new Integer(index), new Float(element));
        }

        public static int arrayReadInt(Object array, int index) {
            return ((Integer)intMap.get(new Integer(index))).intValue();
        }

        public static void arrayWriteInt(Object array, int index, int element) {
            intMap.put(new Integer(index), new Integer(element));
        }

        public static long arrayReadLong(Object array, int index) {
            return ((Long)longMap.get(new Integer(index))).longValue();
        }

        public static void arrayWriteLong(Object array, int index, long element) {
            longMap.put(new Integer(index), new Long(element));
        }

        public static short arrayReadShort(Object array, int index) {
            return ((Short)shortMap.get(new Integer(index))).shortValue();
        }

        public static void arrayWriteShort(Object array, int index, short element) {
            shortMap.put(new Integer(index), new Short(element));
        }
    }

    public static class Foo {
        public int bar;

        public Foo(int bar) {
            this.bar = bar;
        }

        public int hashCode() {
            return bar;
        }

        public boolean equals(Object o) {
            if (! (o instanceof Foo))
                return false;

            return ((Foo)o).bar == bar;
        }
    }

    public static interface SimpleInterface {
        public void setBoolean(int pos, boolean value);
        public boolean getBoolean(int pos);

        public void setByte(int pos, byte value);
        public byte getByte(int pos);

        public void setShort(int pos, short value);
        public short getShort(int pos);

        public void setChar(int pos, char value);
        public char getChar(int pos);

        public void setInt(int pos, int value);
        public int getInt(int pos);

        public void setLong(int pos, long value);
        public long getLong(int pos);

        public void setFloat(int pos, float value);
        public float getFloat(int pos);

        public void setDouble(int pos, double value);
        public double getDouble(int pos);

        public void setObject(int pos, Object value);
        public Object getObject(int pos);

        public void setFoo(int pos, Foo value);
        public Foo getFoo(int pos);

        public void setMultiFoo(int one, int two, int three, Foo foo);
        public Foo getMultiFoo(int one, int two, int three);
    }

    public static class Simple implements SimpleInterface {
        private boolean[] booleans;
        private byte[] bytes;
        private short[] shorts;
        private char[] chars;
        private int[] ints;
        private long[] longs;
        private float[] floats;
        private double[] doubles;
        private Object[] objects;
        private Foo[] foos;
        private Foo[][][] multi;

        public Simple() {
           multi[0] = new Foo[0][0];
           multi[0][1] = new Foo[0];
        }

        public boolean getBoolean(int pos) {
            return booleans[pos];
        }

        public byte getByte(int pos) {
            return bytes[pos];
        }

        public char getChar(int pos) {
            return chars[pos];
        }

        public double getDouble(int pos) {
            return doubles[pos];
        }

        public float getFloat(int pos) {
            return floats[pos];
        }

        public Foo getFoo(int pos) {
            return foos[pos];
        }

        public int getInt(int pos) {
            return ints[pos];
        }

        public long getLong(int pos) {
            return longs[pos];
        }

        public Object getObject(int pos) {
            return objects[pos];
        }

        public short getShort(int pos) {
            return shorts[pos];
        }

        public Foo getMultiFoo(int one, int two, int three) {
            return multi[one][two][three];
        }

        public void setBoolean(int pos, boolean value) {
            booleans[pos] = value;
        }

        public void setByte(int pos, byte value) {
            bytes[pos] = value;
        }

        public void setChar(int pos, char value) {
            chars[pos] = value;
        }

        public void setDouble(int pos, double value) {
            doubles[pos] = value;
        }

        public void setFloat(int pos, float value) {
            floats[pos] = value;
        }

        public void setFoo(int pos, Foo value) {
            foos[pos] = value;
        }

        public void setInt(int pos, int value) {
            ints[pos] = value;
        }

        public void setLong(int pos, long value) {
            longs[pos] = value;
        }

        public void setObject(int pos, Object value) {
            objects[pos] = value;
        }

        public void setShort(int pos, short value) {
            shorts[pos] = value;
        }

        public void setMultiFoo(int one, int two, int three, Foo foo) {
            multi[one][two][three] = foo;
        }
    }

    public static interface ComplexInterface {
        public Number complexRead(int x);
    }

    public static class Complex implements ComplexInterface {
        private Integer[] nums;
        private Long[] longNums;
        private static Integer justRead;

        public static Object arrayReadObject(Object array, int offset) {
            return new Integer(justRead.intValue() + offset);
        }

        public static void arrayWriteObject(Object array, int offset, Object element) {
            justRead = (Integer) element;
        }

        public Object getInteger(int i) {
            return (Object) new Integer(i);
        }

        public Number complexRead(int x) {
            Number[] ns = null;
            Number n1, n2, n3, n4;
            try {
                ((Object[])ns)[1] = getInteger(x);
                // We have to throw an error since we can't intercept
                // a guaranteed null array read yet (likely never will be able to)
                throw new Error("hi");
            } catch (Error error) {
                ns = nums;
            } catch (Exception exception) {
                ns = longNums;
            } finally {
                n1 = ns[1];
                n2 = ns[2];
                n3 = ns[3];
                n4 = ns[4];

                n2.intValue();
                n3.intValue();
                n4.intValue();
            }

            return n1;
        }
    }
}