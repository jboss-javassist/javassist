/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 2004 Bill Burke. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.bytecode.annotation;

import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ConstPool;

import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Iterator;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 1.2 $
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
