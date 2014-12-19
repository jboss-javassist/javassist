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
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.List;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Note: if you define a new subclass of AttributeInfo, then
//       update AttributeInfo.read(), .copy(), and (maybe) write().

/**
 * <code>attribute_info</code> structure.
 */
public class AttributeInfo {
    protected ConstPool constPool;
    int name;
    byte[] info;

    protected AttributeInfo(ConstPool cp, int attrname, byte[] attrinfo) {
        constPool = cp;
        name = attrname;
        info = attrinfo;
    }

    protected AttributeInfo(ConstPool cp, String attrname) {
        this(cp, attrname, (byte[])null);
    }

    /**
     * Constructs an <code>attribute_info</code> structure.
     *
     * @param cp                constant pool table
     * @param attrname          attribute name
     * @param attrinfo          <code>info</code> field
     *                          of <code>attribute_info</code> structure.
     */
    public AttributeInfo(ConstPool cp, String attrname, byte[] attrinfo) {
        this(cp, cp.addUtf8Info(attrname), attrinfo);
    }

    protected AttributeInfo(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        constPool = cp;
        name = n;
        int len = in.readInt();
        info = new byte[len];
        if (len > 0)
            in.readFully(info);
    }

    static AttributeInfo read(ConstPool cp, DataInputStream in)
        throws IOException
    {
        int name = in.readUnsignedShort();
        String nameStr = cp.getUtf8Info(name);
        if (nameStr.charAt(0) < 'L') {
            if (nameStr.equals(AnnotationDefaultAttribute.tag))
                return new AnnotationDefaultAttribute(cp, name, in);
            else if (nameStr.equals(BootstrapMethodsAttribute.tag))
                return new BootstrapMethodsAttribute(cp, name, in);
            else if (nameStr.equals(CodeAttribute.tag))
                return new CodeAttribute(cp, name, in);
            else if (nameStr.equals(ConstantAttribute.tag))
                return new ConstantAttribute(cp, name, in);
            else if (nameStr.equals(DeprecatedAttribute.tag))
                return new DeprecatedAttribute(cp, name, in);
            else if (nameStr.equals(EnclosingMethodAttribute.tag))
                return new EnclosingMethodAttribute(cp, name, in);
            else if (nameStr.equals(ExceptionsAttribute.tag))
                return new ExceptionsAttribute(cp, name, in);
            else if (nameStr.equals(InnerClassesAttribute.tag))
                return new InnerClassesAttribute(cp, name, in);
        }
        else {
            /* Note that the names of Annotations attributes begin with 'R'. 
             */
            if (nameStr.equals(LineNumberAttribute.tag))
                return new LineNumberAttribute(cp, name, in);
            else if (nameStr.equals(LocalVariableAttribute.tag))
                return new LocalVariableAttribute(cp, name, in);
            else if (nameStr.equals(LocalVariableTypeAttribute.tag))
                return new LocalVariableTypeAttribute(cp, name, in);
            else if (nameStr.equals(MethodParametersAttribute.tag))
                return new MethodParametersAttribute(cp, name, in);
            else if (nameStr.equals(AnnotationsAttribute.visibleTag)
                     || nameStr.equals(AnnotationsAttribute.invisibleTag)) {
                // RuntimeVisibleAnnotations or RuntimeInvisibleAnnotations
                return new AnnotationsAttribute(cp, name, in);
            }
            else if (nameStr.equals(ParameterAnnotationsAttribute.visibleTag)
                || nameStr.equals(ParameterAnnotationsAttribute.invisibleTag))
                return new ParameterAnnotationsAttribute(cp, name, in);
            else if (nameStr.equals(SignatureAttribute.tag))
                return new SignatureAttribute(cp, name, in);
            else if (nameStr.equals(SourceFileAttribute.tag))
                return new SourceFileAttribute(cp, name, in);
            else if (nameStr.equals(SyntheticAttribute.tag))
                return new SyntheticAttribute(cp, name, in);
            else if (nameStr.equals(StackMap.tag))
                return new StackMap(cp, name, in);
            else if (nameStr.equals(StackMapTable.tag))
                return new StackMapTable(cp, name, in);
        }

        return new AttributeInfo(cp, name, in);
    }

