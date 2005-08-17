/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 2004 Bill Burke. All Rights Reserved.
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

/**
 * Visitor for traversing member values included in an annotation.
 *
 * @see MemberValue#accept(MemberValueVisitor)
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
public interface MemberValueVisitor {
   public void visitAnnotationMemberValue(AnnotationMemberValue node);
   public void visitArrayMemberValue(ArrayMemberValue node);
   public void visitBooleanMemberValue(BooleanMemberValue node);
   public void visitByteMemberValue(ByteMemberValue node);
   public void visitCharMemberValue(CharMemberValue node);
   public void visitDoubleMemberValue(DoubleMemberValue node);
   public void visitEnumMemberValue(EnumMemberValue node);
   public void visitFloatMemberValue(FloatMemberValue node);
   public void visitIntegerMemberValue(IntegerMemberValue node);
   public void visitLongMemberValue(LongMemberValue node);
   public void visitShortMemberValue(ShortMemberValue node);
   public void visitStringMemberValue(StringMemberValue node);
   public void visitClassMemberValue(ClassMemberValue node);
}
