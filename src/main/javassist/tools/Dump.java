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

package javassist.tools;

import java.io.*;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ClassFilePrinter;

/**
 * Dump is a tool for viewing the class definition in the given
 * class file.  Unlike the JDK javap tool, Dump works even if
 * the class file is broken.
 *
 * <p>For example,
 * <ul><pre>% java javassist.tools.Dump foo.class</pre></ul>
 *
 * <p>prints the contents of the constant pool and the list of methods
 * and fields.
 */
public class Dump {
    private Dump() {}

    /**
     * Main method.
     *
     * @param args           <code>args[0]</code> is the class file name.
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
        ClassFilePrinter.print(w, out);
    }
}
