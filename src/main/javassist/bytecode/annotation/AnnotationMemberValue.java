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
public class AnnotationMemberValue extends MemberValue
{
   AnnotationInfo annotation;
   public AnnotationMemberValue(AnnotationInfo a, ConstPool cp)
   {
      super('@', cp);
      this.annotation = a;
   }

   public AnnotationMemberValue(ConstPool cp)
   {
      super('@', cp);
   }

   public AnnotationInfo getNestedAnnotation()
   {
      return annotation;
   }

   public void setNestedAnnotation(AnnotationInfo info)
   {
      annotation = info;
   }

   public String toString()
   {
      return annotation.toString();
   }

   public void write(DataOutputStream dos) throws IOException
   {
      super.write(dos);
      annotation.write(dos);
   }

   public void accept(MemberValueVisitor visitor)
   {
      visitor.visitAnnotationMemberValue(this);
   }

}
