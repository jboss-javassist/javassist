/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package javassist.bytecode.annotation;

import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ConstPool;

import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Iterator;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 1.1 $
 *
 **/
public class AnnotationGroup
{
   AttributeInfo parent;
   LinkedHashMap annotations = new LinkedHashMap();
   public AnnotationGroup(AttributeInfo parent)
   {
      this.parent = parent;
      if (!parent.getName().equals("RuntimeInvisibleAnnotations") &&
          !parent.getName().equals("RuntimeVisibleAnnotations"))
         throw new RuntimeException("Annotation group must be RuntimeInvisibleAnnotations or RuntimeVisibleAnnotations, it was: " + parent.getName());

      try
      {
         initialize();
      }
      catch (java.io.IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   public String getName()
   {
      return parent.getName();
   }

   public Collection getAnnotations()
   {
      if (annotations == null) return null;
      return annotations.values();
   }

   public AnnotationInfo getAnnotation(String type)
   {
      if (annotations == null) return null;
      return (AnnotationInfo)annotations.get(type);
   }

   private void initialize() throws java.io.IOException
   {
      ConstPool cp = parent.getConstPool();
      byte[] bytes = parent.get();
      if (bytes == null) return;
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      DataInputStream di = new DataInputStream(bais);
      short num_annotations = di.readShort();
      for (int i = 0; i < num_annotations; i++)
      {
         AnnotationInfo info = AnnotationInfo.readAnnotationInfo(cp, di);
         annotations.put(info.getAnnotationType(), info);
      }
   }

   public void addAnnotation(AnnotationInfo info)
   {
      annotations.put(info.getAnnotationType(), info);
      try
      {
         parent.set(convertToBytes());
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   public void removeAnnotation(String name)
   {
      annotations.remove(name);
      try
      {
         parent.set(convertToBytes());
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   private byte[] convertToBytes() throws IOException
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream dos = new DataOutputStream(baos);
      short num_annotations = (short)annotations.size();
      dos.writeShort(num_annotations);
      Iterator it = annotations.values().iterator();
      while (it.hasNext())
      {
         AnnotationInfo info = (AnnotationInfo)it.next();
         info.write(dos);
      }
      dos.flush();
      return baos.toByteArray();
   }
}
