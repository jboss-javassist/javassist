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

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 1.3 $
 *
 **/
public class StringMemberValue extends MemberValue
{
   short const_value_index;

   public StringMemberValue(short cvi, ConstPool cp)
   {
      super('s', cp);
      this.const_value_index = cvi;
   }

   public StringMemberValue(ConstPool cp)
   {
      super('s', cp);
      setValue("");
   }

   public String getValue()
   {
      return cp.getUtf8Info(const_value_index);
   }
   public void setValue(String newVal)
   {
      const_value_index = (short)cp.addUtf8Info(newVal);
   }

   public String toString()
   {
       return "\"" + getValue() + "\"";
   }
   public void write(DataOutputStream dos) throws IOException
   {
      super.write(dos);
      dos.writeShort(const_value_index);
   }
   public void accept(MemberValueVisitor visitor)
   {
      visitor.visitStringMemberValue(this);
   }
}
