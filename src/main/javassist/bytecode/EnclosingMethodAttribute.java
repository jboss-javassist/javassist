/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * <code>EnclosingMethod_attribute</code>.
 */
public class EnclosingMethodAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"EnclosingMethod"</code>.
     */
    public static final String tag = "EnclosingMethod";

    EnclosingMethodAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    /**
     * Constructs an EnclosingMethod attribute.
     *
     * @param cp                a constant pool table.
     * @param className         the name of the innermost enclosing class.
     * @param methodName        the name of the enclosing method.
     * @param methodDesc        the descriptor of the enclosing method.
     */
    public EnclosingMethodAttribute(ConstPool cp, String className,
                                    String methodName, String methodDesc) {
        super(cp, tag);
        int ci = cp.addClassInfo(className);
        int ni = cp.addNameAndTypeInfo(methodName, methodDesc);
        byte[] bvalue = new byte[4];
        bvalue[0] = (byte)(ci >>> 8);
        bvalue[1] = (byte)ci;
        bvalue[2] = (byte)(ni >>> 8);
        bvalue[3] = (byte)ni;
        set(bvalue);
    }

    /**
     * Constructs an EnclosingMethod attribute.
     * The value of <code>method_index</code> is set to 0.
     *
     * @param cp                a constant pool table.
     * @param className         the name of the innermost enclosing class.
     */
    public EnclosingMethodAttribute(ConstPool cp, String className) {
        super(cp, tag);
        int ci = cp.addClassInfo(className);
        int ni = 0;
        byte[] bvalue = new byte[4];
        bvalue[0] = (byte)(ci >>> 8);
        bvalue[1] = (byte)ci;
        bvalue[2] = (byte)(ni >>> 8);
        bvalue[3] = (byte)ni;
        set(bvalue);
    }

    /**
     * Returns the value of <code>class_index</code>.
     */
    public int classIndex() {
        return ByteArray.readU16bit(get(), 0);
    }

    /**
     * Returns the value of <code>method_index</code>.
     */
    public int methodIndex() {
        return ByteArray.readU16bit(get(), 2);
    }

    /**
     * Returns the name of the class specified by <code>class_index</code>.
     */
    public String className() {
        return getConstPool().getClassInfo(classIndex());
    }

    /**
     * Returns the method name specified by <code>method_index</code>.
     */
    public String methodName() {
        ConstPool cp = getConstPool();
        int mi = methodIndex();
        int ni = cp.getNameAndTypeName(mi);
        return cp.getUtf8Info(ni);
    }

    /**
     * Returns the method descriptor specified by <code>method_index</code>.
     */
    public String methodDescriptor() {
        ConstPool cp = getConstPool();
        int mi = methodIndex();
        int ti = cp.getNameAndTypeDescriptor(mi);
        return cp.getUtf8Info(ti);
    }

    /**
     * Makes a copy.  Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        pairs of replaced and substituted
     *                          class names.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        if (methodIndex() == 0) 
            return new EnclosingMethodAttribute(newCp, className());
        else
            return new EnclosingMethodAttribute(newCp, className(),
                                            methodName(), methodDescriptor());
    }
}
