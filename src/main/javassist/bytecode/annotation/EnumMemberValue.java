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
public class EnumMemberValue extends MemberValue
{
   short type_name_index;
   short const_name_index;

   public EnumMemberValue(short type, short cni, ConstPool cp)
   {
      super('e', cp);
      this.type_name_index = type;
      this.const_name_index = cni;
   }

   public EnumMemberValue(String type, ConstPool cp)
   {
      super('e', cp);
      setEnumType(type);
   }

   public EnumMemberValue(ConstPool cp)
   {
      super('e', cp);
   }

   /**
    * @return tring representing the classname
    */
   public String getEnumType()
   {
      return Descriptor.toClassName(cp.getUtf8Info(type_name_index));
   }

   /**
    *
    * @param classname FQN classname
    */
   public void setEnumType(String classname)
   {
      type_name_index = (short)cp.addUtf8Info(Descriptor.of(classname));
   }

   public String getEnumVal()
   {
      return cp.getUtf8Info(const_name_index);
   }

   public void setEnumVal(String name)
   {
      const_name_index = (short)cp.addUtf8Info(name);
   }



   public String toString()
   {
      return getEnumType() + "." + getEnumVal();
   }

   public void write(DataOutputStream dos) throws IOException
   {
      super.write(dos);
      dos.writeShort(type_name_index);
      dos.writeShort(const_name_index);
   }
   public void accept(MemberValueVisitor visitor)
   {
      visitor.visitEnumMemberValue(this);
   }
}