    /**
     * Returns an attribute name.
     */
    public String getName() {
        return constPool.getUtf8Info(name);
    }

    /**
     * Returns a constant pool table.
     */
    public ConstPool getConstPool() { return constPool; }

    /**
     * Returns the length of this <code>attribute_info</code>
     * structure.
     * The returned value is <code>attribute_length + 6</code>.
     */
    public int length() {
        return info.length + 6;
    }

    /**
     * Returns the <code>info</code> field
     * of this <code>attribute_info</code> structure.
     *
     * <p>This method is not available if the object is an instance
     * of <code>CodeAttribute</code>.
     */
    public byte[] get() { return info; }

    /**
     * Sets the <code>info</code> field
     * of this <code>attribute_info</code> structure.
     *
     * <p>This method is not available if the object is an instance
     * of <code>CodeAttribute</code>.
     */
    public void set(byte[] newinfo) { info = newinfo; }

    /**
     * Makes a copy.  Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        pairs of replaced and substituted
     *                          class names.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        int s = info.length;
        byte[] srcInfo = info;
        byte[] newInfo = new byte[s];
        for (int i = 0; i < s; ++i)
            newInfo[i] = srcInfo[i];

        return new AttributeInfo(newCp, getName(), newInfo);
    }

    void write(DataOutputStream out) throws IOException {
        out.writeShort(name);
        out.writeInt(info.length);
        if (info.length > 0)
            out.write(info);
    }

    static int getLength(ArrayList list) {
        int size = 0;
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            AttributeInfo attr = (AttributeInfo)list.get(i);
            size += attr.length();
        }

        return size;
    }

    static AttributeInfo lookup(ArrayList list, String name) {
        if (list == null)
            return null;

        ListIterator iterator = list.listIterator();
        while (iterator.hasNext()) {
            AttributeInfo ai = (AttributeInfo)iterator.next();
            if (ai.getName().equals(name))
                return ai;
        }

        return null;            // no such attribute
    }

    static synchronized void remove(ArrayList list, String name) {
        if (list == null)
            return;

        ListIterator iterator = list.listIterator();
        while (iterator.hasNext()) {
            AttributeInfo ai = (AttributeInfo)iterator.next();
            if (ai.getName().equals(name))
                iterator.remove();
        }
    }

    static void writeAll(ArrayList list, DataOutputStream out)
        throws IOException
    {
        if (list == null)
            return;

        int n = list.size();
        for (int i = 0; i < n; ++i) {
            AttributeInfo attr = (AttributeInfo)list.get(i);
            attr.write(out);
        }
    }

    static ArrayList<AttributeInfo> copyAll(ArrayList<AttributeInfo> list, ConstPool cp) {
        if (list == null)
            return null;

        ArrayList<AttributeInfo> newList = new ArrayList<AttributeInfo>();
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            AttributeInfo attr = list.get(i);
            newList.add(attr.copy(cp, null));
        }

        return newList;
    }

    /**
     * Returns true if the given object represents the same attribute
     * as this object.  The equality test checks the member values.
     */
    public boolean equals(Object obj) {
        if (obj instanceof AttributeInfo) {
            AttributeInfo ai = (AttributeInfo)obj;
            return ai.getName().equals(getName()) && Arrays.equals(ai.info, this.info);
        }
        else
            return false;
    }
    
    /**
     * Special equals in case if some of nested attributes should be ignored during 
     * the comparison
     * @param obj - object to compare
     * @param ignoreList - list of nested attribute tags to be ignored 
     * @return true if the given object represents the same attribute
     * as this object with taking into account ignoreList. The equality test checks the member values.
     */
    public boolean equals(AttributeInfo attr, List<String> ignoreList)
    {
    	return equals(attr, ignoreList, new ArrayList<Pattern>());
    }
    
