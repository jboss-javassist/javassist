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

package javassist.tools.reflect;

import javassist.CtClass;
import javassist.ClassPool;
import java.io.PrintStream;

class CompiledClass {
    public String classname;
    public String metaobject;
    public String classobject;
}

/**
 * A bytecode translator for reflection.
 *
 * <p>This translator directly modifies class files on a local disk so that
 * the classes represented by those class files are reflective.
 * After the modification, the class files can be run with the standard JVM
 * without <code>javassist.tools.reflect.Loader</code>
 * or any other user-defined class loader.
 *
 * <p>The modified class files are given as the command-line parameters,
 * which are a sequence of fully-qualified class names followed by options:
 *
 * <p><code>-m <i>classname</i></code> : specifies the class of the
 * metaobjects associated with instances of the class followed by
 * this option.  The default is <code>javassit.reflect.Metaobject</code>.
 *
 * <p><code>-c <i>classname</i></code> : specifies the class of the
 * class metaobjects associated with instances of the class followed by
 * this option.  The default is <code>javassit.reflect.ClassMetaobject</code>.
 *
 * <p>If a class name is not followed by any options, the class indicated
 * by that class name is not reflective.
 * 
 * <p>For example,
 * <ul><pre>% java Compiler Dog -m MetaDog -c CMetaDog Cat -m MetaCat Cow
 * </pre></ul>
 *
 * <p>modifies class files <code>Dog.class</code>, <code>Cat.class</code>,
 * and <code>Cow.class</code>.
 * The metaobject of a Dog object is a MetaDog object and the class
 * metaobject is a CMetaDog object.
 * The metaobject of a Cat object is a MetaCat object but
 * the class metaobject is a default one.
 * Cow objects are not reflective.
 *
 * <p>Note that if the super class is also made reflective, it must be done
 * before the sub class.
 *
 * @see javassist.tools.reflect.Metaobject
 * @see javassist.tools.reflect.ClassMetaobject
 * @see javassist.tools.reflect.Reflection
 */
public class Compiler {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            help(System.err);
            return;
        }

        CompiledClass[] entries = new CompiledClass[args.length];
        int n = parse(args, entries);

        if (n < 1) {
            System.err.println("bad parameter.");
            return;
        }

        processClasses(entries, n);
    }

    private static void processClasses(CompiledClass[] entries, int n)
        throws Exception
    {
        Reflection implementor = new Reflection();
        ClassPool pool = ClassPool.getDefault();
        implementor.start(pool);

        for (int i = 0; i < n; ++i) {
            CtClass c = pool.get(entries[i].classname);
            if (entries[i].metaobject != null
                                        || entries[i].classobject != null) {
                String metaobj, classobj;

                if (entries[i].metaobject == null)
                    metaobj = "javassist.tools.reflect.Metaobject";
                else
                    metaobj = entries[i].metaobject;

                if (entries[i].classobject == null)
                    classobj = "javassist.tools.reflect.ClassMetaobject";
                else
                    classobj = entries[i].classobject;

                if (!implementor.makeReflective(c, pool.get(metaobj),
                                              pool.get(classobj)))
                    System.err.println("Warning: " + c.getName()
                                + " is reflective.  It was not changed.");

                System.err.println(c.getName() + ": " + metaobj + ", "
                                   + classobj);
            }
            else
                System.err.println(c.getName() + ": not reflective");
        }

        for (int i = 0; i < n; ++i) {
            implementor.onLoad(pool, entries[i].classname);
            pool.get(entries[i].classname).writeFile();
        }
    }

    private static int parse(String[] args, CompiledClass[] result) {
        int n = -1;
        for (int i = 0; i < args.length; ++i) {
            String a = args[i];
            if (a.equals("-m"))
                if (n < 0 || i + 1 > args.length)
                    return -1;
                else
                    result[n].metaobject = args[++i];
            else if (a.equals("-c"))
                if (n < 0 || i + 1 > args.length)
                    return -1;
                else
                    result[n].classobject = args[++i];
            else if (a.charAt(0) == '-')
                return -1;
            else {
                CompiledClass cc = new CompiledClass();
                cc.classname = a;
                cc.metaobject = null;
                cc.classobject = null;
                result[++n] = cc;
            }
        }

        return n + 1;
    }

    private static void help(PrintStream out) {
        out.println("Usage: java javassist.tools.reflect.Compiler");
        out.println("            (<class> [-m <metaobject>] [-c <class metaobject>])+");
    }
}
