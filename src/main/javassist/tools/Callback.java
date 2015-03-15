package javassist.tools;

import javassist.CannotCompileException;
import javassist.CtBehavior;

import java.util.HashMap;
import java.util.UUID;

/**
 * Creates bytecode that when executed calls back to the instance's result method.
 *
 * Inserts callbacks in <code>CtBehaviour</code>
 *
 */
public abstract class Callback {

    public static HashMap<String, Callback> callbacks = new HashMap<String, Callback>();

    private final String sourceCode;

    /**
     * Constructs a new <code>Callback</code> object.
     *
     * @param src       The source code representing the inserted callback bytecode.
     *                  Can be one or many single statements or blocks each returning one object.
     *                  If many single statements or blocks are used they must be comma separated.
     */
    public Callback(String src){
        String uuid = UUID.randomUUID().toString();
        callbacks.put(uuid, this);
        sourceCode = "((javassist.tools.Callback) javassist.tools.Callback.callbacks.get(\""+uuid+"\")).result(new Object[]{"+src+"});";
    }

    /**
     * Gets called when bytecode is executed
     *
     * @param objects   Objects that the bytecode in callback returns
     */
    public abstract void result(Object... objects);

    @Override
    public String toString(){
        return sourceCode();
    }

    public String sourceCode(){
        return sourceCode;
    }

    /**
     * Inserts callback at the beginning of the body.
     *
     * @param callback  The callback
     *
     * @see CtBehavior#insertBefore(String)
     */
    public static void insertBefore(CtBehavior behavior, Callback callback)
            throws CannotCompileException
    {
        behavior.insertBefore(callback.toString());
    }

    /**
     * Inserts callback at the end of the body.
     * The callback is inserted just before every return instruction.
     * It is not executed when an exception is thrown.
     *
     * @param behavior  The behaviour to insert callback in
     * @param callback  The callback
     *
     * @see CtBehavior#insertAfter(String, boolean)
     */
    public static void insertAfter(CtBehavior behavior,Callback callback)
            throws CannotCompileException
    {
        behavior.insertAfter(callback.toString(), false);
    }

    /**
     * Inserts callback at the end of the body.
     * The callback is inserted just before every return instruction.
     * It is not executed when an exception is thrown.
     *
     * @param behavior  The behaviour to insert callback in
     * @param callback  The callback representing the inserted.
     * @param asFinally True if the inserted is executed
     *                  Not only when the control normally returns
     *                  but also when an exception is thrown.
     *                  If this parameter is true, the inserted code cannot
     *                  access local variables.
     *
     * @see CtBehavior#insertAfter(String, boolean)
     */
    public static void insertAfter(CtBehavior behavior, Callback callback, boolean asFinally)
            throws CannotCompileException
    {
        behavior.insertAfter(callback.toString(), asFinally);
    }

    /**
     * Inserts callback at the specified line in the body.
     *
     * @param behavior  The behaviour to insert callback in
     * @param callback  The callback representing.
     * @param lineNum   The line number.  The callback is inserted at the
     *                  beginning of the code at the line specified by this
     *                  line number.
     *
     * @return      The line number at which the callback has been inserted.
     *
     * @see CtBehavior#insertAt(int, String)
     */
    public static int insertAt(CtBehavior behavior, Callback callback, int lineNum)
            throws CannotCompileException
    {
        return behavior.insertAt(lineNum, callback.toString());
    }
}
