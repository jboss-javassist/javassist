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
public class EnumMemberValue extends MemberValue
{
   short type_name_index;
   short const_name_index;

   public EnumMemberValue(short type, short cni)
   {
      tag = 'e';
      this.type_name_index = type;
      this.const_name_index = cni;
   }

   public String getEnumType()
   {
      return cp.getUtf8Info(type_name_index);
   }

   public String getEnumVal()
   {
      return cp.getUtf8Info(const_name_index);
   }

   public String toString()
   {
      return getEnumType() + "." + getEnumVal();
   }

   public void write(DataOutputStream dos) throws IOException
   {
      super.write(dos);
      dos.writeShort(type_name_index);
      dos.writeShort(const_name_index);
   }
}
