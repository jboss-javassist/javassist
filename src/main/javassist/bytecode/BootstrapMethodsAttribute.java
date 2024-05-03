package javassist.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class BootstrapMethodsAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"BootstrapMethods"</code>.
     */
    public static final String tag = "BootstrapMethods";

    /**
     * An element of <code>bootstrap_methods</code>.
     */
    public static class BootstrapMethod {
        /**
         * Constructs an element of <code>bootstrap_methods</code>.
         *
         * @param method        <code>bootstrap_method_ref</code>.
         * @param args          <code>bootstrap_arguments</code>.
         */
        public BootstrapMethod(int method, int[] args) {
            methodRef = method;
            arguments = args;
        }

        /**
         * <code>bootstrap_method_ref</code>.
         * The value at this index must be a <code>CONSTANT_MethodHandle_info</code>.
         */
        public int methodRef;

        /**
         * <code>bootstrap_arguments</code>.
         */
        public int[] arguments;

        /**
         * Makes a copy.  Class names are replaced according to the
         *          * given <code>Map</code> object.
         *
         * @param srcCp     the constant pool table from the source
         * @param destCp    the constant pool table used bt new copy
         * @param classnames    pairs of replaced and substituted class names.
         *
         * @return new BootstrapMethod
         */
        protected BootstrapMethod copy(ConstPool srcCp, ConstPool destCp, Map<String,String> classnames) {
            int newMethodRef = srcCp.copy(methodRef, destCp, classnames);
            int[] newArguments = new int[arguments.length];

            for (int i = 0; i < arguments.length; i++)
                newArguments[i] = srcCp.copy(arguments[i], destCp, classnames);

            return new BootstrapMethod(newMethodRef, newArguments);
        }
    }

    BootstrapMethodsAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    /**
     * Constructs a BootstrapMethods attribute.
     *
     * @param cp                a constant pool table.
     * @param methods           the contents.
     */
    public BootstrapMethodsAttribute(ConstPool cp, BootstrapMethod[] methods) {
        super(cp, tag);

        set(convertMethodsToBytes(methods));
    }

    /**
     * Obtains <code>bootstrap_methods</code> in this attribute.
     *
     * @return an array of <code>BootstrapMethod</code>.  Since it
     *          is a fresh copy, modifying the returned array does not
     *          affect the original contents of this attribute.
     */
    public BootstrapMethod[] getMethods() {
        byte[] data = this.get();
        int num = ByteArray.readU16bit(data, 0);
        BootstrapMethod[] methods = new BootstrapMethod[num];
        int pos = 2;
        for (int i = 0; i < num; i++) {
            int ref = ByteArray.readU16bit(data, pos);
            int len = ByteArray.readU16bit(data, pos + 2);
            int[] args = new int[len];
            pos += 4;
            for (int k = 0; k < len; k++) {
                args[k] = ByteArray.readU16bit(data, pos);
                pos += 2;
            }

            methods[i] = new BootstrapMethod(ref, args);
        }

        return methods;
    }

    /**
     * Makes a copy.  Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        pairs of replaced and substituted
     *                          class names.
     */
    @Override
    public AttributeInfo copy(ConstPool newCp, Map<String,String> classnames) {
        BootstrapMethod[] methods = getMethods();
        ConstPool thisCp = getConstPool();
        for (int i = 0; i < methods.length; i++) {
            methods[i] = methods[i].copy(thisCp, newCp, classnames);
        }

        return new BootstrapMethodsAttribute(newCp, methods);
    }

    /**
     * add bootstrap method from given <code>ConstPool</code> and <code>BootstrapMethod</code>,
     * and add it to the specified index. Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * <p>
     *      if the index less than 0 or large than the origin method length, then throw <code>RuntimeException</code>;<br>
     *      if the index large or equals to 0 and less or equals to the origin method length,
     *          then replace the origin method with the new <code>BootstrapMethod srcBm</code> ;<br>
     *      if the index equals to the origin method length, then append the new <code>BootstrapMethod srcBm</code> at
     *          the origin methods tail.
     * </p>
     *
     * @param srcCp     the constant pool table of source.
     * @param srcBm     the bootstrap method of source
     * @param index     the new method index on bootstrap methods
     * @param classnames        pairs of replaced and substituted
     *                          class names.
     */
    public void addMethod(ConstPool srcCp, BootstrapMethod srcBm, int index, Map<String,String> classnames) {
        BootstrapMethod[] methods = getMethods();

        if (index < 0 || index > methods.length) {
            throw new RuntimeException("index out of range");
        }

        if (index == methods.length) {
            BootstrapMethod[] newBmArray = new BootstrapMethod[methods.length + 1];
            System.arraycopy(methods, 0, newBmArray, 0, methods.length);
            methods = newBmArray;
        }

        methods[index] = srcBm.copy(srcCp, getConstPool(), classnames);
        set(convertMethodsToBytes(methods));
    }

    private static byte[] convertMethodsToBytes(BootstrapMethod[] methods) {
        int size = 2;
        for (int i = 0; i < methods.length; i++)
            size += 4 + methods[i].arguments.length * 2;

        byte[] data = new byte[size];
        ByteArray.write16bit(methods.length, data, 0);    // num_bootstrap_methods
        int pos = 2;
        for (int i = 0; i < methods.length; i++) {
            ByteArray.write16bit(methods[i].methodRef, data, pos);
            ByteArray.write16bit(methods[i].arguments.length, data, pos + 2);
            int[] args = methods[i].arguments;
            pos += 4;
            for (int k = 0; k < args.length; k++) {
                ByteArray.write16bit(args[k], data, pos);
                pos += 2;
            }
        }

        return data;
    }
}
