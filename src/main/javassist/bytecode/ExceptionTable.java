/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

class ExceptionTableEntry {
    int startPc;
    int endPc;
    int handlerPc;
    int catchType;

    ExceptionTableEntry(int start, int end, int handle, int type) {
        startPc = start;
        endPc = end;
        handlerPc = handle;
        catchType = type;
    }
}

/**
 * <code>exception_table[]</code> of <code>Code_attribute</code>.
 */
public class ExceptionTable {
    private ConstPool constPool;
    private ArrayList entries;

    /**
     * Constructs an <code>exception_table[]</code>.
     *
     * @param cp        constant pool table.
     */
    public ExceptionTable(ConstPool cp) {
        constPool = cp;
        entries = new ArrayList();
    }

    ExceptionTable(ConstPool cp, DataInputStream in) throws IOException {
        constPool = cp;
        int length = in.readUnsignedShort();
        ArrayList list = new ArrayList(length);
        for (int i = 0; i < length; ++i) {
            int start = in.readUnsignedShort();
            int end = in.readUnsignedShort();
            int handle = in.readUnsignedShort();
            int type = in.readUnsignedShort();
            list.add(new ExceptionTableEntry(start, end, handle, type));
        }

        entries = list;
    }

    /**
     * Returns <code>exception_table_length</code>, which is the number
     * of entries in the <code>exception_table[]</code>.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns <code>startPc</code> of the <i>n</i>-th entry.
     *
     * @param nth               the <i>n</i>-th (&gt;= 0).
     */
    public int startPc(int nth) {
        ExceptionTableEntry e = (ExceptionTableEntry)entries.get(nth);
        return e.startPc;
    }

    /**
     * Sets <code>startPc</code> of the <i>n</i>-th entry.
     *
     * @param nth               the <i>n</i>-th (&gt;= 0).
     * @param value             new value.
     */
    public void setStartPc(int nth, int value) {
        ExceptionTableEntry e = (ExceptionTableEntry)entries.get(nth);
        e.startPc = value;
    }

    /**
     * Returns <code>endPc</code> of the <i>n</i>-th entry.
     *
     * @param nth               the <i>n</i>-th (&gt;= 0).
     */
    public int endPc(int nth) {
        ExceptionTableEntry e = (ExceptionTableEntry)entries.get(nth);
        return e.endPc;
    }

    /**
     * Sets <code>endPc</code> of the <i>n</i>-th entry.
     *
     * @param nth               the <i>n</i>-th (&gt;= 0).
     * @param value             new value.
     */
    public void setEndPc(int nth, int value) {
        ExceptionTableEntry e = (ExceptionTableEntry)entries.get(nth);
        e.endPc = value;
    }

    /**
     * Returns <code>handlerPc</code> of the <i>n</i>-th entry.
     *
     * @param nth               the <i>n</i>-th (&gt;= 0).
     */
    public int handlerPc(int nth) {
        ExceptionTableEntry e = (ExceptionTableEntry)entries.get(nth);
        return e.handlerPc;
    }

    /**
     * Sets <code>handlerPc</code> of the <i>n</i>-th entry.
     *
     * @param nth               the <i>n</i>-th (&gt;= 0).
     * @param value             new value.
     */
    public void setHandlerPc(int nth, int value) {
        ExceptionTableEntry e = (ExceptionTableEntry)entries.get(nth);
        e.handlerPc = value;
    }

    /**
     * Returns <code>catchType</code> of the <i>n</i>-th entry.
     *
     * @param nth               the <i>n</i>-th (&gt;= 0).
     */
    public int catchType(int nth) {
        ExceptionTableEntry e = (ExceptionTableEntry)entries.get(nth);
        return e.catchType;
    }

    /**
     * Sets <code>catchType</code> of the <i>n</i>-th entry.
     *
     * @param nth               the <i>n</i>-th (&gt;= 0).
     * @param value             new value.
     */
    public void setCatchType(int nth, int value) {
        ExceptionTableEntry e = (ExceptionTableEntry)entries.get(nth);
        e.catchType = value;
    }

    /**
     * Copies the given exception table at the specified position
     * in the table.
     *
     * @param index     index (&gt;= 0) at which the entry is to be inserted.
     * @param offset    the offset added to the code position.
     */
    public void add(int index, ExceptionTable table, int offset) {
        int len = table.size();
        while (--len >= 0) {
            ExceptionTableEntry e
                = (ExceptionTableEntry)table.entries.get(len);
            add(index, e.startPc + offset, e.endPc + offset,
                e.handlerPc + offset, e.catchType);
        }
    }

    /**
     * Adds a new entry at the specified position in the table.
     *
     * @param index     index (&gt;= 0) at which the entry is to be inserted.
     * @param start     <code>startPc</code>
     * @param end       <code>endPc</code>
     * @param handler   <code>handlerPc</code>
     * @param type      <code>catchType</code>
     */
    public void add(int index, int start, int end, int handler, int type) {
        entries.add(index,
                    new ExceptionTableEntry(start, end, handler, type));
    }

    /**
     * Appends a new entry at the end of the table.
     *
     * @param start     <code>startPc</code>
     * @param end       <code>endPc</code>
     * @param handler   <code>handlerPc</code>
     * @param type      <code>catchType</code>
     */
    public void add(int start, int end, int handler, int type) {
        entries.add(new ExceptionTableEntry(start, end, handler, type));
    }

    /**
     * Removes the entry at the specified position in the table.
     *
     * @param index     the index of the removed entry.
     */
    public void remove(int index) {
        entries.remove(index);
    }

    /**
     * Makes a copy of this <code>exception_table[]</code>.
     * Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        pairs of replaced and substituted
     *                          class names.
     */
    public ExceptionTable copy(ConstPool newCp, Map classnames) {
        ExceptionTable et = new ExceptionTable(newCp);
        ConstPool srcCp = constPool;
        int len = size();
        for (int i = 0; i < len; ++i) {
            ExceptionTableEntry e = (ExceptionTableEntry)entries.get(i);
            int type = srcCp.copy(e.catchType, newCp, classnames);
            et.add(e.startPc, e.endPc, e.handlerPc, type);
        }

        return et;
    }

    void shiftPc(int where, int gapLength, boolean exclusive) {
        int len = size();
        for (int i = 0; i < len; ++i) {
            ExceptionTableEntry e = (ExceptionTableEntry)entries.get(i);
            e.startPc = shiftPc(e.startPc, where, gapLength, exclusive);
            e.endPc = shiftPc(e.endPc, where, gapLength, exclusive);
            e.handlerPc = shiftPc(e.handlerPc, where, gapLength, exclusive);
        }
    }

    private static int shiftPc(int pc, int where, int gapLength,
                               boolean exclusive) {
        if (pc > where || (exclusive && pc == where))
            pc += gapLength;

        return pc;
    }

    void write(DataOutputStream out) throws IOException {
        int len = size();
        out.writeShort(len);            // exception_table_length
        for (int i = 0; i < len; ++i) {
            ExceptionTableEntry e = (ExceptionTableEntry)entries.get(i);
            out.writeShort(e.startPc);
            out.writeShort(e.endPc);
            out.writeShort(e.handlerPc);
            out.writeShort(e.catchType);
        }
    }
}
