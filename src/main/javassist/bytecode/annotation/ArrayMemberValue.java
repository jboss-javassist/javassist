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

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 1.3 $
 *
 **/
public class ArrayMemberValue extends MemberValue
{
   MemberValue[] values;
   MemberValue type;

   private ArrayMemberValue(ConstPool cp)
   {
      super('[', cp);

   }

   public ArrayMemberValue(MemberValue type, ConstPool cp)
   {
      this(cp);
      this.type = type;
   }

   public MemberValue[] getValue()
   {
      return values;
   }

   public void setValue(MemberValue[] values)
   {
      if (values != null && values.length > 0) type = values[0];
      this.values = values;
   }

   public MemberValue getType()
   {
      return type;
   }


   public static ArrayMemberValue readArray(ConstPool cp, DataInput di) throws java.io.IOException
   {
      ArrayMemberValue rtn = new ArrayMemberValue(cp);
      int length = di.readShort();
      ArrayList values = new ArrayList(length);
      for (int i = 0; i < length; i++)
      {
         MemberValue type = MemberValue.readMemberValue(cp, di);
         rtn.type = type;
         values.add(type);
      }
      rtn.values = (MemberValue[]) values.toArray(new MemberValue[length]);
      return rtn;

   }

   public void write(DataOutputStream dos) throws IOException
   {
      super.write(dos);
      if (values == null)
      {
         dos.writeShort(0);
         return;
      }
      dos.writeShort(values.length);
      for (int i = 0; i < values.length; i++)
      {
         values[i].write(dos);
      }
   }

   public String toString()
   {
      StringBuffer buf = new StringBuffer("{");
      if (values != null)
      {
         for (int i = 0; i < values.length; i++)
         {
            buf.append(values[i].toString());
            if (i + 1 < values.length) buf.append(", ");
         }
      }
      buf.append("}");
      return buf.toString();
   }

   public void accept(MemberValueVisitor visitor)
   {
      visitor.visitArrayMemberValue(this);
   }
}
