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
public class StringMemberValue extends MemberValue
{
   short const_value_index;

   public StringMemberValue(short cvi)
   {
      tag = 's';
      this.const_value_index = cvi;
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
}
