/*
 * This file is part of the Javassist toolkit.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * either http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is Javassist.
 *
 * The Initial Developer of the Original Code is Shigeru Chiba.  Portions
 * created by Shigeru Chiba are Copyright (C) 1999-2003 Shigeru Chiba.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * The development of this software is supported in part by the PRESTO
 * program (Sakigake Kenkyu 21) of Japan Science and Technology Corporation.
 */

package javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import javassist.CtClass;

/**
 * <code>Code_attribute</code>.
 *
 * <p>To browse the <code>code</code> field of
 * a <code>Code_attribute</code> structure,
 * use <code>CodeIterator</code>.
 *
 * @see CodeIterator
 */
public class CodeAttribute extends AttributeInfo implements Opcode {
    /**
     * The name of this attribute <code>"Code"</code>.
     */
    public static final String tag = "Code";

    // code[] is stored in AttributeInfo.info.

    private int maxStack;
    private int maxLocals;
    private ExceptionTable exceptions;
    private LinkedList attributes;

    /**
     * Constructs a <code>Code_attribute</code>.
     *
     * @param cp	constant pool table
     * @param stack	<code>max_stack</code>
     * @param locals	<code>max_locals</code>
     * @param code	<code>code[]</code>
     * @param etable	<code>exception_table[]</code>
     */
    public CodeAttribute(ConstPool cp, int stack, int locals, byte[] code,
			 ExceptionTable etable)
    {
	super(cp, tag);
	maxStack = stack;
	maxLocals = locals;
	info = code;
	exceptions = etable;
	attributes = new LinkedList();
    }

    /**
     * Constructs a copy of <code>Code_attribute</code>.
     * Specified class names are replaced during the copy.
     *
     * @param cp		constant pool table.
     * @param src		source Code attribute.
     * @param classnames	pairs of replaced and substituted
     *				class names.
     */
    private CodeAttribute(ConstPool cp, CodeAttribute src, Map classnames)
	throws BadBytecode
    {
	super(cp, tag);

	maxStack = src.getMaxStack();
	maxLocals = src.getMaxLocals();
	exceptions = src.getExceptionTable().copy(cp, classnames);
	info = src.copyCode(cp, classnames, exceptions);
	attributes = new LinkedList();

	/* Since an index into the source constant pool table may not
	   be translated, we don't copy the attributes.
	*/
	/*
	List src_attr = src.getAttributes();
	int num = src_attr.size();
	for (int i = 0; i < num; ++i) {
	    AttributeInfo ai = (AttributeInfo)src_attr.get(i);
	    attributes.add(ai.copy(cp, classnames));
	}
	*/
    }

    CodeAttribute(ConstPool cp, int name_id, DataInputStream in)
	throws IOException
    {
	super(cp, name_id, (byte[])null);
	int attr_len = in.readInt();

	maxStack = in.readUnsignedShort();
	maxLocals = in.readUnsignedShort();

	int code_len = in.readInt();
	info = new byte[code_len];
	in.readFully(info);

	exceptions = new ExceptionTable(cp, in);

	attributes = new LinkedList();
	int num = in.readUnsignedShort();
	for (int i = 0; i < num; ++i)
	    attributes.add(AttributeInfo.read(cp, in));
    }

    /**
     * Makes a copy.  Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * @param newCp	the constant pool table used by the new copy.
     * @param classnames	pairs of replaced and substituted
     *				class names.
     * @exception RuntimeCopyException	if a <code>BadBytecode</code>
     *				exception is thrown, it is
     *				converted into
     *				<code>RuntimeCopyException</code>.
     *
     * @return <code>CodeAttribute</code> object.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames)
	throws RuntimeCopyException
    {
	try {
	    return new CodeAttribute(newCp, this, classnames);
	}
	catch (BadBytecode e) {
	    throw new RuntimeCopyException("bad bytecode. fatal?");
	}
    }

    /**
     * An exception that may be thrown by <code>copy()</code>
     * in <code>CodeAttribute</code>.
     */
    public static class RuntimeCopyException extends RuntimeException {
	/**
	 * Constructs an exception.
	 */
	public RuntimeCopyException(String s) {
	    super(s);
	}
    }

    /**
     * Returns the length of this <code>attribute_info</code>
     * structure.
     * The returned value is <code>attribute_length + 6</code>.
     */
    public int length() {
	return 18 + info.length + exceptions.size() * 8
	       + AttributeInfo.getLength(attributes);
    }

    void write(DataOutputStream out) throws IOException {
	out.writeShort(name);		// attribute_name_index
	out.writeInt(length() - 6);	// attribute_length
	out.writeShort(maxStack);	// max_stack
	out.writeShort(maxLocals);	// max_locals
	out.writeInt(info.length);	// code_length
	out.write(info);		// code
	exceptions.write(out);
	out.writeShort(attributes.size());	// attributes_count
	AttributeInfo.writeAll(attributes, out);	// attributes
    }

    /**
     * This method is not available.
     *
     * @throws java.lang.UnsupportedOperationException	always thrown.
     */
    public byte[] get() { 
	throw new UnsupportedOperationException("CodeAttribute.get()");
    }

    /**
     * This method is not available.
     *
     * @throws java.lang.UnsupportedOperationException	always thrown.
     */
    public void set(byte[] newinfo) {
	throw new UnsupportedOperationException("CodeAttribute.set()");
    }

