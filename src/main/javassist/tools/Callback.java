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

package javassist.tools;

import javassist.CannotCompileException;
import javassist.CtBehavior;

import java.util.HashMap;
import java.util.UUID;

/**
 * Creates bytecode that when executed calls back to the instance's result method.
 *
 * <p>Example of how to create and insert a callback:</p>
 * <pre>
 * ctMethod.insertAfter(new Callback("Thread.currentThread()") {
 *     public void result(Object[] objects) {
 *         Thread thread = (Thread) objects[0];
 *         // do something with thread...
 *     }
 * }.sourceCode());
 * </pre>
 * <p>Contains utility methods for inserts callbacks in <code>CtBehaviour</code>, example:</p>
 * <pre>
 * insertAfter(ctBehaviour, new Callback("Thread.currentThread(), dummyString") {
 *     public void result(Object[] objects) {
 *         Thread thread = (Thread) objects[0];
 *         // do something with thread...
 *     }
 * });
 * </pre>
 *
 * @author Marten Hedborg
 * @author Shigeru Chiba
 */
public abstract class Callback {

    public static HashMap callbacks = new HashMap();

    private final String sourceCode;

    /**
     * Constructs a new <code>Callback</code> object.
     *
     * @param src       The source code representing the inserted callback bytecode.
     *                  Can be one or many single statements each returning one object.
     *                  If many single statements are used they must be comma separated.
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
    public abstract void result(Object[] objects);

    public String toString(){
        return sourceCode();
    }

    public String sourceCode(){
        return sourceCode;
    }

    /**
     * Utility method to insert callback at the beginning of the body.
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
     * Utility method to inserts callback at the end of the body.
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
     * Utility method to inserts callback at the end of the body.
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
     * Utility method to inserts callback at the specified line in the body.
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
