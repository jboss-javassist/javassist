/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.bytecode.annotation;

import java.io.*;

import javassist.bytecode.ByteArray;
import javassist.bytecode.ConstPool;

/**
 * A convenience class for constructing a
 * <code>..Annotations_attribute</code>.
 * See the source code of the <code>AnnotationsAttribute.Copier</code> class.
 *
 * <p>The following code snippet is an example of use of this class:
 *
 * <ul><pre>
 * ConstPool pool = ...;
 * output = new ByteArrayOutputStream();
 * writer = new AnnotationsWriter(output, pool);
 *
 * writer.numAnnotations(1);
 * writer.annotation("Author", 2);
 * writer.memberValuePair("name");
 * writer.constValueIndex("chiba");
 * writer.memberValuePair("address");
 * writer.constValueIndex("tokyo");
 *
 * writer.close();
 * byte[] attribute_info = output.toByteArray();
 * AnnotationsAttribute anno
 *     = new AnnotationsAttribute(pool, AnnotationsAttribute.visibleTag,
 *                                attribute_info);
 * </pre></ul>
 *
 * <p>The code snippet above generates the annotation attribute
 * corresponding to this annotation:
 *
 * <ul><pre>
 * &nbsp;@Author(name = "chiba", address = "tokyo")
 * </pre></ul>
 *
 * @see javassist.bytecode.AnnotationsAttribute
 * @see javassist.bytecode.ParameterAnnotationsAttribute
 */
public class AnnotationsWriter {
    private OutputStream output;
    private ConstPool pool;

    /**
     * Constructs with the given output stream.
     *
     * @param os    the output stream.
     * @param cp    the constant pool.
     */
    public AnnotationsWriter(OutputStream os, ConstPool cp) {
        output = os;
        pool = cp;
    }

    /**
     * Obtains the constant pool given to the constructor.
     */
    public ConstPool getConstPool() {
        return pool;
    }

    /**
     * Closes the output stream.
     *
     */
    public void close() throws IOException {
        output.close();
    }

    /**
     * Writes <code>num_parameters</code> in
     * <code>Runtime(In)VisibleParameterAnnotations_attribute</code>.
     * This method must be followed by <code>num</code> calls to
     * <code>numAnnotations()</code>.
     */
    public void numParameters(int num) throws IOException {
        output.write(num);
    }

    /**
     * Writes <code>num_annotations</code> in
     * <code>Runtime(In)VisibleAnnotations_attribute</code>.
     * This method must be followed by <code>num</code> calls to
     * <code>annotation()</code>.
     */
    public void numAnnotations(int num) throws IOException {
        write16bit(num);
    }

    /**
     * Writes <code>annotation</code>.
     * This method must be followed by <code>numMemberValuePairs</code>
     * calls to <code>memberValuePair()</code>.
     *
     * @param type                  the annotation interface name.
     * @param numMemberValuePairs   <code>num_member_value_pairs</code>
     *                              in <code>annotation</code>.
     */
    public void annotation(String type, int numMemberValuePairs)
        throws IOException
    {
        annotation(pool.addUtf8Info(type), numMemberValuePairs);
    }

    /**
     * Writes <code>annotation</code>.
     * This method must be followed by <code>numMemberValuePairs</code>
     * calls to <code>memberValuePair()</code>.
     *
     * @param typeIndex  <code>type_index</code> in <code>annotation</code>.
     * @param numMemberValuePairs   <code>num_member_value_pairs</code>
     *                              in <code>annotation</code>.
     */
    public void annotation(int typeIndex, int numMemberValuePairs)
        throws IOException
    {
        write16bit(typeIndex);
        write16bit(numMemberValuePairs);
    }

    /**
     * Writes an element of a <code>member_value_pairs</code> array
     * in <code>annotation</code>.
     * This method must be followed by a
     * call to <code>constValueIndex()</code>, <code>enumConstValue()</code>,
     * etc.
     *
     * @param memberName        the name of the annotation type member.
     */
    public void memberValuePair(String memberName) throws IOException {
        memberValuePair(pool.addUtf8Info(memberName));
    }

    /**
     * Writes an element of a <code>member_value_pairs</code> array
     * in <code>annotation</code>.
     * This method must be followed by a
     * call to <code>constValueIndex()</code>, <code>enumConstValue()</code>,
     * etc.
     *
     * @param memberNameIndex   <code>member_name_index</code>
     *                          in <code>member_value_pairs</code> array.
     */
    public void memberValuePair(int memberNameIndex) throws IOException {
        write16bit(memberNameIndex);
    }

    /**
     * Writes <code>tag</code> and <code>const_value_index</code> 
     * in <code>member_value</code>.
     *
     * @param value     the constant value.
     */
    public void constValueIndex(boolean value) throws IOException {
        constValueIndex('Z', pool.addIntegerInfo(value ? 1 : 0));
    }

