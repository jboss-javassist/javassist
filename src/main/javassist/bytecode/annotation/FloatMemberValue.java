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
public class FloatMemberValue extends MemberValue
{
   short const_value_index;

   public FloatMemberValue(short cvi)
   {
      tag = 'F';
      this.const_value_index = cvi;
   }

   public float getValue()
   {
      return cp.getFloatInfo(const_value_index);
   }
   public void setValue(float newVal)
   {
      const_value_index = (short)cp.addFloatInfo(newVal);
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
