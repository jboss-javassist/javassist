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

package javassist.tools.reflect;

import javassist.*;
import javassist.CtMethod.ConstParameter;

/**
 * The class implementing the behavioral reflection mechanism.
 *
 * <p>If a class is reflective,
 * then all the method invocations on every
 * instance of that class are intercepted by the runtime
 * metaobject controlling that instance.  The methods inherited from the
 * super classes are also intercepted except final methods.  To intercept
 * a final method in a super class, that super class must be also reflective.
 *
 * <p>To do this, the original class file representing a reflective class:
 *
 * <ul><pre>
 * class Person {
 *   public int f(int i) { return i + 1; }
 *   public int value;
 * }
 * </pre></ul>
 *
 * <p>is modified so that it represents a class:
 *
 * <ul><pre>
 * class Person implements Metalevel {
 *   public int _original_f(int i) { return i + 1; }
 *   public int f(int i) { <i>delegate to the metaobject</i> }
 *
 *   public int value;
 *   public int _r_value() { <i>read "value"</i> }
 *   public void _w_value(int v) { <i>write "value"</i> }
 *
 *   public ClassMetaobject _getClass() { <i>return a class metaobject</i> }
 *   public Metaobject _getMetaobject() { <i>return a metaobject</i> }
 *   public void _setMetaobject(Metaobject m) { <i>change a metaobject</i> }
 * }
 * </pre></ul>
 *
 * @see javassist.tools.reflect.ClassMetaobject
 * @see javassist.tools.reflect.Metaobject
 * @see javassist.tools.reflect.Loader
 * @see javassist.tools.reflect.Compiler
 */
public class Reflection implements Translator {

    static final String classobjectField = "_classobject";
    static final String classobjectAccessor = "_getClass";
    static final String metaobjectField = "_metaobject";
    static final String metaobjectGetter = "_getMetaobject";
    static final String metaobjectSetter = "_setMetaobject";
    static final String readPrefix = "_r_";
    static final String writePrefix = "_w_";

    static final String metaobjectClassName = "javassist.tools.reflect.Metaobject";
    static final String classMetaobjectClassName
        = "javassist.tools.reflect.ClassMetaobject";

    protected CtMethod trapMethod, trapStaticMethod;
    protected CtMethod trapRead, trapWrite;
    protected CtClass[] readParam;

    protected ClassPool classPool;
    protected CodeConverter converter;

    private boolean isExcluded(String name) {
        return name.startsWith(ClassMetaobject.methodPrefix)
            || name.equals(classobjectAccessor)
            || name.equals(metaobjectSetter)
            || name.equals(metaobjectGetter)
            || name.startsWith(readPrefix)
            || name.startsWith(writePrefix);
    }

    /**
     * Constructs a new <code>Reflection</code> object.
     */
    public Reflection() {
        classPool = null;
        converter = new CodeConverter();
    }

    /**
     * Initializes the object.
     */
    public void start(ClassPool pool) throws NotFoundException {
        classPool = pool;
        final String msg
            = "javassist.tools.reflect.Sample is not found or broken.";
        try {
            CtClass c = classPool.get("javassist.tools.reflect.Sample");
            trapMethod = c.getDeclaredMethod("trap");
            trapStaticMethod = c.getDeclaredMethod("trapStatic");
            trapRead = c.getDeclaredMethod("trapRead");
            trapWrite = c.getDeclaredMethod("trapWrite");
            readParam
                = new CtClass[] { classPool.get("java.lang.Object") };
        }
        catch (NotFoundException e) {
            throw new RuntimeException(msg);
        }
    }

    /**
     * Inserts hooks for intercepting accesses to the fields declared
     * in reflective classes.
     */
    public void onLoad(ClassPool pool, String classname)
        throws CannotCompileException, NotFoundException
    {
        CtClass clazz = pool.get(classname);
        clazz.instrument(converter);
    }

