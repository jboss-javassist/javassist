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

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.analysis.FramePrinter;

/**
 * framedump is a tool for viewing a merged combination of the instructions and frame state
 *  of all methods in a class.
 *
 * <p>For example,
 * <ul><pre>% java javassist.tools.framedump foo.class</pre></ul>
 */
public class framedump {
    private framedump() {}

    /**
     * Main method.
     *
     * @param args <code>args[0]</code> is the class file name.
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java javassist.tools.framedump <class file name>");
            return;
        }
        
        ClassPool pool = ClassPool.getDefault();
        CtClass clazz = pool.get(args[0]);
        System.out.println("Frame Dump of " + clazz.getName() + ":");
        FramePrinter.print(clazz, System.out);
    }
}
