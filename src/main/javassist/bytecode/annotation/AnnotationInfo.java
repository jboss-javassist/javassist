/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package javassist.bytecode.annotation;

import javassist.bytecode.ConstPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 1.1 $
 *
 **/
public class AnnotationInfo
{
   short type_index;
   LinkedHashMap members;
   HashMap memberName2Index;
   ConstPool cp;

   private AnnotationInfo()
   {

   }

   /**
    * todo Enums are not supported right now.
    * This is for creation at runtime
    * @param clazz
    */
   public AnnotationInfo(ConstPool cp, CtClass clazz) throws javassist.NotFoundException
   {

      if (!clazz.isInterface()) throw new RuntimeException("Only interfaces are allowed for AnnotationInfo creation.");
      this.cp = cp;
      type_index = (short) cp.addClassInfo(clazz);
      CtMethod methods[] = clazz.getDeclaredMethods();
      if (methods.length > 0)
      {
         members = new LinkedHashMap();
         memberName2Index = new HashMap();
      }
      for (int i = 0; i < methods.length; i++)
      {
         CtClass returnType = methods[i].getReturnType();
         if (returnType.equals(CtPrimitiveType.booleanType))
         {
            addMemberValue(methods[i].getName(), new BooleanMemberValue((short) -1));
         }
         else if (returnType.equals(CtPrimitiveType.byteType))
         {
            addMemberValue(methods[i].getName(), new ByteMemberValue((short) -1));
         }
         else if (returnType.equals(CtPrimitiveType.charType))
         {
            addMemberValue(methods[i].getName(), new CharMemberValue((short) -1));
         }
         else if (returnType.equals(CtPrimitiveType.doubleType))
         {
            addMemberValue(methods[i].getName(), new DoubleMemberValue((short) -1));
         }
         else if (returnType.equals(CtPrimitiveType.floatType))
         {
            addMemberValue(methods[i].getName(), new FloatMemberValue((short) -1));
         }
         else if (returnType.equals(CtPrimitiveType.intType))
         {
            addMemberValue(methods[i].getName(), new IntegerMemberValue((short) -1));
         }
         else if (returnType.equals(CtPrimitiveType.longType))
         {
            addMemberValue(methods[i].getName(), new LongMemberValue((short) -1));
         }
         else if (returnType.equals(CtPrimitiveType.shortType))
         {
            addMemberValue(methods[i].getName(), new ShortMemberValue((short) -1));
         }
         else if (returnType.getName().equals("java.lang.Class"))
         {
            addMemberValue(methods[i].getName(), new ClassMemberValue((short) -1));
         }
         else if (returnType.getName().equals("java.lang.String") || returnType.getName().equals("String"))
         {
            addMemberValue(methods[i].getName(), new StringMemberValue((short) -1));
         }
         else if (returnType.isArray())
         {
            addMemberValue(methods[i].getName(), new ArrayMemberValue());
         }
         else if (returnType.isInterface())
         {
            addMemberValue(methods[i].getName(), new AnnotationMemberValue(null));
         }
         else
         {
            throw new RuntimeException("cannot handle member type: " + returnType.getName());
         }
      }
   }

   private void addMemberValue(String name, MemberValue value)
   {
      short index = (short) cp.addUtf8Info(name);
      members.put(name, value);
      memberName2Index.put(name, new Short(index));
      value.cp = this.cp;
   }

   public String getAnnotationType()
   {
      return cp.getClassInfo(type_index);
   }

   public Set getMemberNames()
   {
      if (members == null) return null;
      return members.keySet();
   }

   public MemberValue getMemberValue(String member)
   {
      if (members == null) return null;
      return (MemberValue) members.get(member);
   }

   public static AnnotationInfo readAnnotationInfo(ConstPool cp, DataInput di) throws java.io.IOException
   {
      AnnotationInfo info = new AnnotationInfo();
      info.cp = cp;
      short type_index = di.readShort();
      info.type_index = type_index;
      short num_member_value_pairs = di.readShort();
      if (num_member_value_pairs > 0)
      {
         info.members = new LinkedHashMap();
         info.memberName2Index = new HashMap();
      }
      for (int j = 0; j < num_member_value_pairs; j++)
      {
         short member_name_index = di.readShort();
         String memberName = cp.getUtf8Info(member_name_index);
         MemberValue value = MemberValue.readMemberValue(cp, di);
         info.members.put(memberName, value);
         info.memberName2Index.put(memberName, new Short(member_name_index));
      }
      return info;
   }

   public void write(DataOutputStream dos) throws IOException
   {
      dos.writeShort(type_index);
      dos.writeShort(members.size());
      Iterator it = members.keySet().iterator();
      while (it.hasNext())
      {
         String name = (String) it.next();
         Short index = (Short) memberName2Index.get(name);
         dos.writeShort(index.shortValue());
         MemberValue value = (MemberValue) members.get(name);
         value.write(dos);
      }
   }

   public String toString()
   {
      StringBuffer buf = new StringBuffer("@");
      buf.append(getAnnotationType());
      if (members != null)
      {
         buf.append("(");
         Iterator mit = members.keySet().iterator();
         while (mit.hasNext())
         {
            String name = (String) mit.next();
            buf.append(name).append("=").append(getMemberValue(name));
            if (mit.hasNext()) buf.append(", ");
         }
         buf.append(")");
      }
      return buf.toString();
   }
}

