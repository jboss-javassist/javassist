package javassist.expr;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.MethodInfo;

/**
 * Constructor call such as <code>this()</code> and <code>super()</code>
 * within a constructor body.
 *
 * @see NewExpr
 */
public class ConstructorCall extends MethodCall {
    /**
     * Undocumented constructor.  Do not use; internal-use only.
     */
    protected ConstructorCall(int pos, CodeIterator i, CtClass decl, MethodInfo m) {
        super(pos, i, decl, m);
    }

    /**
     * Returns <code>"super"</code> or "<code>"this"</code>.
     */
    public String getMethodName() {
        return isSuper() ? "super" : "this";
    }

    /**
     * Always throws a <code>NotFoundException</code>.
     *
     * @see #getConstructor()
     */
    public CtMethod getMethod() throws NotFoundException {
        throw new NotFoundException("this is a constructor call.  Call getConstructor().");
    }

    /**
     * Returns the called constructor.
     */
    public CtConstructor getConstructor() throws NotFoundException {
        return getCtClass().getConstructor(getSignature());
    }

    /**
     * Returns true if the called constructor is not <code>this()</code>
     * but <code>super()</code> (a constructor declared in the super class).
     */
    public boolean isSuper() {
        return super.isSuper();
    }
}
