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
public class IntegerMemberValue extends MemberValue
{
   short const_value_index;

   public IntegerMemberValue(short cvi, ConstPool cp)
   {
      super('I', cp);
      this.const_value_index = cvi;
   }

   public IntegerMemberValue(ConstPool cp)
   {
      super('I', cp);
      setValue(0);
   }

   public int getValue()
   {
      return cp.getIntegerInfo(const_value_index);
   }

   public void setValue(int newVal)
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
      visitor.visitIntegerMemberValue(this);
   }
}
