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
public class FloatMemberValue extends MemberValue
{
   short const_value_index;

   public FloatMemberValue(short cvi, ConstPool cp)
   {
      super('F', cp);
      this.const_value_index = cvi;
   }

   public FloatMemberValue(ConstPool cp)
   {
      super('F', cp);
      setValue(0);
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
   public void accept(MemberValueVisitor visitor)
   {
      visitor.visitFloatMemberValue(this);
   }
}
