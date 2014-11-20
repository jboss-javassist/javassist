/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.bytecode;

import java.io.DataInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.io.IOException;

import javassist.Modifier;

/**
 * <code>InnerClasses_attribute</code>.
 */
public class InnerClassesAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"InnerClasses"</code>.
     */
    public static final String tag = "InnerClasses";

    private String signature;
    
    InnerClassesAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    private InnerClassesAttribute(ConstPool cp, byte[] info) {
        super(cp, tag, info);
    }

    /**
     * Constructs an empty InnerClasses attribute.
     *
     * @see #append(String, String, String, int)
     */
    public InnerClassesAttribute(ConstPool cp) {
        super(cp, tag, new byte[2]);
        ByteArray.write16bit(0, get(), 0);
    }

    /**
     * Returns <code>number_of_classes</code>.
     */
    public int tableLength() { return ByteArray.readU16bit(get(), 0); }

    /**
     * Returns <code>classes[nth].inner_class_info_index</code>.
     */
    public int innerClassIndex(int nth) {
        return ByteArray.readU16bit(get(), nth * 8 + 2);
    }

    /**
     * Returns the class name indicated
     * by <code>classes[nth].inner_class_info_index</code>.
     *
     * @return null or the class name.
     */
    public String innerClass(int nth) {
        int i = innerClassIndex(nth);
        if (i == 0)
            return null;
        else
            return constPool.getClassInfo(i);
    }

    /**
     * Sets <code>classes[nth].inner_class_info_index</code> to
     * the given index.
     */
    public void setInnerClassIndex(int nth, int index) {
        ByteArray.write16bit(index, get(), nth * 8 + 2);
    }

    /**
     * Returns <code>classes[nth].outer_class_info_index</code>.
     */
    public int outerClassIndex(int nth) {
        return ByteArray.readU16bit(get(), nth * 8 + 4);
    }

    /**
     * Returns the class name indicated
     * by <code>classes[nth].outer_class_info_index</code>.
     *
     * @return null or the class name.
     */
    public String outerClass(int nth) {
        int i = outerClassIndex(nth);
        if (i == 0)
            return null;
        else
            return constPool.getClassInfo(i);
    }

    /**
     * Sets <code>classes[nth].outer_class_info_index</code> to
     * the given index.
     */
    public void setOuterClassIndex(int nth, int index) {
        ByteArray.write16bit(index, get(), nth * 8 + 4);
    }

    /**
     * Returns <code>classes[nth].inner_name_index</code>.
     */
    public int innerNameIndex(int nth) {
        return ByteArray.readU16bit(get(), nth * 8 + 6);
    }

    /**
     * Returns the simple class name indicated
     * by <code>classes[nth].inner_name_index</code>.
     *
     * @return null or the class name.
     */
    public String innerName(int nth) {
        int i = innerNameIndex(nth);
        if (i == 0)
            return null;
        else
            return constPool.getUtf8Info(i);
    }

    /**
     * Sets <code>classes[nth].inner_name_index</code> to
     * the given index.
     */
    public void setInnerNameIndex(int nth, int index) {
        ByteArray.write16bit(index, get(), nth * 8 + 6);
    }

    /**
     * Returns <code>classes[nth].inner_class_access_flags</code>.
     */
    public int accessFlags(int nth) {
        return ByteArray.readU16bit(get(), nth * 8 + 8);
    }

    /**
     * Sets <code>classes[nth].inner_class_access_flags</code> to
     * the given index.
     */
    public void setAccessFlags(int nth, int flags) {
        ByteArray.write16bit(flags, get(), nth * 8 + 8);
    }

    public String GetSignature()
    {
    	if (null == signature) {
    		signature = updateSignature();
    	}
    	return signature;
    }

    String updateSignature() {
    	String res = "";

        int len = tableLength();
        for (int i = 0; i < len; i++ )
        {
      	  res += innerClass(i) + outerClass(i) + innerName(i) + accessFlags(i);
        }
        
        try {
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(res.getBytes());
			res = InstructionEqualizer.getHex(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return res;
    }

    /**
     * Returns true if the given object represents the same InnerClassesAttribute  
     * as this object.  The equality test checks the inner classes' lists.
     */
	public boolean equals(Object obj) {
        if (obj instanceof InnerClassesAttribute) {
        	InnerClassesAttribute ic = (InnerClassesAttribute)obj;
            return CompareInnerClasses(this, ic).isEmpty();
        }
        else
            return false;
    }
	
	String GetEntry(int idx)
	{
      return innerClass(idx) + outerClass(idx) + innerName(idx) + accessFlags(idx);
	}
    
    String CompareInnerClasses(InnerClassesAttribute ic1, InnerClassesAttribute ic2) 
    {
  	  String res = "";
      if (null != ic1)
      {
        if (null != ic2)
        {
  		  int ic1size = ic1.tableLength();
  		  int ic2size = ic2.tableLength();
  	      for (int i = 0; i < ic1size; ++i) 
          {
            int ic1count = 0;
            String ic1Value = ic1.GetEntry(i);
  	        for (int j = 0; j < ic2size; ++j) 
  	        {
           	  if ( ic1Value.equals(ic2.GetEntry(j)))
           	  {
           		ic1count++;
              }
            }
  	        if (ic1count < 1)
            {
       		  res += "Innerclasses: " + ic1Value + " removed\n";
            }
          }
  	      for (int i = 0; i < ic2size; ++i) 
  	      {
  	        int ic2count = 0;
            String ic2Value = ic2.GetEntry(i);
  	        for (int j = 0; j < ic1size; ++j) 
  	        {
  	       	  if ( ic2Value.equals(ic1.GetEntry(j)) ) 
  	       	  {
  	      	    ic2count++;
           	  }
            }
            if (ic2count < 1)
  	        {
          	  res += "Innerclasses: " + ic2Value + " added\n"; 
  	        }
  	      }
  	    }
        else
  	    {
  		  res += "InnerClasses were removed: " + ic1.Dump() + "\n"; 
  	    }
  	  } 
  	  else 
  	  {
        res += "InnerClasses were added: " + ic2.Dump() + "\n"; 
  	  }
  	  return res;
    }

	/**
	 * Dumps the content of the attribute
	 * @return human readable content
	 */
    public String Dump()
    {
      String res = "innerclasses: {";
      int len = tableLength();
      for (int i = 0; i < len; i++ )
      {
    	  res += "inner_cii: " + innerClass(i) + " outer_cii: "
    	    +outerClass(i) + " inner_name: " + innerName(i) 
    	    + " aflags: " + Modifier.toString(AccessFlag.toModifier(accessFlags(i))) + "|";
      }
      res += "}";
      return res;
    }
    
    /**
     * Appends a new entry.
     *
     * @param inner     <code>inner_class_info_index</code>
     * @param outer     <code>outer_class_info_index</code>
     * @param name      <code>inner_name_index</code>
     * @param flags     <code>inner_class_access_flags</code>
     */
    public void append(String inner, String outer, String name, int flags) {
        int i = constPool.addClassInfo(inner);
        int o = constPool.addClassInfo(outer);
        int n = constPool.addUtf8Info(name);
        append(i, o, n, flags);
    }

    /**
     * Appends a new entry.
     *
     * @param inner     <code>inner_class_info_index</code>
     * @param outer     <code>outer_class_info_index</code>
     * @param name      <code>inner_name_index</code>
     * @param flags     <code>inner_class_access_flags</code>
     */
    public void append(int inner, int outer, int name, int flags) {
        byte[] data = get();
        int len = data.length;
        byte[] newData = new byte[len + 8];
        for (int i = 2; i < len; ++i)
            newData[i] = data[i];

        int n = ByteArray.readU16bit(data, 0);
        ByteArray.write16bit(n + 1, newData, 0);

        ByteArray.write16bit(inner, newData, len);
        ByteArray.write16bit(outer, newData, len + 2);
        ByteArray.write16bit(name, newData, len + 4);
        ByteArray.write16bit(flags, newData, len + 6);

        set(newData);
    }

    /**
     * Makes a copy.  Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        pairs of replaced and substituted
     *                          class names.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        byte[] src = get();
        byte[] dest = new byte[src.length];
        ConstPool cp = getConstPool();
        InnerClassesAttribute attr = new InnerClassesAttribute(newCp, dest);
        int n = ByteArray.readU16bit(src, 0);
        ByteArray.write16bit(n, dest, 0);
        int j = 2;
        for (int i = 0; i < n; ++i) {
            int innerClass = ByteArray.readU16bit(src, j);
            int outerClass = ByteArray.readU16bit(src, j + 2);
            int innerName = ByteArray.readU16bit(src, j + 4);
            int innerAccess = ByteArray.readU16bit(src, j + 6);

            if (innerClass != 0)
                innerClass = cp.copy(innerClass, newCp, classnames);

            ByteArray.write16bit(innerClass, dest, j);

            if (outerClass != 0)
                outerClass = cp.copy(outerClass, newCp, classnames);

            ByteArray.write16bit(outerClass, dest, j + 2);

            if (innerName != 0)
                innerName = cp.copy(innerName, newCp, classnames);

            ByteArray.write16bit(innerName, dest, j + 4);
            ByteArray.write16bit(innerAccess, dest, j + 6);
            j += 8;
        }

        return attr;
    }
}
