/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package javassist.bytecode.annotation;

import javassist.bytecode.ConstPool;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 1.1 $
 *
 **/
public abstract class MemberValue
{
   ConstPool cp;
   char tag;


   public void write(DataOutputStream dos) throws IOException
   {
      byte btag = (byte)tag;
      dos.writeByte(btag);
   }
   public static MemberValue readMemberValue(ConstPool cp, DataInput di) throws java.io.IOException
   {
      byte btag = di.readByte();
      char tag = (char) btag;
      MemberValue rtn = null;
      switch (tag)
      {
         case 'B':
            rtn = new ByteMemberValue(di.readShort());
            break;
         case 'C':
            rtn = new CharMemberValue(di.readShort());
            break;
         case 'D':
            rtn = new DoubleMemberValue(di.readShort());
            break;
         case 'F':
            rtn = new FloatMemberValue(di.readShort());
            break;
         case 'I':
            rtn = new IntegerMemberValue(di.readShort());
            break;
         case 'J':
            rtn = new LongMemberValue(di.readShort());
            break;
         case 'S':
            rtn = new ShortMemberValue(di.readShort());
            break;
         case 'Z':
            rtn = new BooleanMemberValue(di.readShort());
            break;
         case 's':
            rtn = new StringMemberValue(di.readShort());
            break;
         case 'e':
            rtn = new EnumMemberValue(di.readShort(), di.readShort());
            break;
         case 'c':
            rtn = new ClassMemberValue(di.readShort());
            break;
         case '@':
            rtn = new AnnotationMemberValue(AnnotationInfo.readAnnotationInfo(cp, di));
            break;
         case '[':
            rtn = ArrayMemberValue.readArray(cp, di);
            break;
      }
      rtn.cp = cp;
      rtn.tag = tag;
      return rtn;
   }
}


