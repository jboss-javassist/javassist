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
     * @param args[0]		class file name.
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
