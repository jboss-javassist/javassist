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
public class LongMemberValue extends MemberValue
{
   short const_value_index;

   public LongMemberValue(short cvi, ConstPool cp)
   {
      super('J', cp);
      this.const_value_index = cvi;
   }

   public LongMemberValue(ConstPool cp)
   {
      super('J', cp);
      setValue(0);
   }

   public long getValue()
   {
      return cp.getLongInfo(const_value_index);
   }
   public void setValue(long newVal)
   {
      const_value_index = (short)cp.addLongInfo(newVal);
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
      visitor.visitLongMemberValue(this);
   }
}
