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
package javassist;

import java.io.*;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ClassFileWriter;

/**
 * Dump is a tool for viewing the class definition in the given
 * class file.  Unlike the JDK javap tool, Dump works even if
 * the class file is broken.
 *
 * <p>For example,
 * <ul><pre>% java javassist.Dump foo.class</pre></ul>
 *
 * <p>prints the contents of the constant pool and the list of methods
 * and fields.
 */
public class Dump {
    private Dump() {}

    /**
     * Main method.
     *
     * @param args[0]           class file name.
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java Dump <class file name>");
            return;
        }

        DataInputStream in = new DataInputStream(
                                         new FileInputStream(args[0]));
        ClassFile w = new ClassFile(in);
        PrintWriter out = new PrintWriter(System.out, true);
        out.println("*** constant pool ***");
        w.getConstPool().print(out);
        out.println();
        out.println("*** members ***");
        ClassFileWriter.print(w, out);
    }
}
