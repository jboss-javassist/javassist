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
public class ClassMemberValue extends MemberValue
{
   short class_info_index;

   public ClassMemberValue(short cii)
   {
      tag = 'c';
      this.class_info_index = cii;
   }

   public String getClassName()
   {
      return cp.getClassInfo(class_info_index);
   }

   public void setClassName(String name)
   {
      class_info_index = (short)cp.addClassInfo(name);
   }

   public String toString()
   {
       return getClassName();
   }
   public void write(DataOutputStream dos) throws IOException
   {
      super.write(dos);
      dos.writeShort(class_info_index);
   }
}
