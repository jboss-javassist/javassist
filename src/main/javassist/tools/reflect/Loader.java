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

import javassist.CannotCompileException;
import javassist.NotFoundException;
import javassist.ClassPool;

/**
 * A class loader for reflection.
 *
 * <p>To run a program, say <code>MyApp</code>,
 * including a reflective class,
 * you must write a start-up program as follows:
 *
 * <ul><pre>
 * public class Main {
 *   public static void main(String[] args) throws Throwable {
 *     javassist.tools.reflect.Loader cl
 *         = (javassist.tools.reflect.Loader)Main.class.getClassLoader();
 *     cl.makeReflective("Person", "MyMetaobject",
 *                       "javassist.tools.reflect.ClassMetaobject");
 *     cl.run("MyApp", args);
 *   }
 * }
 * </pre></ul>
 *
 * <p>Then run this program as follows:
 *
 * <ul><pre>% java javassist.tools.reflect.Loader Main arg1, ...</pre></ul>
 *
 * <p>This command runs <code>Main.main()</code> with <code>arg1</code>, ...
 * and <code>Main.main()</code> runs <code>MyApp.main()</code> with
 * <code>arg1</code>, ...
 * The <code>Person</code> class is modified
 * to be a reflective class.  Method calls on a <code>Person</code>
 * object are intercepted by an instance of <code>MyMetaobject</code>.
 *
 * <p>Also, you can run <code>MyApp</code> in a slightly different way:
 *
 * <ul><pre>
 * public class Main2 {
 *   public static void main(String[] args) throws Throwable {
 *     javassist.tools.reflect.Loader cl = new javassist.tools.reflect.Loader();
 *     cl.makeReflective("Person", "MyMetaobject",
 *                       "javassist.tools.reflect.ClassMetaobject");
 *     cl.run("MyApp", args);
 *   }
 * }
 * </pre></ul>
 *
 * <p>This program is run as follows:
 *
 * <ul><pre>% java Main2 arg1, ...</pre></ul>
 *
 * <p>The difference from the former one is that the class <code>Main</code>
 * is loaded by <code>javassist.tools.reflect.Loader</code> whereas the class
 * <code>Main2</code> is not.  Thus, <code>Main</code> belongs
 * to the same name space (security domain) as <code>MyApp</code>
 * whereas <code>Main2</code> does not; <code>Main2</code> belongs
 * to the same name space as <code>javassist.tools.reflect.Loader</code>.
 * For more details,
 * see the notes in the manual page of <code>javassist.Loader</code>.
 *
 * <p>The class <code>Main2</code> is equivalent to this class:
 *
 * <ul><pre>
 * public class Main3 {
 *   public static void main(String[] args) throws Throwable {
 *     Reflection reflection = new Reflection();
 *     javassist.Loader cl
 *         = new javassist.Loader(ClassPool.getDefault(reflection));
 *     reflection.makeReflective("Person", "MyMetaobject",
 *                               "javassist.tools.reflect.ClassMetaobject");
 *     cl.run("MyApp", args);
 *   }
 * }
 * </pre></ul>
 *
 * <p><b>Note:</b>
 *
 * <p><code>javassist.tools.reflect.Loader</code> does not make a class reflective
 * if that class is in a <code>java.*</code> or
 * <code>javax.*</code> pacakge because of the specifications
 * on the class loading algorithm of Java.  The JVM does not allow to
 * load such a system class with a user class loader.
 *
 * <p>To avoid this limitation, those classes should be statically
 * modified with <code>javassist.tools.reflect.Compiler</code> and the original
 * class files should be replaced.
 *
 * @see javassist.tools.reflect.Reflection
 * @see javassist.tools.reflect.Compiler
 * @see javassist.Loader
 */
public class Loader extends javassist.Loader {
    protected Reflection reflection;

    /**
     * Loads a class with an instance of <code>Loader</code>
     * and calls <code>main()</code> in that class.
     *
     * @param args              command line parameters.
     * <ul>
     * <code>args[0]</code> is the class name to be loaded.
     * <br><code>args[1..n]</code> are parameters passed
     *                      to the target <code>main()</code>.
     * </ul>
     */
    public static void main(String[] args) throws Throwable {
        Loader cl = new Loader();
        cl.run(args);
    }

    /**
     * Constructs a new class loader.
     */
    public Loader() throws CannotCompileException, NotFoundException {
        super();
        delegateLoadingOf("javassist.tools.reflect.Loader");

        reflection = new Reflection();
        ClassPool pool = ClassPool.getDefault();
        addTranslator(pool, reflection);
    }

    /**
     * Produces a reflective class.
     * If the super class is also made reflective, it must be done
     * before the sub class.
     *
     * @param clazz             the reflective class.
     * @param metaobject        the class of metaobjects.
     *                          It must be a subclass of
     *                          <code>Metaobject</code>.
     * @param metaclass         the class of the class metaobject.
     *                          It must be a subclass of
     *                          <code>ClassMetaobject</code>.
     * @return <code>false</code>       if the class is already reflective.
     *
     * @see javassist.tools.reflect.Metaobject
     * @see javassist.tools.reflect.ClassMetaobject
     */
    public boolean makeReflective(String clazz,
                                  String metaobject, String metaclass)
        throws CannotCompileException, NotFoundException
    {
        return reflection.makeReflective(clazz, metaobject, metaclass);
    }
}
