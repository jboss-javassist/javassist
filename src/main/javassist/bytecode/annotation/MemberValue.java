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

import javassist.bytecode.ConstPool;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 1.3 $
 *
 **/
public abstract class MemberValue
{
   ConstPool cp;
   char tag;

   protected MemberValue(char tag, ConstPool cp)
   {
      this.cp = cp;
      this.tag = tag;
   }

   public abstract void accept(MemberValueVisitor visitor);

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
            rtn = new ByteMemberValue(di.readShort(), cp);
            break;
         case 'C':
            rtn = new CharMemberValue(di.readShort(), cp);
            break;
         case 'D':
            rtn = new DoubleMemberValue(di.readShort(), cp);
            break;
         case 'F':
            rtn = new FloatMemberValue(di.readShort(), cp);
            break;
         case 'I':
            rtn = new IntegerMemberValue(di.readShort(), cp);
            break;
         case 'J':
            rtn = new LongMemberValue(di.readShort(), cp);
            break;
         case 'S':
            rtn = new ShortMemberValue(di.readShort(), cp);
            break;
         case 'Z':
            rtn = new BooleanMemberValue(di.readShort(), cp);
            break;
         case 's':
            rtn = new StringMemberValue(di.readShort(), cp);
            break;
         case 'e':
            rtn = new EnumMemberValue(di.readShort(), di.readShort(), cp);
            break;
         case 'c':
            rtn = new ClassMemberValue(di.readShort(), cp);
            break;
         case '@':
            rtn = new AnnotationMemberValue(AnnotationInfo.readAnnotationInfo(cp, di), cp);
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


