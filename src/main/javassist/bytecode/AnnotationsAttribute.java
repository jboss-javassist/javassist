/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2004 Shigeru Chiba. All Rights Reserved.
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

import java.util.Map;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.ByteArrayOutputStream;

import javassist.bytecode.annotation.*;

/**
 * A class representing
 * <code>RuntimeVisibleAnnotations_attribute</code> and
 * <code>RuntimeInvisibleAnnotations_attribute</code>.
 */
public class AnnotationsAttribute extends AttributeInfo {
    /**
     * The name of the <code>RuntimeVisibleAnnotations</code> attribute.
     */
    public static final String visibleTag = "RuntimeVisibleAnnotations";

    /**
     * The name of the <code>RuntimeInvisibleAnnotations</code> attribute.
     */
    public static final String invisibleTag = "RuntimeInvisibleAnnotations"; 

    /**
     * Constructs a <code>Runtime(In)VisisbleAnnotations_attribute</code>.
     *
     * @param cp            constant pool
     * @param attrname      attribute name (<code>visibleTag</code> or
     *                      <code>invisibleTag</code>).
     * @param info          the contents of this attribute.  It does not
     *                      include <code>attribute_name_index</code> or
     *                      <code>attribute_length</code>.
     */
    public AnnotationsAttribute(ConstPool cp, String attrname, byte[] info) {
        super(cp, attrname, info);
    }

    /**
     * Constructs an empty
     * <code>Runtime(In)VisisbleAnnotations_attribute</code>.
     *
     * @param cp            constant pool
     * @param attrname      attribute name (<code>visibleTag</code> or
     *                      <code>invisibleTag</code>).
     */
    public AnnotationsAttribute(ConstPool cp, String attrname) {
        this(cp, attrname, new byte[] { 0, 0 });
    }

    /**
     * @param n     the attribute name.
     */
    AnnotationsAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    /**
     * Returns <code>num_annotations</code>. 
     */
    public int numAnnotations() {
        return ByteArray.readU16bit(info, 0);
    }

    /**
     * Copies this attribute and returns a new copy.
     * This method works even if this object is an instance of
     * <code>ParameterAnnotationsAttribute</code>.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        return new Copier(newCp, classnames).copy(this);
    }

    AnnotationsAttribute makeCopy(ConstPool newCp, byte[] info) {
        return new AnnotationsAttribute(newCp, getName(), info);
    }

    /**
     * Runs the parser to analyze this annotation.
     * It invokes methods on the given visitor object while parsing.
     *
     * @see AnnotationsWriter
     */
    public void accept(AnnotationsVisitor visitor) throws Exception {
        int num = numAnnotations();
        int pos = 2;
        visitor.beginAnnotationsArray(num);
        for (int i = 0; i < num; ++i)
            pos = readAnnotation(visitor, pos);

        visitor.endAnnotationsArray();
    }

    int readAnnotation(AnnotationsVisitor visitor, int pos) throws Exception {
        int type = ByteArray.readU16bit(info, pos);
        int numPairs = ByteArray.readU16bit(info, pos + 2);
        visitor.beginAnnotation(constPool, type, numPairs);
        pos += 4;
        for (int j = 0; j < numPairs; ++j)
            pos = readMemberValuePair(visitor, pos);

        visitor.endAnnotation();
        return pos;
    }

    private int readMemberValuePair(AnnotationsVisitor visitor, int pos)
        throws Exception
    {
        int nameIndex = ByteArray.readU16bit(info, pos);
        visitor.beginMemberValuePair(constPool, nameIndex);
        pos = readMemberValue(visitor, pos + 2);
        visitor.endMemberValuePair();
        return pos;
    }

