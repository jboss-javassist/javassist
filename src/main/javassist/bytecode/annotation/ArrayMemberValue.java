/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
 * @version $Revision: 1.1 $
 *
 **/
public class ArrayMemberValue extends MemberValue
{
   MemberValue[] values;
   public ArrayMemberValue()
   {
      tag = '[';
   }

   public MemberValue[] getValue()
   {
      return values;
   }

   public void setValue(MemberValue[] values)
   {
      this.values = values;
   }


   public static ArrayMemberValue readArray(ConstPool cp, DataInput di) throws java.io.IOException
   {
      ArrayMemberValue rtn = new ArrayMemberValue();
      int length = di.readShort();
      ArrayList values = new ArrayList(length);
      for (int i = 0; i < length; i++)
      {
         values.add(MemberValue.readMemberValue(cp, di));
      }
      rtn.values = (MemberValue[])values.toArray(new MemberValue[length]);
      return rtn;

   }

   public void write(DataOutputStream dos) throws IOException
   {
      super.write(dos);
      dos.writeShort(values.length);
      for (int i = 0; i < values.length; i++)
      {
         values[i].write(dos);
      }
   }
   public String toString()
   {
      StringBuffer buf = new StringBuffer("{");
      for (int i = 0; i < values.length; i++)
      {
         buf.append(values[i].toString());
         if (i + 1 < values.length) buf.append(", ");
      }
      buf.append("}");
      return buf.toString();
   }
}