    /**
     * Produces a reflective class.
     * If the super class is also made reflective, it must be done
     * before the sub class.
     *
     * @param classname         the name of the reflective class
     * @param metaobject        the class name of metaobjects.
     * @param metaclass         the class name of the class metaobject.
     * @return <code>false</code>       if the class is already reflective.
     *
     * @see javassist.tools.reflect.Metaobject
     * @see javassist.tools.reflect.ClassMetaobject
     */
    public boolean makeReflective(String classname,
                                  String metaobject, String metaclass)
        throws CannotCompileException, NotFoundException
    {
        return makeReflective(classPool.get(classname),
                              classPool.get(metaobject),
                              classPool.get(metaclass));
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
    public boolean makeReflective(Class clazz,
                                  Class metaobject, Class metaclass)
        throws CannotCompileException, NotFoundException
    {
        return makeReflective(clazz.getName(), metaobject.getName(),
                              metaclass.getName());
    }

    /**
     * Produces a reflective class.  It modifies the given
     * <code>CtClass</code> object and makes it reflective.
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
    public boolean makeReflective(CtClass clazz,
                                  CtClass metaobject, CtClass metaclass)
        throws CannotCompileException, CannotReflectException,
               NotFoundException
    {
        if (clazz.isInterface())
            throw new CannotReflectException(
                    "Cannot reflect an interface: " + clazz.getName());

        if (clazz.subclassOf(classPool.get(classMetaobjectClassName)))
            throw new CannotReflectException(
                "Cannot reflect a subclass of ClassMetaobject: "
                + clazz.getName());

        if (clazz.subclassOf(classPool.get(metaobjectClassName)))
            throw new CannotReflectException(
                "Cannot reflect a subclass of Metaobject: "
                + clazz.getName());

        registerReflectiveClass(clazz);
        return modifyClassfile(clazz, metaobject, metaclass);
    }

    /**
     * Registers a reflective class.  The field accesses to the instances
     * of this class are instrumented.
     */
    private void registerReflectiveClass(CtClass clazz) {
        CtField[] fs = clazz.getDeclaredFields();
        for (int i = 0; i < fs.length; ++i) {
            CtField f = fs[i];
            int mod = f.getModifiers();
            if ((mod & Modifier.PUBLIC) != 0 && (mod & Modifier.FINAL) == 0) {
                String name = f.getName();
                converter.replaceFieldRead(f, clazz, readPrefix + name);
                converter.replaceFieldWrite(f, clazz, writePrefix + name);
            }
        }
    }

    private boolean modifyClassfile(CtClass clazz, CtClass metaobject,
                                    CtClass metaclass)
        throws CannotCompileException, NotFoundException
    {
        if (clazz.getAttribute("Reflective") != null)
            return false;       // this is already reflective.
        else
            clazz.setAttribute("Reflective", new byte[0]);

        CtClass mlevel = classPool.get("javassist.tools.reflect.Metalevel");
        boolean addMeta = !clazz.subtypeOf(mlevel);
        if (addMeta)
            clazz.addInterface(mlevel);

        processMethods(clazz, addMeta);
        processFields(clazz);

        CtField f;
        if (addMeta) {
            f = new CtField(classPool.get("javassist.tools.reflect.Metaobject"),
                            metaobjectField, clazz);
            f.setModifiers(Modifier.PROTECTED);
            clazz.addField(f, CtField.Initializer.byNewWithParams(metaobject));

            clazz.addMethod(CtNewMethod.getter(metaobjectGetter, f));
            clazz.addMethod(CtNewMethod.setter(metaobjectSetter, f));
        }

        f = new CtField(classPool.get("javassist.tools.reflect.ClassMetaobject"),
                        classobjectField, clazz);
        f.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
        clazz.addField(f, CtField.Initializer.byNew(metaclass,
                                        new String[] { clazz.getName() }));

        clazz.addMethod(CtNewMethod.getter(classobjectAccessor, f));
        return true;
    }

    private void processMethods(CtClass clazz, boolean dontSearch)
        throws CannotCompileException, NotFoundException
    {
        CtMethod[] ms = clazz.getMethods();
        for (int i = 0; i < ms.length; ++i) {
            CtMethod m = ms[i];
            int mod = m.getModifiers();
            if (Modifier.isPublic(mod) && !Modifier.isAbstract(mod))
                processMethods0(mod, clazz, m, i, dontSearch);
        }
    }

    private void processMethods0(int mod, CtClass clazz,
                        CtMethod m, int identifier, boolean dontSearch)
        throws CannotCompileException, NotFoundException
    {
        CtMethod body;
        String name = m.getName();

        if (isExcluded(name))   // internally-used method inherited
            return;             // from a reflective class.

        CtMethod m2;
        if (m.getDeclaringClass() == clazz) {
            if (Modifier.isNative(mod))
                return;

            m2 = m;
            if (Modifier.isFinal(mod)) {
                mod &= ~Modifier.FINAL;
                m2.setModifiers(mod);
            }
        }
        else {
            if (Modifier.isFinal(mod))
                return;

            mod &= ~Modifier.NATIVE;
            m2 = CtNewMethod.delegator(findOriginal(m, dontSearch), clazz);
            m2.setModifiers(mod);
            clazz.addMethod(m2);
        }

        m2.setName(ClassMetaobject.methodPrefix + identifier
                      + "_" + name);

        if (Modifier.isStatic(mod))
            body = trapStaticMethod;
        else
            body = trapMethod;

        CtMethod wmethod
            = CtNewMethod.wrapped(m.getReturnType(), name,
                                  m.getParameterTypes(), m.getExceptionTypes(),
                                  body, ConstParameter.integer(identifier),
                                  clazz);
        wmethod.setModifiers(mod);
        clazz.addMethod(wmethod);
    }

    private CtMethod findOriginal(CtMethod m, boolean dontSearch)
        throws NotFoundException
    {
        if (dontSearch)
            return m;

        String name = m.getName();
        CtMethod[] ms = m.getDeclaringClass().getDeclaredMethods();
        for (int i = 0; i < ms.length; ++i) {
            String orgName = ms[i].getName();
            if (orgName.endsWith(name)
                && orgName.startsWith(ClassMetaobject.methodPrefix)
                && ms[i].getSignature().equals(m.getSignature()))
                return ms[i];
        }

        return m;
    }

    private void processFields(CtClass clazz)
        throws CannotCompileException, NotFoundException
    {
        CtField[] fs = clazz.getDeclaredFields();
        for (int i = 0; i < fs.length; ++i) {
            CtField f = fs[i];
            int mod = f.getModifiers();
            if ((mod & Modifier.PUBLIC) != 0 && (mod & Modifier.FINAL) == 0) {
                mod |= Modifier.STATIC;
                String name = f.getName();
                CtClass ftype = f.getType();
                CtMethod wmethod
                    = CtNewMethod.wrapped(ftype, readPrefix + name,
                                          readParam, null, trapRead,
                                          ConstParameter.string(name),
                                          clazz);
                wmethod.setModifiers(mod);
                clazz.addMethod(wmethod);
                CtClass[] writeParam = new CtClass[2];
                writeParam[0] = classPool.get("java.lang.Object");
                writeParam[1] = ftype;
                wmethod = CtNewMethod.wrapped(CtClass.voidType,
                                writePrefix + name,
                                writeParam, null, trapWrite,
                                ConstParameter.string(name), clazz);
                wmethod.setModifiers(mod);
                clazz.addMethod(wmethod);
            }
        }
    }
}