    private int readMemberValue(AnnotationsVisitor visitor, int pos)
        throws Exception
    {
        int tag = info[pos] & 0xff;
        if (tag == 'e') {
            int typeNameIndex = ByteArray.readU16bit(info, pos + 1);
            int constNameIndex = ByteArray.readU16bit(info, pos + 3);
            visitor.enumConstValue(constPool, typeNameIndex, constNameIndex);
            return pos + 5;
        }
        else if (tag == 'c') {
            int index = ByteArray.readU16bit(info, pos + 1);
            visitor.classInfoIndex(constPool, index);
            return pos + 3;
        }
        else if (tag == '@') {
            visitor.beginAnnotationValue();
            pos = readAnnotation(visitor, pos + 1);
            visitor.endAnnotationValue();
            return pos;
        }
        else if (tag == '[') {
            int num = ByteArray.readU16bit(info, pos + 1);
            pos += 3;
            visitor.beginArrayValue(num);
            for (int i = 0; i < num; ++i) {
                pos = readMemberValue(visitor, pos);
                visitor.arrayElement(i);
            }

            visitor.endArrayValue();
            return pos;
        }
        else { // primitive types or String.
            int index = ByteArray.readU16bit(info, pos + 1);
            visitor.constValueIndex(constPool, tag, index);
            return pos + 3;
        }
    }

    /**
     * A visitor for copying the contents of an
     * <code>AnnotationsAttribute</code>.
     *
     * <p>This class is typically used as following:
     * <ul><pre>
     * new Copier(dest, map).copy(src)
     * </pre></ul>
     *
     * <p>This expression returns a copy of the source annotations attribute.
     *
     * @see AnnotationsAttribute#accept(AnnotationsVisitor)
     * @see AnnotationsWriter
     */
    public static class Copier extends AnnotationsVisitor {
        protected ByteArrayOutputStream output; 
        protected AnnotationsWriter writer;
        protected ConstPool destPool;
        protected Map classnames;

        /**
         * Copies a constant pool entry into the destination constant pool
         * and returns the index of the copied entry.
         *
         * @param srcIndex      the index of the copied entry into the source
         *                      constant pool. 
         * @return the index of the copied item into the destination
         *          constant pool. 
         */
        protected int copy(ConstPool srcPool, int srcIndex) {
            return srcPool.copy(srcIndex, destPool, classnames);
        }

        /**
         * Constructs a copier.  This copier renames some class names
         * into the new names specified by <code>map</code> when it copies
         * an annotation attribute.
         *
         * @param src        the constant pool of the source class.
         * @param dest       the constant pool of the destination class.
         * @param map        pairs of replaced and substituted class names.
         *                   It can be null.
         */
        public Copier(ConstPool dest, Map map) {
            output = new ByteArrayOutputStream();
            writer = new AnnotationsWriter(output, dest);
            destPool = dest;
            classnames = map;
        }

        /**
         * Does copying.  This calls <code>accept()</code>
         * on <code>src</code> with this visitor object.
         *
         * @param src       the source attribute.  It can be an instance
         *                  of <code>ParameterAnnotationsAttribute</code>.
         * @return a copy of the source attribute.
         */
        public AnnotationsAttribute copy(AnnotationsAttribute src) {
            try {
                src.accept(this);
                writer.close();
            }
            catch (Exception e) {
                throw new RuntimeException(e.toString());
            }

            return src.makeCopy(destPool, output.toByteArray());
        }

        /**
         * Writes <code>num_parameters</code>.
         */
        public void beginParameters(int num) throws IOException {
            writer.numParameters(num);
        }

        /**
         * Does nothing.
         */
        public void endParameters() {}

        /**
         * Writes <code>num_annotations</code>.
         */
        public void beginAnnotationsArray(int num) throws IOException {
            writer.numAnnotations(num);
        }

        /**
         * Does nothing.
         */
        public void endAnnotationsArray() {}

        /**
         * Writes <code>type_index</code> and
         * <code>num_member_value_pairs</code>.
         */
        public void beginAnnotation(ConstPool cp,
                                    int typeIndex, int numMemberValuePairs)
            throws IOException
        {
            writer.annotation(copy(cp, typeIndex), numMemberValuePairs);
        }

        /**
         * Does nothing.
         */
        public void endAnnotation() {}

        /**
         * Writes <code>member_name_index</code>.
         */
        public void beginMemberValuePair(ConstPool cp, int memberNameIndex)
            throws IOException
        {
            writer.memberValuePair(copy(cp, memberNameIndex));
        }

        /**
         * Does nothing.
         */
        public void endMemberValuePair() {}

