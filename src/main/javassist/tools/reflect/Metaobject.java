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

import java.lang.reflect.Method;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A runtime metaobject.
 *
 * <p>A <code>Metaobject</code> is created for
 * every object at the base level.  A different reflective object is
 * associated with a different metaobject.
 *
 * <p>The metaobject intercepts method calls
 * on the reflective object at the base-level.  To change the behavior
 * of the method calls, a subclass of <code>Metaobject</code>
 * should be defined.
 *
 * <p>To obtain a metaobject, calls <code>_getMetaobject()</code>
 * on a reflective object.  For example,
 *
 * <ul><pre>Metaobject m = ((Metalevel)reflectiveObject)._getMetaobject();
 * </pre></ul>
 *
 * @see javassist.tools.reflect.ClassMetaobject
 * @see javassist.tools.reflect.Metalevel
 */
public class Metaobject implements Serializable {
    protected ClassMetaobject classmetaobject;
    protected Metalevel baseobject;
    protected Method[] methods;

    /**
     * Constructs a <code>Metaobject</code>.  The metaobject is
     * constructed before the constructor is called on the base-level
     * object.
     *
     * @param self      the object that this metaobject is associated with.
     * @param args      the parameters passed to the constructor of
     *                  <code>self</code>.
     */
    public Metaobject(Object self, Object[] args) {
        baseobject = (Metalevel)self;
        classmetaobject = baseobject._getClass();
        methods = classmetaobject.getReflectiveMethods();
    }

    /**
     * Constructs a <code>Metaobject</code> without initialization.
     * If calling this constructor, a subclass should be responsible
     * for initialization.
     */
    protected Metaobject() {
        baseobject = null;
        classmetaobject = null;
        methods = null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(baseobject);
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        baseobject = (Metalevel)in.readObject();
        classmetaobject = baseobject._getClass();
        methods = classmetaobject.getReflectiveMethods();
    }

    /**
     * Obtains the class metaobject associated with this metaobject.
     *
     * @see javassist.tools.reflect.ClassMetaobject
     */
    public final ClassMetaobject getClassMetaobject() {
        return classmetaobject;
    }

    /**
     * Obtains the object controlled by this metaobject.
     */
    public final Object getObject() {
        return baseobject;
    }

    /**
     * Changes the object controlled by this metaobject.
     *
     * @param self      the object
     */
    public final void setObject(Object self) {
        baseobject = (Metalevel)self;
        classmetaobject = baseobject._getClass();
        methods = classmetaobject.getReflectiveMethods();

        // call _setMetaobject() after the metaobject is settled.
        baseobject._setMetaobject(this);
    }

    /**
     * Returns the name of the method specified
     * by <code>identifier</code>.
     */
    public final String getMethodName(int identifier) {
        String mname = methods[identifier].getName();
        int j = ClassMetaobject.methodPrefixLen;
        for (;;) {
            char c = mname.charAt(j++);
            if (c < '0' || '9' < c)
                break;
        }

        return mname.substring(j);
    }

    /**
     * Returns an array of <code>Class</code> objects representing the
     * formal parameter types of the method specified
     * by <code>identifier</code>.
     */
    public final Class[] getParameterTypes(int identifier) {
        return methods[identifier].getParameterTypes();
    }

    /**
     * Returns a <code>Class</code> objects representing the
     * return type of the method specified by <code>identifier</code>.
     */
    public final Class getReturnType(int identifier) {
        return methods[identifier].getReturnType();
    }

    /**
     * Is invoked when public fields of the base-level
     * class are read and the runtime system intercepts it.
     * This method simply returns the value of the field.
     *
     * <p>Every subclass of this class should redefine this method.
     */
    public Object trapFieldRead(String name) {
        Class jc = getClassMetaobject().getJavaClass();
        try {
            return jc.getField(name).get(getObject());
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e.toString());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Is invoked when public fields of the base-level
     * class are modified and the runtime system intercepts it.
     * This method simply sets the field to the given value.
     *
     * <p>Every subclass of this class should redefine this method.
     */
    public void trapFieldWrite(String name, Object value) {
        Class jc = getClassMetaobject().getJavaClass();
        try {
            jc.getField(name).set(getObject(), value);
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e.toString());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Is invoked when base-level method invocation is intercepted.
     * This method simply executes the intercepted method invocation
     * with the original parameters and returns the resulting value.
     *
     * <p>Every subclass of this class should redefine this method.
     *
     * <p>Note: this method is not invoked if the base-level method
     * is invoked by a constructor in the super class.  For example,
     *
     * <ul><pre>abstract class A {
     *   abstract void initialize();
     *   A() {
     *       initialize();    // not intercepted
     *   }
     * }
     *
     * class B extends A {
     *   void initialize() { System.out.println("initialize()"); }
     *   B() {
     *       super();
     *       initialize();    // intercepted
     *   }
     * }</pre></ul>
     *
     * <p>if an instance of B is created,
     * the invocation of initialize() in B is intercepted only once.
     * The first invocation by the constructor in A is not intercepted.
     * This is because the link between a base-level object and a
     * metaobject is not created until the execution of a
     * constructor of the super class finishes.
     */
    public Object trapMethodcall(int identifier, Object[] args) 
        throws Throwable
    {
        try {
            return methods[identifier].invoke(getObject(), args);
        }
        catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getTargetException();
        }
        catch (java.lang.IllegalAccessException e) {
            throw new CannotInvokeException(e);
        }
    }
}
