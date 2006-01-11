package sample.evolve;

import javassist.*;

/**
 * Evolution provides a set of methods for instrumenting bytecodes.
 * 
 * For class evolution, updatable class A is renamed to B. Then an abstract
 * class named A is produced as the super class of B. If the original class A
 * has a public method m(), then the abstract class A has an abstract method
 * m().
 * 
 * abstract class A abstract m() _makeInstance() | class A --------> class B m()
 * m()
 * 
 * Also, all the other classes are translated so that "new A(i)" in the methods
 * is replaced with "_makeInstance(i)". This makes it possible to change the
 * behavior of the instantiation of the class A.
 */
public class Evolution implements Translator {
    public final static String handlerMethod = "_makeInstance";

    public final static String latestVersionField = VersionManager.latestVersionField;

    public final static String versionManagerMethod = "initialVersion";

    private static CtMethod trapMethod;

    private static final int initialVersion = 0;

    private ClassPool pool;

    private String updatableClassName = null;

    private CtClass updatableClass = null;

    public void start(ClassPool _pool) throws NotFoundException {
        pool = _pool;

        // Get the definition of Sample.make() and store it into trapMethod
        // for later use.
        trapMethod = _pool.getMethod("sample.evolve.Sample", "make");
    }

    public void onLoad(ClassPool _pool, String classname)
            throws NotFoundException, CannotCompileException {
        onLoadUpdatable(classname);

        /*
         * Replaces all the occurrences of the new operator with a call to
         * _makeInstance().
         */
        CtClass clazz = _pool.get(classname);
        CtClass absClass = updatableClass;
        CodeConverter converter = new CodeConverter();
        converter.replaceNew(absClass, absClass, handlerMethod);
        clazz.instrument(converter);
    }

    private void onLoadUpdatable(String classname) throws NotFoundException,
            CannotCompileException {
        // if the class is a concrete class,
        // classname is <updatableClassName>$$<version>.

        int i = classname.lastIndexOf("$$");
        if (i <= 0)
            return;

        String orgname = classname.substring(0, i);
        if (!orgname.equals(updatableClassName))
            return;

        int version;
        try {
            version = Integer.parseInt(classname.substring(i + 2));
        }
        catch (NumberFormatException e) {
            throw new NotFoundException(classname, e);
        }

        CtClass clazz = pool.getAndRename(orgname, classname);
        makeConcreteClass(clazz, updatableClass, version);
    }

    /*
     * Register an updatable class.
     */
    public void makeUpdatable(String classname) throws NotFoundException,
            CannotCompileException {
        if (pool == null)
            throw new RuntimeException(
                    "Evolution has not been linked to ClassPool.");

        CtClass c = pool.get(classname);
        updatableClassName = classname;
        updatableClass = makeAbstractClass(c);
    }

    /**
     * Produces an abstract class.
     */
    protected CtClass makeAbstractClass(CtClass clazz)
            throws CannotCompileException, NotFoundException {
        int i;

        CtClass absClass = pool.makeClass(clazz.getName());
        absClass.setModifiers(Modifier.PUBLIC | Modifier.ABSTRACT);
        absClass.setSuperclass(clazz.getSuperclass());
        absClass.setInterfaces(clazz.getInterfaces());

        // absClass.inheritAllConstructors();

        CtField fld = new CtField(pool.get("java.lang.Class"),
                latestVersionField, absClass);
        fld.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

        CtField.Initializer finit = CtField.Initializer.byCall(pool
                .get("sample.evolve.VersionManager"), versionManagerMethod,
                new String[] { clazz.getName() });
        absClass.addField(fld, finit);

        CtField[] fs = clazz.getDeclaredFields();
        for (i = 0; i < fs.length; ++i) {
            CtField f = fs[i];
            if (Modifier.isPublic(f.getModifiers()))
                absClass.addField(new CtField(f.getType(), f.getName(),
                        absClass));
        }

        CtConstructor[] cs = clazz.getDeclaredConstructors();
        for (i = 0; i < cs.length; ++i) {
            CtConstructor c = cs[i];
            int mod = c.getModifiers();
            if (Modifier.isPublic(mod)) {
                CtMethod wm = CtNewMethod.wrapped(absClass, handlerMethod, c
                        .getParameterTypes(), c.getExceptionTypes(),
                        trapMethod, null, absClass);
                wm.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
                absClass.addMethod(wm);
            }
        }

        CtMethod[] ms = clazz.getDeclaredMethods();
        for (i = 0; i < ms.length; ++i) {
            CtMethod m = ms[i];
            int mod = m.getModifiers();
            if (Modifier.isPublic(mod))
                if (Modifier.isStatic(mod))
                    throw new CannotCompileException(
                            "static methods are not supported.");
                else {
                    CtMethod m2 = CtNewMethod.abstractMethod(m.getReturnType(),
                            m.getName(), m.getParameterTypes(), m
                                    .getExceptionTypes(), absClass);
                    absClass.addMethod(m2);
                }
        }

        return absClass;
    }

    /**
     * Modifies the given class file so that it is a subclass of the abstract
     * class produced by makeAbstractClass().
     * 
     * Note: the naming convention must be consistent with
     * VersionManager.update().
     */
    protected void makeConcreteClass(CtClass clazz, CtClass abstractClass,
            int version) throws CannotCompileException, NotFoundException {
        int i;
        clazz.setSuperclass(abstractClass);
        CodeConverter converter = new CodeConverter();
        CtField[] fs = clazz.getDeclaredFields();
        for (i = 0; i < fs.length; ++i) {
            CtField f = fs[i];
            if (Modifier.isPublic(f.getModifiers()))
                converter.redirectFieldAccess(f, abstractClass, f.getName());
        }

        CtConstructor[] cs = clazz.getDeclaredConstructors();
        for (i = 0; i < cs.length; ++i)
            cs[i].instrument(converter);

        CtMethod[] ms = clazz.getDeclaredMethods();
        for (i = 0; i < ms.length; ++i)
            ms[i].instrument(converter);
    }
}