    /**
     * Writes <code>tag</code> and <code>const_value_index</code> 
     * in <code>member_value</code>.
     *
     * @param value     the constant value.
     */
    public void constValueIndex(byte value) throws IOException {
        constValueIndex('B', pool.addIntegerInfo(value));
    }

    /**
     * Writes <code>tag</code> and <code>const_value_index</code> 
     * in <code>member_value</code>.
     *
     * @param value     the constant value.
     */
    public void constValueIndex(char value) throws IOException {
        constValueIndex('C', pool.addIntegerInfo(value));
    }

    /**
     * Writes <code>tag</code> and <code>const_value_index</code> 
     * in <code>member_value</code>.
     *
     * @param value     the constant value.
     */
    public void constValueIndex(short value) throws IOException {
        constValueIndex('S', pool.addIntegerInfo(value));
    }

    /**
     * Writes <code>tag</code> and <code>const_value_index</code> 
     * in <code>member_value</code>.
     *
     * @param value     the constant value.
     */
    public void constValueIndex(int value) throws IOException {
        constValueIndex('I', pool.addIntegerInfo(value));
    }

    /**
     * Writes <code>tag</code> and <code>const_value_index</code> 
     * in <code>member_value</code>.
     *
     * @param value     the constant value.
     */
    public void constValueIndex(long value) throws IOException {
        constValueIndex('J', pool.addLongInfo(value));
    }

    /**
     * Writes <code>tag</code> and <code>const_value_index</code> 
     * in <code>member_value</code>.
     *
     * @param value     the constant value.
     */
    public void constValueIndex(float value) throws IOException {
        constValueIndex('F', pool.addFloatInfo(value));
    }

    /**
     * Writes <code>tag</code> and <code>const_value_index</code> 
     * in <code>member_value</code>.
     *
     * @param value     the constant value.
     */
    public void constValueIndex(double value) throws IOException {
        constValueIndex('D', pool.addDoubleInfo(value));
    }

    /**
     * Writes <code>tag</code> and <code>const_value_index</code> 
     * in <code>member_value</code>.
     *
     * @param value     the constant value.
     */
    public void constValueIndex(String value) throws IOException {
        constValueIndex('s', pool.addUtf8Info(value));
    }

    /**
     * Writes <code>tag</code> and <code>const_value_index</code> 
     * in <code>member_value</code>.
     *
     * @param tag       <code>tag</code> in <code>member_value</code>.
     * @param index     <code>const_value_index</code>
     *                              in <code>member_value</code>.
     */
    public void constValueIndex(int tag, int index)
        throws IOException
    {
        output.write(tag);
        write16bit(index);
    }

    /**
     * Writes <code>tag</code> and <code>enum_const_value</code> 
     * in <code>member_value</code>.
     *
     * @param typeName      the type name of the enum constant.
     * @param constName     the simple name of the enum constant.
     */
    public void enumConstValue(String typeName, String constName)
        throws IOException
    {
        enumConstValue(pool.addUtf8Info(typeName),
                       pool.addUtf8Info(constName));
    }

    /**
     * Writes <code>tag</code> and <code>enum_const_value</code> 
     * in <code>member_value</code>.
     *
     * @param typeNameIndex       <code>type_name_index</code>
     *                              in <code>member_value</code>.
     * @param constNameIndex     <code>const_name_index</code>
     *                              in <code>member_value</code>.
     */
    public void enumConstValue(int typeNameIndex, int constNameIndex)
        throws IOException
    {
        output.write('e');
        write16bit(typeNameIndex);
        write16bit(constNameIndex);
    }

    /**
     * Writes <code>tag</code> and <code>class_info_index</code> 
     * in <code>member_value</code>.
     *
     * @param name      the class name.
     */
    public void classInfoIndex(String name) throws IOException {
        classInfoIndex(pool.addUtf8Info(name));
    }

    /**
     * Writes <code>tag</code> and <code>class_info_index</code> 
     * in <code>member_value</code>.
     *
     * @param index       <code>class_info_index</code>
     */
    public void classInfoIndex(int index) throws IOException {
        output.write('c');
        write16bit(index);
    }

    /**
     * Writes <code>tag</code> and <code>annotation_value</code> 
     * in <code>member_value</code>.
     * This method must be followed by a call to <code>annotation()</code>.
     */
    public void annotationValue() throws IOException {
        output.write('@');
    }

    /**
     * Writes <code>tag</code> and <code>array_value</code> 
     * in <code>member_value</code>.
     * This method must be followed by <code>numValues</code> calls
     * to <code>constValueIndex()</code>, <code>enumConstValue()</code>,
     * etc.
     *
     * @param numValues     <code>num_values</code>
     *                      in <code>array_value</code>.
     */
    public void arrayValue(int numValues) throws IOException {
        output.write('[');
        write16bit(numValues);
    }

    private void write16bit(int value) throws IOException {
        byte[] buf = new byte[2];
        ByteArray.write16bit(value, buf, 0);
        output.write(buf);
    }
}
