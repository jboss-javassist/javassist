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

import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 1.5 $
 *
 **/
public class ClassMemberValue extends MemberValue
{
   short class_info_index;

   public ClassMemberValue(short cii, ConstPool cp)
   {
      super('c', cp);
      this.class_info_index = cii;
   }

   public ClassMemberValue(ConstPool cp)
   {
      super('c', cp);
      setClassName("java.lang.Class");
   }

   public String getClassName()
   {
      // beta1 return cp.getClassInfo(class_info_index);
      return Descriptor.toClassName(cp.getUtf8Info(class_info_index));
   }

   public void setClassName(String name)
   {
      // beta1 class_info_index = (short)cp.addClassInfo(name);
      class_info_index = (short)cp.addUtf8Info(Descriptor.of(name));
   }

   public String toString()
   {
       return getClassName();
   }
   public void write(DataOutputStream dos) throws IOException
   {
      super.write(dos);
      dos.writeShort(class_info_index);
   }
   public void accept(MemberValueVisitor visitor)
   {
      visitor.visitClassMemberValue(this);
   }
}