    /**
     * Special equals in case if some of nested attributes should be ignored during 
     * the comparison
     * @param obj - object to compare
     * @param ignoreList - list of nested attribute tags to be ignored 
     * @param ignoreRegexps - list of regular expressions those to be tested on the attribute's
     * value included into ignoreList in order to be actually ignored during comparison 
     * @return true if the given object represents the same attribute
     * as this object with taking into account ignoreList. The equality test checks the member values.
     */
    public boolean equals(AttributeInfo attr, List<String> ignoreList, List<Pattern> ignoreRegexps)
    {
    	return equals(attr);
    }
    
    /**
     * Compares attribute lists 
     * @param attributes1 - first list
     * @param attributes2 - second list
     * @return human readable difference between the lists
     */
    public static String CompareAttributes(List<AttributeInfo> attributes1, List<AttributeInfo> attributes2)
    {
      return CompareAttributes(attributes1, attributes2, new ArrayList<String>() );
    }
    
    /**
     * Compares attribute lists ignoring attributes from the ignoreList 
     * @param attributes1 - first list
     * @param attributes2 - second list
     * @param ignoreList- list attribute names to ignore;
     *                     if attribute name matches, that attribute won't be compared
     * @return human readable difference between the lists
     */
    public static String CompareAttributes(List<AttributeInfo> attributes1, List<AttributeInfo> attributes2, List<String> ignoreList)
    {
      return CompareAttributes(attributes1, attributes2, ignoreList, new ArrayList<Pattern>() );
    }
    
    /**
     * Compares attribute lists ignoring attributes from the ignoreList 
     * @param attributes1 - first list
     * @param attributes2 - second list
     * @param ignoreList - list attribute names to ignore; if attribute name matches 
     *                     that attribute won't be compared
     * @param ignoreRegexps - list of regular expressions matched against attribute content (using toString())
     *                     if attribute content matches for some attribute in ignoreList, 
     *                     that attribute won't be compared
     * @return human readable difference between the lists
     */
    public static String CompareAttributes(List<AttributeInfo> attributes1, List<AttributeInfo> attributes2, List<String> ignoreList, List<Pattern> ignorePatterns) 
    {
  	  String res = "";
      if (null != attributes1)
      {
  		if (null != attributes2)
  		{
  			int a1size = attributes1.size();
  			int a2size = attributes2.size();
  	        for (int i = 0; i < a1size; ++i) 
  	        {
  	            AttributeInfo ai1 = (AttributeInfo)attributes1.get(i);
  	            if (ignoreList.contains(ai1.getName()) && patternMatches(ignorePatterns, ai1.toString())) 
  	            {
  	            	continue;
  	            }
  	            int a1count = 0;
  	            for (int j = 0; j < a2size; ++j) 
  	            {
  	            	AttributeInfo ai2 = (AttributeInfo)attributes2.get(j);
  	  	            if (ignoreList.contains(ai2.getName()) && patternMatches(ignorePatterns, ai2.toString())) 
  	  	            {
  	  	            	continue;
  	  	            }
  	            	if ( ai1.equals(ai2, ignoreList, ignorePatterns)) 
  	            	{
  	            		a1count++;
  	            	}
  	            }
  	            if (a1count < 1)
  	            {
              		res += "Attributes are different: " + ai1.Dump() 
              		  + " 1 -> " + a1count + "\n";
  	            }
  	        }
  	        for (int i = 0; i < a2size; ++i) 
  	        {
  	            AttributeInfo ai2 = (AttributeInfo)attributes2.get(i);
  	            if (ignoreList.contains(ai2.getName()) && patternMatches(ignorePatterns, ai2.toString())) 
  	            {
  	            	continue;
  	            }
  	            int a2count = 0;
  	            for (int j = 0; j < a1size; ++j) 
  	            {
  	            	AttributeInfo ai1 = (AttributeInfo)attributes1.get(j);
  	  	            if (ignoreList.contains(ai1.getName()) && patternMatches(ignorePatterns, ai1.toString())) 
  	  	            {
  	  	            	continue;
  	  	            }
  	            	if ( ai2.equals(ai1, ignoreList, ignorePatterns)) 
  	            	{
  	            		a2count++;
  	            	}
  	            }
  	            if (a2count < 1)
  	            {
              		res += "Attributes are different: " + ai2.Dump() 
              		  + " " + a2count + " -> 1\n";
  	            }
  	        }
  		}
  		else
  		{
  		  res += "Attributes were added: " + attributes1.toString() + "\n"; 
  		}
  	  } 
  	  else 
  	  {
        res += "Attributes were removed: " + attributes2.toString() + "\n"; 
  	  }
  	  return res;
    }