        /**
         * Writes <code>tag</code> and <code>const_value_index</code>.
         */
        public void constValueIndex(ConstPool cp, int tag, int index)
            throws IOException
        {
            writer.constValueIndex(tag, copy(cp, index));
        }

        /**
         * Writes <code>tag</code> and <code>enum_const_value</code>.
         */
        public void enumConstValue(ConstPool cp, int typeNameIndex,
                                   int constNameIndex)
            throws IOException
        {
            writer.enumConstValue(copy(cp, typeNameIndex),
                                  copy(cp, constNameIndex));
        }

        /**
         * Writes <code>tag</code> and <code>class_info_index</code>.
         */
        public void classInfoIndex(ConstPool cp, int index) throws IOException {
            writer.classInfoIndex(copy(cp, index));
        }

        /**
         * Writes <code>tag</code>.
         */
        public void beginAnnotationValue() throws IOException {
            writer.annotationValue();
        }

        /**
         * Does nothing.
         */
        public void endAnnotationValue() {}

        /**
         * Writes <code>num_values</code> in <code>array_value</code>.
         */
        public void beginArrayValue(int numValues) throws IOException {
            writer.arrayValue(numValues);
        }

        /**
         * Does nothing.
         */
        public void arrayElement(int i) {}

        /**
         * Invoked when the parser ends parsing <code>array_value</code>
         * in <code>member_value</code>.
         */
        public void endArrayValue() {}
    }

    /**
     * Returns a string representation of this object.
     */
    public String toString() {
        return getName() + ":" + new Printer().toString(this);
    }

    static class Printer extends AnnotationsVisitor {
        private StringBuffer sbuf;

        public Printer() {
            sbuf = new StringBuffer();
        }

        public String toString(AnnotationsAttribute src) {
            try {
                src.accept(this);
                return sbuf.toString();
            }
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new RuntimeException(e.toString());
            }
        }

        public void beginParameters(int num) {
            sbuf.append("parameters[").append(num).append("]{");
        }

        public void endParameters() {
            sbuf.append('}');
        }

        public void beginAnnotationsArray(int num) {
            sbuf.append("annotations[").append(num).append("]{");
        }

        public void endAnnotationsArray() {
            sbuf.append('}');
        }

        public void beginAnnotation(ConstPool cp,
                                    int typeIndex, int numMemberValuePairs) {
            String name = Descriptor.toClassName(cp.getUtf8Info(typeIndex));
            sbuf.append('@').append(name).append('{');
        }

        public void endAnnotation() {
            sbuf.append('}');
        }

        public void beginMemberValuePair(ConstPool cp, int memberNameIndex) {
            sbuf.append(cp.getUtf8Info(memberNameIndex)).append('=');
        }

        public void endMemberValuePair() {
            sbuf.append(", ");
        }

        public void constValueIndex(ConstPool cp, int tag, int index) {
            if (tag == 'Z' || tag == 'B' || tag == 'C' || tag == 'S'
                || tag == 'I')
                sbuf.append(cp.getIntegerInfo(index));
            else if (tag == 'J')
                sbuf.append(cp.getLongInfo(index));
            else if (tag == 'F')
                sbuf.append(cp.getFloatInfo(index));
            else if (tag == 'D')
                sbuf.append(cp.getDoubleInfo(index));
            else if (tag == 's')
                sbuf.append('"').append(cp.getUtf8Info(index)).append('"');
            else
                throw new RuntimeException("unknown tag:" + tag );
        }

        public void enumConstValue(ConstPool cp, int typeNameIndex,
                                   int constNameIndex) {
            String name
                = Descriptor.toClassName(cp.getUtf8Info(typeNameIndex));
            sbuf.append(name)
                .append('.').append(cp.getUtf8Info(constNameIndex));
        }

        public void classInfoIndex(ConstPool cp, int index)
            throws IOException
        {
            sbuf.append(Descriptor.toClassName(cp.getUtf8Info(index)))
                .append(" class");
        }

        public void beginAnnotationValue() {}

        public void endAnnotationValue() {}

        public void beginArrayValue(int numValues) {
            sbuf.append("array[").append(numValues).append("]{");
        }

        public void arrayElement(int i) {
            sbuf.append(", ");
        }

        public void endArrayValue() {
            sbuf.append('}');
        }
    }

}
