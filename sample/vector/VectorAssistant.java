package sample.vector;

import java.io.IOException;
import javassist.*;
import sample.preproc.Assistant;

/**
 * This is a Javassist program which produce a new class representing
 * vectors of a given type.  For example,
 *
 * <ul>import java.util.Vector by sample.vector.VectorAssistant(int)</ul>
 *
 * <p>requests the Javassist preprocessor to substitute the following
 * lines for the original import declaration:
 *
 * <ul><pre>
 * import java.util.Vector;
 * import sample.vector.intVector;
 * </pre></ul>
 *
 * <p>The Javassist preprocessor calls <code>VectorAssistant.assist()</code>
 * and produces class <code>intVector</code> equivalent to:
 *
 * <ul><pre>
 * package sample.vector;
 *
 * public class intVector extends Vector {
 *   pubilc void add(int value) {
 *     addElement(new Integer(value));
 *   }
 *
 *   public int at(int index) {
 *     return elementAt(index).intValue();
 *   }
 * }
 * </pre></ul>
 *
 * <p><code>VectorAssistant.assist()</code> uses
 * <code>sample.vector.Sample</code> and <code>sample.vector.Sample2</code>
 * as a template to produce the methods <code>add()</code> and
 * <code>at()</code>.
 */
public class VectorAssistant implements Assistant {
    public final String packageName = "sample.vector.";

    /**
     * Calls <code>makeSubclass()</code> and produces a new vector class.
     * This method is called by a <code>sample.preproc.Compiler</code>.
     *
     * @see sample.preproc.Compiler
     */
    public CtClass[] assist(ClassPool pool, String vec, String[] args)
	throws CannotCompileException
    {
	if (args.length != 1)
	    throw new CannotCompileException(
			"VectorAssistant receives a single argument.");

	try {
	    CtClass subclass;
	    CtClass elementType = pool.get(args[0]);
	    if (elementType.isPrimitive())
		subclass = makeSubclass2(pool, elementType);
	    else
		subclass = makeSubclass(pool, elementType);

	    CtClass[] results = { subclass, pool.get(vec) };
	    return results;
	}
	catch (NotFoundException e) {
	    throw new CannotCompileException(e);
	}
	catch (IOException e) {
	    throw new CannotCompileException(e);
	}
    }

    /**
     * Produces a new vector class.  This method does not work if
     * the element type is a primitive type.
     *
     * @param type	the type of elements
     */
    public CtClass makeSubclass(ClassPool pool, CtClass type)
	throws CannotCompileException, NotFoundException, IOException
    {
	CtClass vec = pool.makeClass(makeClassName(type));
	vec.setSuperclass(pool.get("java.util.Vector"));

	CtClass c = pool.get("sample.vector.Sample");
	CtMethod addmethod = c.getDeclaredMethod("add");
	CtMethod atmethod = c.getDeclaredMethod("at");

	ClassMap map = new ClassMap();
	map.put("sample.vector.X", type.getName());

	vec.addMethod(CtNewMethod.copy(addmethod, "add", vec, map));
	vec.addMethod(CtNewMethod.copy(atmethod, "at", vec, map));
	vec.writeFile();
	return vec;
    }

    /**
     * Produces a new vector class.  This uses wrapped methods so that
     * the element type can be a primitive type.
     *
     * @param type	the type of elements
     */
    public CtClass makeSubclass2(ClassPool pool, CtClass type)
	throws CannotCompileException, NotFoundException, IOException
    {
	CtClass vec = pool.makeClass(makeClassName(type));
	vec.setSuperclass(pool.get("java.util.Vector"));

	CtClass c = pool.get("sample.vector.Sample2");
	CtMethod addmethod = c.getDeclaredMethod("add");
	CtMethod atmethod = c.getDeclaredMethod("at");

	CtClass[] args1 = { type };
	CtClass[] args2 = { CtClass.intType };
	CtMethod m
	    = CtNewMethod.wrapped(CtClass.voidType, "add", args1,
				  null, addmethod, null, vec);
	vec.addMethod(m);
	m = CtNewMethod.wrapped(type, "at", args2,
				null, atmethod, null, vec);
	vec.addMethod(m);
	vec.writeFile();
	return vec;
    }

    private String makeClassName(CtClass type) {
	return packageName + type.getSimpleName() + "Vector";
    }
}
