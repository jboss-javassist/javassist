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
public class DoubleMemberValue extends MemberValue
{
   short const_value_index;

   public DoubleMemberValue(short cvi)
   {
      tag = 'D';
      this.const_value_index = cvi;
   }

   public double getValue()
   {
      return cp.getDoubleInfo(const_value_index);
   }
   public void setValue(double newVal)
   {
      const_value_index = (short)cp.addDoubleInfo(newVal);
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