    /**
     * checks if the value matches at least one of the ignorePatterns
     * @param ignorePatterns - patterns to match
     * @param value - string to check
     * @return true if ignorePatterns lsit is empty or value matches at least one of ignorePatterns
     */
    private static boolean patternMatches(List<Pattern> ignorePatterns,
			String value) 
    {
    	if (ignorePatterns.isEmpty())
    	{
    		return true;
    	}
    	for (Pattern p:ignorePatterns)
    	{
    		Matcher m = p.matcher(value);
    		if (m.matches()) {
    			return true;
    		}
    	}
    	return false;
	}

	/**
	 * Dumps the content of the attribute
	 * @return human readable content
	 */
    public String Dump()
    {
      String res = "";
      res += getName() + " " + InstructionEqualizer.getHex(info);
      return res;
    }
    
    /* The following two methods are used to implement
     * ClassFile.renameClass().
     * Only CodeAttribute, LocalVariableAttribute,
     * AnnotationsAttribute, and SignatureAttribute
     * override these methods.
     */
    void renameClass(String oldname, String newname) {}
    void renameClass(Map classnames) {}

    static void renameClass(List attributes, String oldname, String newname) {
        Iterator iterator = attributes.iterator();
        while (iterator.hasNext()) {
            AttributeInfo ai = (AttributeInfo)iterator.next();
            ai.renameClass(oldname, newname);
        }
    }

    static void renameClass(List attributes, Map classnames) {
        Iterator iterator = attributes.iterator();
        while (iterator.hasNext()) {
            AttributeInfo ai = (AttributeInfo)iterator.next();
            ai.renameClass(classnames);
        }
    }

    void getRefClasses(Map classnames) {}

    static void getRefClasses(List attributes, Map classnames) {
        Iterator iterator = attributes.iterator();
        while (iterator.hasNext()) {
            AttributeInfo ai = (AttributeInfo)iterator.next();
            ai.getRefClasses(classnames);
        }
    }
    
	/**
	 * Utility method to compare two string arrays and return human readable difference
	 * @param ilist1 - first string array
	 * @param ilist2 - second string array
	 * @param name - name to represent the compared entities
	 * @return human readable difference between two string arrays
	 */
    public static String CompareStringArrays(String[] ilist1, String[] ilist2, String name)
	{
		  String res = "";
		  for (int i = 0; i<ilist1.length; ++i)
		  {
			boolean matched = false;
		    for (int j = 0; j<ilist2.length; ++j)
		    {
		      if (ilist1[i].equals(ilist2[j])) {
		    	matched = true;
		    	break;
              }
		    }
		    if (! matched) {
		      res += name + ": " + ilist1[i] + " has been removed\n";
		    }
		  }
		  for (int i = 0; i<ilist2.length; ++i)
		  {
			boolean matched = false;
		    for (int j = 0; j<ilist1.length; ++j)
		    {
		      if (ilist2[i].equals(ilist1[j])) {
		    	matched = true;
		    	break;
		      }
		    }
		    if (! matched) {
		      res += name + ": " + ilist2[i] + " has been added\n";
		    }
		  }
		  return res;
	}

}
