/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba. All Rights Reserved.
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

package javassist.bytecode;

import java.io.PrintWriter;
import javassist.Modifier;
import java.util.List;

/**
 * A utility class for priting the contents of a class file.
 * It prints a constant pool table, fields, and methods in a
 * human readable representation.
 */
public class ClassFilePrinter {
    /**
     * Prints the contents of a class file to the standard output stream.
     */
    public static void print(ClassFile cf) {
        print(cf, new PrintWriter(System.out, true));
    }

    /**
     * Prints the contents of a class file.
     */
    public static void print(ClassFile cf, PrintWriter out) {
        List list;
        int n;

        /* 0x0020 (SYNCHRONIZED) means ACC_SUPER if the modifiers
         * are of a class.
         */
        int mod
            = AccessFlag.toModifier(cf.getAccessFlags()
                                    & ~AccessFlag.SYNCHRONIZED);
        out.println("major: " + cf.major + ", minor: " + cf.minor
                    + " modifiers: " + Integer.toHexString(cf.getAccessFlags()));
        out.println(Modifier.toString(mod) + " class "
                    + cf.getName() + " extends " + cf.getSuperclass());

        String[] infs = cf.getInterfaces();
        if (infs != null && infs.length > 0) {
            out.print("    implements ");
            out.print(infs[0]);
            for (int i = 1; i < infs.length; ++i)
                out.print(", " + infs[i]);

            out.println();
        }

        out.println();
        list = cf.getFields();
        n = list.size();
        for (int i = 0; i < n; ++i) {
            FieldInfo finfo = (FieldInfo)list.get(i);
            int acc = finfo.getAccessFlags();
            out.println(Modifier.toString(AccessFlag.toModifier(acc))
                        + " " + finfo.getName() + "\t"
                        + finfo.getDescriptor());
            printAttributes(finfo.getAttributes(), out, 'f');
        }

        out.println();
        list = cf.getMethods();
        n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            int acc = minfo.getAccessFlags();
            out.println(Modifier.toString(AccessFlag.toModifier(acc))
                        + " " + minfo.getName() + "\t"
                        + minfo.getDescriptor());
            printAttributes(minfo.getAttributes(), out, 'm');
            out.println();
        }

        out.println();
        printAttributes(cf.getAttributes(), out, 'c');
    }

    static void printAttributes(List list, PrintWriter out, char kind) {
        if (list == null)
            return;

        int n = list.size();
        for (int i = 0; i < n; ++i) {
            AttributeInfo ai = (AttributeInfo)list.get(i);
            if (ai instanceof CodeAttribute) {
                CodeAttribute ca = (CodeAttribute)ai;
                out.println("attribute: " + ai.getName() + ": "
                            + ai.getClass().getName());
                out.println("max stack " + ca.getMaxStack()
                            + ", max locals " + ca.getMaxLocals()
                            + ", " + ca.getExceptionTable().size()
                            + " catch blocks");
                out.println("<code attribute begin>");
                printAttributes(ca.getAttributes(), out, kind);
                out.println("<code attribute end>");
            }
            else if (ai instanceof AnnotationsAttribute) {
                out.println("annnotation: " + ai.toString());
            }
            else if (ai instanceof ParameterAnnotationsAttribute) {
                out.println("parameter annnotations: " + ai.toString());
            }
            else if (ai instanceof StackMapTable) {
                out.println("<stack map table begin>");
                StackMapTable.Printer.print((StackMapTable)ai, out);
                out.println("<stack map table end>");
            }
            else if (ai instanceof StackMap) {
                out.println("<stack map begin>");
                ((StackMap)ai).print(out);
                out.println("<stack map end>");
            }
            else if (ai instanceof SignatureAttribute) {
                SignatureAttribute sa = (SignatureAttribute)ai;
                String sig = sa.getSignature();
                out.println("signature: " + sig);
                try {
                    String s;
                    if (kind == 'c')
                        s = SignatureAttribute.toClassSignature(sig).toString();
                    else if (kind == 'm')
                        s = SignatureAttribute.toMethodSignature(sig).toString();
                    else
                        s = SignatureAttribute.toFieldSignature(sig).toString();

                    out.println("           " + s);
                }
                catch (BadBytecode e) {
                    out.println("           syntax error");
                }
            }
            else
                out.println("attribute: " + ai.getName()
                            + " (" + ai.get().length + " byte): "
                            + ai.getClass().getName());
        }
    }
}
