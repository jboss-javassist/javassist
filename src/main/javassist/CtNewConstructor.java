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

package javassist;

import javassist.bytecode.*;
import javassist.compiler.Javac;
import javassist.compiler.CompileError;
import javassist.CtMethod.ConstParameter;

/**
 * A collection of static methods for creating a <code>CtConstructor</code>.
 * An instance of this class does not make any sense.
 *
 * <p>A class initializer (static constructor) cannot be created by the
 * methods in this class.  Call <code>makeClassInitializer()</code> in
 * <code>CtClass</code> and append code snippet to the body of the class
 * initializer obtained by <code>makeClassInitializer()</code>.
 *
 * @see CtClass#addConstructor(CtConstructor)
 * @see CtClass#makeClassInitializer()
 */
public class CtNewConstructor {
    /**
     * Specifies that no parameters are passed to a super-class'
     * constructor.  That is, the default constructor is invoked.
     */
    public static final int PASS_NONE = 0;      // call super()

    /**
     * Specifies that parameters are converted into an array of
     * <code>Object</code> and passed to a super-class'
     * constructor.
     */
    public static final int PASS_ARRAY = 1;     // an array of parameters

    /**
     * Specifies that parameters are passed <i>as is</i>
     * to a super-class' constructor.  The signature of that
     * constructor must be the same as that of the created constructor.
     */
    public static final int PASS_PARAMS = 2;

    /**
     * Compiles the given source code and creates a constructor.
     * The source code must include not only the constructor body
     * but the whole declaration.
     *
     * @param src               the source text. 
     * @param declaring    the class to which the created constructor is added.
     */
    public static CtConstructor make(String src, CtClass declaring)
        throws CannotCompileException
    {
        Javac compiler = new Javac(declaring);
        try {
            CtMember obj = compiler.compile(src);
            if (obj instanceof CtConstructor) {
                // a stack map table has been already created.
                return (CtConstructor)obj;
            }
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }

        throw new CannotCompileException("not a constructor");
    }

