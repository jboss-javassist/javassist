/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2004 Shigeru Chiba. All Rights Reserved.
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

package javassist;

import javassist.bytecode.*;
import javassist.convert.*;

/**
 * Simple translator of method bodies
 * (also see the <code>javassist.expr</code> package).
 *
 * <p>Instances of this class specifies how to instrument of the
 * bytecodes representing a method body.  They are passed to
 * <code>CtClass.instrument()</code> or
 * <code>CtMethod.instrument()</code> as a parameter.
 *
 * <p>Example:
 * <ul><pre>
 * ClassPool cp = ClassPool.getDefault();
 * CtClass point = cp.get("Point");
 * CtClass singleton = cp.get("Singleton");
 * CtClass client = cp.get("Client");
 * CodeConverter conv = new CodeConverter();
 * conv.replaceNew(point, singleton, "makePoint");
 * client.instrument(conv);
 * </pre></ul>
 *
 * <p>This program substitutes "<code>Singleton.makePoint()</code>"
 * for all occurrences of "<code>new Point()</code>"
 * appearing in methods declared in a <code>Client</code> class.
 *
 * @see javassist.CtClass#instrument(CodeConverter)
 * @see javassist.CtMethod#instrument(CodeConverter)
 * @see javassist.expr.ExprEditor
 */
public class CodeConverter {
    Transformer transformers = null;

    /**
     * Modify a method body so that instantiation of the specified class
     * is replaced with a call to the specified static method.  For example,
     * <code>replaceNew(ctPoint, ctSingleton, "createPoint")</code>
     * (where <code>ctPoint</code> and <code>ctSingleton</code> are
     * compile-time classes for class <code>Point</code> and class
     * <code>Singleton</code>, respectively)
     * replaces all occurrences of:
     *
     * <ul><code>new Point(x, y)</code></ul>
     *
     * in the method body with:
     *
     * <ul><code>Singleton.createPoint(x, y)</code></ul>
     * 
     * <p>This enables to intercept instantiation of <code>Point</code>
     * and change the samentics.  For example, the following
     * <code>createPoint()</code> implements the singleton pattern:
     *
     * <ul><pre>public static Point createPoint(int x, int y) {
     *     if (aPoint == null)
     *         aPoint = new Point(x, y);
     *     return aPoint;
     * }
     * </pre></ul>
     *
     * <p>The static method call substituted for the original <code>new</code>
     * expression must be
     * able to receive the same set of parameters as the original
     * constructor.  If there are multiple constructors with different
     * parameter types, then there must be multiple static methods
     * with the same name but different parameter types.
     *
     * <p>The return type of the substituted static method must be
     * the exactly same as the type of the instantiated class specified by
     * <code>newClass</code>.
     *
     * @param newClass          the instantiated class.
     * @param calledClass       the class in which the static method is
     *                          declared.
     * @param calledMethod      the name of the static method.
     */
    public void replaceNew(CtClass newClass,
                           CtClass calledClass, String calledMethod) {
        transformers = new TransformNew(transformers, newClass.getName(),
                                        calledClass.getName(), calledMethod);
    }

    /**
     * Modify a method body so that field read/write expressions access
     * a different field from the original one.
     *
     * <p>Note that this method changes only the filed name and the class
     * declaring the field; the type of the target object does not change.
     * Therefore, the substituted field must be declared in the same class
     * or a superclass of the original class.
     *
     * <p>Also, <code>clazz</code> and <code>newClass</code> must specify
     * the class directly declaring the field.  They must not specify
     * a subclass of that class.
     *
     * @param field             the originally accessed field.
     * @param newClass  the class declaring the substituted field.
     * @param newFieldname      the name of the substituted field.
     */
    public void redirectFieldAccess(CtField field,
                                    CtClass newClass, String newFieldname) {
        transformers = new TransformFieldAccess(transformers, field,
                                                newClass.getName(),
                                                newFieldname);
    }

    /**
     * Modify a method body so that an expression reading the specified
     * field is replaced with a call to the specified <i>static</i> method.
     * This static method receives the target object of the original
     * read expression as a parameter.  It must return a value of
     * the same type as the field.
     *
     * <p>For example, the program below
     *
     * <ul><pre>Point p = new Point();
     * int newX = p.x + 3;</pre></ul>
     *
     * <p>can be translated into:
     *
     * <ul><pre>Point p = new Point();
     * int newX = Accessor.readX(p) + 3;</pre></ul>
     *
     * <p>where
     *
     * <ul><pre>public class Accessor {
     *     public static int readX(Object target) { ... }
     * }</pre></ul>
     *
     * <p>The type of the parameter of <code>readX()</code> must
     * be <code>java.lang.Object</code> independently of the actual
     * type of <code>target</code>.  The return type must be the same
     * as the field type.
     *
     * @param field             the field.
     * @param calledClass       the class in which the static method is
     *                          declared.
     * @param calledMethod      the name of the static method.
     */
    public void replaceFieldRead(CtField field,
                                 CtClass calledClass, String calledMethod) {
        transformers = new TransformReadField(transformers, field,
                                              calledClass.getName(),
                                              calledMethod);
    }

