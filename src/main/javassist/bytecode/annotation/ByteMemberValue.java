/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package javassist.bytecode.annotation;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 1.1 $
 *
 **/
public class ByteMemberValue extends MemberValue
{
   short const_value_index;

   public ByteMemberValue(short const_value_index)
   {
      tag = 'B';
      this.const_value_index = const_value_index;
   }

   public byte getValue()
   {
      return (byte)cp.getIntegerInfo(const_value_index);
   }

   public void setValue(byte newVal)
   {
      const_value_index = (short)cp.addIntegerInfo(newVal);
   }

   public String toString()
   {
       return "" + getValue();
   }
   public void write(DataOutputStream dos) throws IOException
   {
      super.write(dos);
      dos.writeShort(const_value_index);
   }
}