    /**
     * Creates a public constructor.
     *
     * @param parameters        a list of the parameter types.
     * @param exceptions        a list of the exception types.
     * @param body              the source text of the constructor body.
     *                  It must be a block surrounded by <code>{}</code>.
     *                  If it is <code>null</code>, the substituted
     *                  constructor body does nothing except calling
     *                  <code>super()</code>.
     * @param declaring    the class to which the created method is added.
     */
    public static CtConstructor make(CtClass[] parameters,
                                     CtClass[] exceptions,
                                     String body, CtClass declaring)
        throws CannotCompileException
    {
        try {
            CtConstructor cc = new CtConstructor(parameters, declaring);
            cc.setExceptionTypes(exceptions);
            cc.setBody(body);
            return cc;
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
    }

    /**
     * Creates a copy of a constructor.
     * This is a convenience method for calling
     * {@link CtConstructor#CtConstructor(CtConstructor, CtClass, ClassMap) this constructor}.
     * See the description of the constructor for particular behavior of the copying.
     *
     * @param c         the copied constructor.
     * @param declaring    the class to which the created method is added.
     * @param map       the hash table associating original class names
     *                  with substituted names.
     *                  It can be <code>null</code>.
     *
     * @see CtConstructor#CtConstructor(CtConstructor,CtClass,ClassMap)
     */
    public static CtConstructor copy(CtConstructor c, CtClass declaring,
                                ClassMap map) throws CannotCompileException {
        return new CtConstructor(c, declaring, map);
    }

    /**
     * Creates a default (public) constructor.
     *
     * <p>The created constructor takes no parameter.  It calls
     * <code>super()</code>.
     */
    public static CtConstructor defaultConstructor(CtClass declaring)
        throws CannotCompileException
    {
        CtConstructor cons = new CtConstructor((CtClass[])null, declaring);

        ConstPool cp = declaring.getClassFile2().getConstPool();
        Bytecode code = new Bytecode(cp, 1, 1);
        code.addAload(0);
        try {
            code.addInvokespecial(declaring.getSuperclass(),
                                  "<init>", "()V");
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }

        code.add(Bytecode.RETURN);

        // no need to construct a stack map table.
        cons.getMethodInfo2().setCodeAttribute(code.toCodeAttribute());
        return cons;
    }

    /**
     * Creates a public constructor that only calls a constructor
     * in the super class.  The created constructor receives parameters
     * specified by <code>parameters</code> but calls the super's
     * constructor without those parameters (that is, it calls the default
     * constructor).
     *
     * <p>The parameters passed to the created constructor should be
     * used for field initialization.  <code>CtField.Initializer</code>
     * objects implicitly insert initialization code in constructor
     * bodies.
     *
     * @param parameters        parameter types
     * @param exceptions        exception types
     * @param declaring         the class to which the created constructor
     *                          is added.
     * @see CtField.Initializer#byParameter(int)
     */
    public static CtConstructor skeleton(CtClass[] parameters,
                        CtClass[] exceptions, CtClass declaring)
        throws CannotCompileException
    {
        return make(parameters, exceptions, PASS_NONE,
                    null, null, declaring);
    }

    /**
     * Creates a public constructor that only calls a constructor
     * in the super class.  The created constructor receives parameters
     * specified by <code>parameters</code> and calls the super's
     * constructor with those parameters.
     *
     * @param parameters        parameter types
     * @param exceptions        exception types
     * @param declaring         the class to which the created constructor
     *                          is added.
     */
    public static CtConstructor make(CtClass[] parameters,
                                     CtClass[] exceptions, CtClass declaring)
        throws CannotCompileException
    {
        return make(parameters, exceptions, PASS_PARAMS,
                    null, null, declaring);
    }

    /**
     * Creates a public constructor.
     *
     * <p>If <code>howto</code> is <code>PASS_PARAMS</code>,
     * the created constructor calls the super's constructor with the
     * same signature.  The superclass must contain
     * a constructor taking the same set of parameters as the created one.
     *
     * <p>If <code>howto</code> is <code>PASS_NONE</code>,
     * the created constructor calls the super's default constructor.
     * The superclass must contain a constructor taking no parameters.
     *
     * <p>If <code>howto</code> is <code>PASS_ARRAY</code>,
     * the created constructor calls the super's constructor
     * with the given parameters in the form of an array of
     * <code>Object</code>.  The signature of the super's constructor
     * must be:
     *
     * <ul><code>constructor(Object[] params, &lt;type&gt; cvalue)
     * </code></ul>
     *
     * <p>Here, <code>cvalue</code> is the constant value specified
     * by <code>cparam</code>.
     *
     * <p>If <code>cparam</code> is <code>null</code>, the signature
     * must be:
     *
     * <ul><code>constructor(Object[] params)</code></ul>
     *
     * <p>If <code>body</code> is not null, a copy of that method is
     * embedded in the body of the created constructor.
     * The embedded method is executed after
     * the super's constructor is called and the values of fields are
     * initialized.  Note that <code>body</code> must not
     * be a constructor but a method.
     *
     * <p>Since the embedded method is wrapped
     * in parameter-conversion code
     * as in <code>CtNewMethod.wrapped()</code>,
     * the constructor parameters are
     * passed in the form of an array of <code>Object</code>.
     * The method specified by <code>body</code> must have the
     * signature shown below:
     *
     * <ul><code>Object method(Object[] params, &lt;type&gt; cvalue)
     * </code></ul>
     *
     * <p>If <code>cparam</code> is <code>null</code>, the signature
     * must be:
     *
     * <ul><code>Object method(Object[] params)</code></ul>
     *
     * <p>Although the type of the returned value is <code>Object</code>,
     * the value must be always <code>null</code>.
     *
     * <p><i>Example:</i>
     *
     * <ul><pre>ClassPool pool = ... ;
     * CtClass xclass = pool.makeClass("X");
     * CtMethod method = pool.getMethod("Sample", "m");
     * xclass.setSuperclass(pool.get("Y"));
     * CtClass[] argTypes = { CtClass.intType };
     * ConstParameter cparam = ConstParameter.string("test");
     * CtConstructor c = CtNewConstructor.make(argTypes, null,
     *                                  PASS_PARAMS, method, cparam, xclass);
     * xclass.addConstructor(c);</pre></ul>
     *
     * <p>where the class <code>Sample</code> is as follows:
     *
     * <ul><pre>public class Sample {
     *     public Object m(Object[] args, String msg) {
     *         System.out.println(msg);
     *         return null;
     *     }
     * }</pre></ul>
     *
     * <p>This program produces the following class:
     *
     * <ul><pre>public class X extends Y {
     *     public X(int p0) {
     *         super(p0);
     *         String msg = "test";
     *         Object[] args = new Object[] { p0 };
     *         // begin of copied body
     *         System.out.println(msg);
     *         Object result = null;
     *         // end
     *     }
     * }</pre></ul>
     *
     * @param parameters        a list of the parameter types
     * @param exceptions        a list of the exceptions
     * @param howto             how to pass parameters to the super-class'
     *                          constructor (<code>PASS_NONE</code>,
     *                          <code>PASS_ARRAY</code>,
     *                          or <code>PASS_PARAMS</code>)
     * @param body              appended body (may be <code>null</code>).
     *                          It must be not a constructor but a method.
     * @param cparam            constant parameter (may be <code>null</code>.)
     * @param declaring         the class to which the created constructor
     *                          is added.
     *
     * @see CtNewMethod#wrapped(CtClass,String,CtClass[],CtClass[],CtMethod,CtMethod.ConstParameter,CtClass)
     */
    public static CtConstructor make(CtClass[] parameters,
                                     CtClass[] exceptions, int howto,
                                     CtMethod body, ConstParameter cparam,
                                     CtClass declaring)
        throws CannotCompileException
    {
        return CtNewWrappedConstructor.wrapped(parameters, exceptions,
                                        howto, body, cparam, declaring);
    }
}