    /**
     * Modify a method body so that an expression writing the specified
     * field is replaced with a call to the specified static method.
     * This static method receives two parameters: the target object of
     * the original
     * write expression and the assigned value.  The return type of the
     * static method is <code>void</code>.
     * 
     * <p>For example, the program below
     *
     * <ul><pre>Point p = new Point();
     * p.x = 3;</pre></ul>
     *
     * <p>can be translated into:
     *
     * <ul><pre>Point p = new Point();
     * Accessor.writeX(3);</pre></ul>
     *
     * <p>where
     *
     * <ul><pre>public class Accessor {
     *     public static void writeX(Object target, int value) { ... }
     * }</pre></ul>
     *
     * <p>The type of the first parameter of <code>writeX()</code> must
     * be <code>java.lang.Object</code> independently of the actual
     * type of <code>target</code>.  The type of the second parameter
     * is the same as the field type.
     *
     * @param field             the field.
     * @param calledClass       the class in which the static method is
     *                          declared.
     * @param calledMethod      the name of the static method.
     */
    public void replaceFieldWrite(CtField field,
                                  CtClass calledClass, String calledMethod) {
        transformers = new TransformWriteField(transformers, field,
                                               calledClass.getName(),
                                               calledMethod);
    }

    /**
     * Modify method invocations in a method body so that a different
     * method is invoked.
     *
     * <p>Note that the target object, the parameters, or 
     * the type of invocation
     * (static method call, interface call, or private method call)
     * are not modified.  Only the method name is changed.  The substituted
     * method must have the same signature that the original one has.
     * If the original method is a static method, the substituted method
     * must be static.
     *
     * @param origMethod        original method
     * @param substMethod       substituted method
     */
    public void redirectMethodCall(CtMethod origMethod,
                                   CtMethod substMethod)
        throws CannotCompileException
    {
        String d1 = origMethod.getMethodInfo2().getDescriptor();
        String d2 = substMethod.getMethodInfo2().getDescriptor();
        if (!d1.equals(d2))
            throw new CannotCompileException("signature mismatch");

        transformers = new TransformCall(transformers, origMethod,
                                         substMethod);
    }

    /**
     * Insert a call to another method before an existing method call.
     * That "before" method must be static.  The return type must be
     * <code>void</code>.  As parameters, the before method receives
     * the target object and all the parameters to the originally invoked
     * method.  For example, if the originally invoked method is
     * <code>move()</code>:
     *
     * <ul><pre>class Point {
     *     Point move(int x, int y) { ... }
     * }</pre></ul>
     *
     * <p>Then the before method must be something like this:
     *
     * <ul><pre>class Verbose {
     *     static void print(Point target, int x, int y) { ... }
     * }</pre></ul>
     *
     * <p>The <code>CodeConverter</code> would translate bytecode
     * equivalent to:
     *
     * <ul><pre>Point p2 = p.move(x + y, 0);</pre></ul>
     *
     * <p>into the bytecode equivalent to:
     *
     * <ul><pre>int tmp1 = x + y;
     * int tmp2 = 0;
     * Verbose.print(p, tmp1, tmp2);
     * Point p2 = p.move(tmp1, tmp2);</pre></ul>
     *
     * @param origMethod        the method originally invoked.
     * @param beforeMethod      the method invoked before
     *                          <code>origMethod</code>.
     */
    public void insertBeforeMethod(CtMethod origMethod,
                                   CtMethod beforeMethod)
        throws CannotCompileException
    {
        try {
            transformers = new TransformBefore(transformers, origMethod,
                                               beforeMethod);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
    }

    /**
     * Inserts a call to another method after an existing method call.
     * That "after" method must be static.  The return type must be
     * <code>void</code>.  As parameters, the after method receives
     * the target object and all the parameters to the originally invoked
     * method.  For example, if the originally invoked method is
     * <code>move()</code>:
     *
     * <ul><pre>class Point {
     *     Point move(int x, int y) { ... }
     * }</pre></ul>
     *
     * <p>Then the after method must be something like this:
     *
     * <ul><pre>class Verbose {
     *     static void print(Point target, int x, int y) { ... }
     * }</pre></ul>
     *
     * <p>The <code>CodeConverter</code> would translate bytecode
     * equivalent to:
     *
     * <ul><pre>Point p2 = p.move(x + y, 0);</pre></ul>
     *
     * <p>into the bytecode equivalent to:
     *
     * <ul><pre>int tmp1 = x + y;
     * int tmp2 = 0;
     * Point p2 = p.move(tmp1, tmp2);
     * Verbose.print(p, tmp1, tmp2);</pre></ul>
     *
     * @param origMethod        the method originally invoked.
     * @param afterMethod       the method invoked after
     *                          <code>origMethod</code>.
     */
    public void insertAfterMethod(CtMethod origMethod,
                                  CtMethod afterMethod)
        throws CannotCompileException
    {
        try {
            transformers = new TransformAfter(transformers, origMethod,
                                               afterMethod);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
    }

    /**
     * Performs code conversion.
     */
    void doit(CtClass clazz, MethodInfo minfo, ConstPool cp)
        throws CannotCompileException
    {
        Transformer t;

        CodeAttribute codeAttr = minfo.getCodeAttribute();
        if (codeAttr == null || transformers == null)
            return;

        for (t = transformers; t != null; t = t.getNext())
            t.initialize(cp, codeAttr);

        CodeIterator iterator = codeAttr.iterator();
        while (iterator.hasNext()) {
            try {
                int pos = iterator.next();
                for (t = transformers; t != null; t = t.getNext())
                    pos = t.transform(clazz, pos, iterator, cp);
            }
            catch (BadBytecode e) {
                throw new CannotCompileException(e);
            }
        }

        int locals = 0;
        for (t = transformers; t != null; t = t.getNext()) {
            int s = t.extraLocals();
            if (s > locals)
                locals = s;
        }

        for (t = transformers; t != null; t = t.getNext())
            t.clean();

        codeAttr.setMaxLocals(codeAttr.getMaxLocals() + locals);
    }
}