    /**
     * Returns the name of the class declaring the method including
     * this code attribute.
     */
    public String getDeclaringClass() {
	ConstPool cp = getConstPool();
	return cp.getClassName();
    }

    /**
     * Returns <code>max_stack</code>.
     */
    public int getMaxStack() {
	return maxStack;
    }

    /**
     * Sets <code>max_stack</code>.
     */
    public void setMaxStack(int value) {
	maxStack = value;
    }

    /**
     * Returns <code>max_locals</code>.
     */
    public int getMaxLocals() {
	return maxLocals;
    }

    /**
     * Sets <code>max_locals</code>.
     */
    public void setMaxLocals(int value) {
	maxLocals = value;
    }

    /**
     * Returns <code>code_length</code>.
     */
    public int getCodeLength() {
	return info.length;
    }

    /**
     * Returns <code>code[]</code>.
     */
    public byte[] getCode() {
	return info;
    }

    /**
     * Sets <code>code[]</code>.
     */
    void setCode(byte[] newinfo) { super.set(newinfo); }

    /**
     * Makes a new iterator for reading this code attribute.
     */
    public CodeIterator iterator() {
	return new CodeIterator(this);
    }

    /**
     * Returns <code>exception_table[]</code>.
     */
    public ExceptionTable getExceptionTable() { return exceptions; }

    /**
     * Returns <code>attributes[]</code>.
     * It returns a list of <code>AttributeInfo</code>.
     *
     * @see AttributeInfo
     */
    public List getAttributes() { return attributes; }

    /**
     * Returns the attribute with the specified name.
     * If it is not found, this method returns null.
     *
     * @param name	attribute name
     * @return		an <code>AttributeInfo</code> object or null.
     */
    public AttributeInfo getAttribute(String name) {
	return AttributeInfo.lookup(attributes, name);
    }

    /**
     * Copies code.
     */
    private byte[] copyCode(ConstPool destCp, Map classnames,
			    ExceptionTable etable)
	throws BadBytecode
    {
	int len = getCodeLength();
	byte[] newCode = new byte[len];

	LdcEntry ldc = copyCode(this.info, 0, len, this.getConstPool(),
				newCode, destCp, classnames);
	return LdcEntry.doit(newCode, ldc, etable);
    }

    private static LdcEntry copyCode(byte[] code, int beginPos, int endPos,
				     ConstPool srcCp, byte[] newcode,
				     ConstPool destCp, Map classnameMap)
	throws BadBytecode
    {
	int i2, index;
	LdcEntry ldcEntry = null;

	for (int i = beginPos; i < endPos; i = i2) {
	    i2 = CodeIterator.nextOpcode(code, i);
	    byte c = code[i];
	    newcode[i] = c;
	    switch (c & 0xff) {
	    case LDC_W :
	    case LDC2_W :
	    case GETSTATIC :
	    case PUTSTATIC :
	    case GETFIELD :
	    case PUTFIELD :
	    case INVOKEVIRTUAL :
	    case INVOKESPECIAL :
	    case INVOKESTATIC :
	    case NEW :
	    case ANEWARRAY :
	    case CHECKCAST :
	    case INSTANCEOF :
		copyConstPoolInfo(i + 1, code, srcCp, newcode, destCp,
				  classnameMap);
		break;
	    case LDC :
		index = code[i + 1] & 0xff;
		index = srcCp.copy(index, destCp, classnameMap);
		if (index < 0x100)
		    newcode[i + 1] = (byte)index;
		else {
		    LdcEntry ldc = new LdcEntry();
		    ldc.where = i;
		    ldc.index = index;
		    ldc.next = ldcEntry;
		    ldcEntry = ldc;
		}
		break;
	    case INVOKEINTERFACE :
		copyConstPoolInfo(i + 1, code, srcCp, newcode, destCp,
				  classnameMap);
		newcode[i + 3] = code[i + 3];
		newcode[i + 4] = code[i + 4];
		break;
	    case MULTIANEWARRAY :
		copyConstPoolInfo(i + 1, code, srcCp, newcode, destCp,
				  classnameMap);
		newcode[i + 3] = code[i + 3];
		break;
	    default :
		while (++i < i2)
		    newcode[i] = code[i];

		break;
	    }
	}

	return ldcEntry;
    }

    private static void copyConstPoolInfo(int i, byte[] code, ConstPool srcCp,
					  byte[] newcode, ConstPool destCp,
					  Map classnameMap) {
	int index = ((code[i] & 0xff) << 8) | (code[i + 1] & 0xff);
	index = srcCp.copy(index, destCp, classnameMap);
	newcode[i] = (byte)(index >> 8);
	newcode[i + 1] = (byte)index;
    }
}

final class LdcEntry {
    LdcEntry next;
    int where;
    int index;

    static byte[] doit(byte[] code, LdcEntry ldc, ExceptionTable etable)
	throws BadBytecode
    {
	while (ldc != null) {
	    int where = ldc.where;
	    code = CodeIterator.insertGap(code, where, 1, false, etable);
	    code[where] = (byte)Opcode.LDC_W;
	    ByteArray.write16bit(ldc.index, code, where + 1);
	    ldc = ldc.next;
	}

	return code;
    }
}
