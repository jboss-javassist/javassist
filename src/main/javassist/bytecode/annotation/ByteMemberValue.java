/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package javassist.bytecode.annotation;

import javassist.bytecode.ConstPool;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 1.2 $
 *
 **/
public class ByteMemberValue extends MemberValue
{
   short const_value_index;

   public ByteMemberValue(short const_value_index, ConstPool cp)
   {
      super('B', cp);
      this.const_value_index = const_value_index;
   }

   public ByteMemberValue(ConstPool cp)
   {
      super('B', cp);
      setValue((byte)0);
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
   public void accept(MemberValueVisitor visitor)
   {
      visitor.visitByteMemberValue(this);
   }
}
