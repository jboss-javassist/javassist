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

package javassist.bytecode.annotation;

import javassist.bytecode.ConstPool;

/**
 * Visitor for parsing an annotations attribute.
 *
 * @see AnnotationsAttribute#accept(AnnotationsVisitor)
 * @see ParameterAnnotationsAttribute#accept(AnnotationsVisitor)
 */
public class AnnotationsVisitor {
    /**
     * Invoked when the parser starts parsing a
     * <code>parameter_annotations</code> array.
     * If the annotations attribute is not a parameter annotations attribute,
     * this method is never invoked.
     *
     * @param numParameters    <code>num_parameters</code>.
     */
    public void beginParameters(int numParameters) throws Exception {}

    /**
     * Invoked when the parser ends parsing a
     * <code>parameter_annotations</code> array.
     * If the annotations attribute is not a parameter annotations attribute,
     * this method is never invoked.
     */
    public void endParameters() throws Exception {}

    /**
     * Invoked when the parser starts parsing an
     * <code>annotations</code> array in
     * <code>..Annotations_attribute</code>.
     *
     * @param numAnnotations        <code>num_annotations</code>.
     */
    public void beginAnnotationsArray(int numAnnotations)
        throws Exception {}

    /**
     * Invoked when the parser ends parsing an
     * <code>annotations</code> array in
     * <code>..Annotations_attribute</code>.
     */
    public void endAnnotationsArray() throws Exception {}

    /**
     * Invoked when the parser starts parsing an element of
     * <code>annotations</code> array in
     * <code>Runtime(In)VisibleAnnotations_attribute</code>
     * or <code>parameter_annotations</code> array.
     *
     * @param cp                        the constant pool.
     * @param typeIndex                 <code>type_index</code>.
     * @param numMemberValuePairs       <code>num_member_value_pairs</code>.
     */
    public void beginAnnotation(ConstPool cp,
                int typeIndex, int numMemberValuePairs) throws Exception {}

    /**
     * Invoked when the parser ends parsing an element of
     * <code>annotations</code> array in
     * <code>Runtime(In)VisibleAnnotations_attribute</code>
     * or <code>parameter_annotations</code> array.
     */
    public void endAnnotation() throws Exception {}

    /**
     * Invoked when the parser starts parsing an element of
     * <code>member_value_pairs</code> array in <code>annotation</code>.
     *
     * @param cp                    the constant pool.
     * @param memberNameIndex       <code>member_name_index</code>.
     */
    public void beginMemberValuePair(ConstPool cp, int memberNameIndex)
        throws Exception {}

    /**
     * Invoked when the parser ends parsing an element of
     * <code>member_value_pairs</code> array in <code>annotation</code>.
     */
    public void endMemberValuePair() throws Exception {}

    /**
     * Invoked when the parser parses <code>const_value_index</code>
     * in <code>member_value</code>.
     *
     * @param cp        the constant pool.
     * @param tag       <code>tag</code>.
     * @param index     <code>const_value_index</code>.
     */
    public void constValueIndex(ConstPool cp, int tag, int index)
        throws Exception {}

    /**
     * Invoked when the parser parses <code>enum_const_value</code>
     * in <code>member_value</code>.
     *
     * @param cp                    the constant pool.
     * @param typeNameIndex         <code>type_name_index</code>.
     * @param constNameIndex        <code>const_name_index</code>.
     */
    public void enumConstValue(ConstPool cp, int typeNameIndex,
                               int constNameIndex) throws Exception {}

    /**
     * Invoked when the parser parses <code>class_info_index</code>
     * in <code>member_value</code>.
     *
     * @param cp        the constant pool.
     * @param index     <code>class_info_index</code>.
     */
    public void classInfoIndex(ConstPool cp, int index) throws Exception {}

    /**
     * Invoked when the parser starts parsing <code>annotation_value</code>
     * in <code>member_value</code>.
     */
    public void beginAnnotationValue() throws Exception {}

    /**
     * Invoked when the parser endss parsing <code>annotation_value</code>
     * in <code>member_value</code>.
     */
    public void endAnnotationValue() throws Exception {}

    /**
     * Invoked when the parser starts parsing <code>array_value</code>
     * in <code>member_value</code>.
     *
     * @param numValues         <code>num_values</code>.
     */
    public void beginArrayValue(int numValues) throws Exception {}

    /**
     * Invoked when the parser ends parsing an element of
     * <code>array_value</code>.
     *
     * @param i         the index of that element.
     */
    public void arrayElement(int i) throws Exception {}

    /**
     * Invoked when the parser ends parsing <code>array_value</code>
     * in <code>member_value</code>.
     */
    public void endArrayValue() throws Exception {}
}
