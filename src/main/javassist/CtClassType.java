/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
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
import javassist.compiler.Javac;
import javassist.compiler.CompileError;
import javassist.expr.ExprEditor;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * Class types.
 */
class CtClassType extends CtClass {
    protected ClassPool classPool;
    protected boolean wasChanged;
    protected boolean wasFrozen;
    protected ClassFile classfile;

    private CtField fieldsCache;
    private CtConstructor constructorsCache;
    private CtConstructor classInitializerCache;
    private CtMethod methodsCache;

    private FieldInitLink fieldInitializers;
    private Hashtable hiddenMethods;    // must be synchronous
    private int uniqueNumberSeed;

    CtClassType(String name, ClassPool cp) {
        super(name);
        classPool = cp;
        wasChanged = wasFrozen = false;
        classfile = null;
        fieldInitializers = null;
        hiddenMethods = null;
        uniqueNumberSeed = 0;
        eraseCache();
    }

    CtClassType(InputStream ins, ClassPool cp) throws IOException {
        this((String)null, cp);
        classfile = new ClassFile(new DataInputStream(ins));
        qualifiedName = classfile.getName();
    }

    protected void extendToString(StringBuffer buffer) {
        if (wasChanged)
            buffer.append(" changed");

        if (wasFrozen)
            buffer.append(" frozen");		

        CtField field = getFieldsCache();
        buffer.append(" fields=");
        while (field != null) {
            buffer.append(field);
            buffer.append(", ");
            field = field.next;
        }

        CtConstructor c = getConstructorsCache();
        buffer.append(" constructors=");
        while (c != null) {
            buffer.append(c);
            buffer.append(", ");
            c = c.next;
        }

        CtMethod m = getMethodsCache();
        buffer.append(" methods=");
        while (m != null) {
            buffer.append(m);
            buffer.append(", ");
            m = m.next;
        }
    }

    protected void eraseCache() {
        fieldsCache = null;
        constructorsCache = null;
        classInitializerCache = null;
        methodsCache = null;
    }

    public ClassFile getClassFile2() {
        if (classfile != null)
            return classfile;

        try {
            byte[] b = classPool.readSource(getName());
            DataInputStream dis
                = new DataInputStream(new ByteArrayInputStream(b));
            return (classfile = new ClassFile(dis));
        }
        catch (NotFoundException e) {
            throw new RuntimeException(e.toString());
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
        catch (CannotCompileException e) {
            throw new RuntimeException(e.toString());
        }
    }

    public ClassPool getClassPool() { return classPool; }

    public boolean isModified() { return wasChanged; }

    public boolean isFrozen() { return wasFrozen; }

    void freeze() { wasFrozen = true; }

    void checkModify() throws RuntimeException {
        super.checkModify();
        wasChanged = true;
    }

    public void defrost() { wasFrozen = false; }

    public boolean subtypeOf(CtClass clazz) throws NotFoundException {
        int i;
        String cname = clazz.getName();
        if (this == clazz || getName().equals(cname))
            return true;

        ClassFile file = getClassFile2();
        String supername = file.getSuperclass();
        if (supername != null && supername.equals(cname))
            return true;

        String[] ifs = file.getInterfaces();
        int num = ifs.length;
        for (i = 0; i < num; ++i)
            if (ifs[i].equals(cname))
                return true;

        if (supername != null && classPool.get(supername).subtypeOf(clazz))
            return true;

        for (i = 0; i < num; ++i)
            if (classPool.get(ifs[i]).subtypeOf(clazz))
                return true;

        return false;
    }

    public void setName(String name) throws RuntimeException {
        String oldname = getName();
        if (name.equals(oldname))
            return;

        classPool.checkNotFrozen(name,
                                 "the class with the new name is frozen");
        ClassFile cf = getClassFile2();
        super.setName(name);
        cf.setName(name);
        eraseCache();
        classPool.classNameChanged(oldname, this);
    }

    public void replaceClassName(ClassMap classnames)
        throws RuntimeException 
    {
        String oldClassName = getName();
        String newClassName
            = (String)classnames.get(Descriptor.toJvmName(oldClassName));
        if (newClassName != null) {
            newClassName = Descriptor.toJavaName(newClassName);
            classPool.checkNotFrozen(newClassName,
                        "the class " + newClassName + " is frozen");
        }

        super.replaceClassName(classnames);
        ClassFile cf = getClassFile2();
        cf.renameClass(classnames);
        eraseCache();

        if (newClassName != null) {
            super.setName(newClassName);
            classPool.classNameChanged(oldClassName, this);
        }
    }

    public void replaceClassName(String oldname, String newname)
        throws RuntimeException 
    {
        String thisname = getName();
        if (thisname.equals(oldname))
            setName(newname);
        else {
            super.replaceClassName(oldname, newname);
            getClassFile2().renameClass(oldname, newname);
            eraseCache();
        }
    }

    public boolean isInterface() {
        return Modifier.isInterface(getModifiers());
    }

    public int getModifiers() {
        int acc = getClassFile2().getAccessFlags();
        acc = AccessFlag.clear(acc, AccessFlag.SUPER);
        return AccessFlag.toModifier(acc);
    }

    public void setModifiers(int mod) {
        checkModify();
        int acc = AccessFlag.of(mod) | AccessFlag.SUPER;
        getClassFile2().setAccessFlags(acc);
    }

    public boolean subclassOf(CtClass superclass) {
        if (superclass == null)
            return false;

        String superName = superclass.getName();
        CtClass curr = this;
        try {
            while (curr != null) {
                if (curr.getName().equals(superName))
                    return true;

                curr = curr.getSuperclass();
            }
        }
        catch (Exception ignored) {}
        return false;
    }

    public CtClass getSuperclass() throws NotFoundException {
        String supername = getClassFile2().getSuperclass();
        if (supername == null)
            return null;
        else
            return classPool.get(supername);
    }

    public void setSuperclass(CtClass clazz) throws CannotCompileException {
        checkModify();
        getClassFile2().setSuperclass(clazz.getName());
    }

    public CtClass[] getInterfaces() throws NotFoundException {
        String[] ifs = getClassFile2().getInterfaces();
        int num = ifs.length;
        CtClass[] ifc = new CtClass[num];
        for (int i = 0; i < num; ++i)
            ifc[i] = classPool.get(ifs[i]);

        return ifc;
    }

    public void setInterfaces(CtClass[] list) {
        checkModify();
        String[] ifs;
        if (list == null)
            ifs = new String[0];
        else {
            int num = list.length;
            ifs = new String[num];
            for (int i = 0; i < num; ++i)
                ifs[i] = list[i].getName();
        }

        getClassFile2().setInterfaces(ifs);
    }

    public void addInterface(CtClass anInterface) {
        checkModify();
        if (anInterface != null)
            getClassFile2().addInterface(anInterface.getName());
    }

    public CtField[] getFields() {
        ArrayList alist = new ArrayList();
        getFields(alist, this);
        return (CtField[])alist.toArray(new CtField[alist.size()]);
    }

    private static void getFields(ArrayList alist, CtClass cc) {
        int i, num;
        if (cc == null)
            return;

        try {
            getFields(alist, cc.getSuperclass());
        }
        catch (NotFoundException e) {}

        try {
            CtClass[] ifs = cc.getInterfaces();
            num = ifs.length;
            for (i = 0; i < num; ++i)
                getFields(alist, ifs[i]);
        }
        catch (NotFoundException e) {}

        CtField cf = ((CtClassType)cc).getFieldsCache();
        while (cf != null) {
            if (Modifier.isPublic(cf.getModifiers()))
                alist.add(cf);

            cf = cf.next;
        }
    }

    public CtField getField(String name) throws NotFoundException {
        try {
            return getDeclaredField(name);
        }
        catch (NotFoundException e) {}

        try {
            CtClass[] ifs = getInterfaces();
            int num = ifs.length;
            for (int i = 0; i < num; ++i)
                try {
                    return ifs[i].getField(name);
                }
                catch (NotFoundException e) {}
        }
        catch (NotFoundException e) {}

        try {
            CtClass s = getSuperclass();
            if (s != null)
                return s.getField(name);
        }
        catch (NotFoundException e) {}

        throw new NotFoundException(name);
    }

    public CtField[] getDeclaredFields() {
        CtField cf = getFieldsCache();
        int num = CtField.count(cf);
        CtField[] cfs = new CtField[num];
        int i = 0;
        while (cf != null) {
            cfs[i++] = cf;
            cf = cf.next;
        }

        return cfs;
    }

    protected CtField getFieldsCache() {
        if (fieldsCache == null) {
            List list = getClassFile2().getFields();
            int n = list.size();
            for (int i = 0; i < n; ++i) {
                FieldInfo finfo = (FieldInfo)list.get(i);
                fieldsCache = CtField.append(fieldsCache,
                                             new CtField(finfo, this));
            }
        }

        return fieldsCache;
    }

    public CtField getDeclaredField(String name) throws NotFoundException {
        CtField cf = getFieldsCache();
        while (cf != null) {
            if (cf.getName().equals(name))
                return cf;

            cf = cf.next;
        }

        throw new NotFoundException(name);
    }

    public CtBehavior[] getDeclaredBehaviors() {
        CtConstructor cc = getConstructorsCache();
        CtMethod cm = getMethodsCache();
        int num = CtMethod.count(cm) + CtConstructor.count(cc);
        CtBehavior[] cb = new CtBehavior[num];
        int i = 0;
        while (cc != null) {
            cb[i++] = cc;
            cc = cc.next;
        }

        while (cm != null) {
            cb[i++] = cm;
            cm = cm.next;
        }
        
        return cb;
    }

    public CtConstructor[] getConstructors() {
        CtConstructor[] cons = getDeclaredConstructors();
        if (cons.length == 0)
            return cons;

        int n = 0;
        int i = cons.length;
        while (--i >= 0)
            if (Modifier.isPublic(cons[i].getModifiers()))
                ++n;

        CtConstructor[] result = new CtConstructor[n];
        n = 0;
        i = cons.length;
        while (--i >= 0) {
            CtConstructor c = cons[i];
            if (Modifier.isPublic(c.getModifiers()))
                result[n++] = c;
        }

        return result;
    }

    public CtConstructor getConstructor(String desc)
        throws NotFoundException
    {
        CtConstructor cc = getConstructorsCache();
        while (cc != null) {
            if (cc.getMethodInfo2().getDescriptor().equals(desc))
                return cc;

            cc = cc.next;
        }

        return super.getConstructor(desc);
    }

    public CtConstructor[] getDeclaredConstructors() {
        CtConstructor cc = getConstructorsCache();
        int num = CtConstructor.count(cc);
        CtConstructor[] ccs = new CtConstructor[num];
        int i = 0;
        while (cc != null) {
            ccs[i++] = cc;
            cc = cc.next;
        }
        
        return ccs;
    }

    protected CtConstructor getConstructorsCache() {
        if (constructorsCache == null) {
            List list = getClassFile2().getMethods();
            int n = list.size();
            for (int i = 0; i < n; ++i) {
                MethodInfo minfo = (MethodInfo)list.get(i);
                if (minfo.isConstructor())
                    constructorsCache
                        = CtConstructor.append(constructorsCache,
                                            new CtConstructor(minfo, this));
            }
        }

        return constructorsCache;
    }

    public CtConstructor getClassInitializer() {
        if (classInitializerCache == null) {
            MethodInfo minfo = getClassFile2().getStaticInitializer();
            if (minfo != null)
                classInitializerCache = new CtConstructor(minfo, this);
        }

        return classInitializerCache;
    }

    public CtMethod[] getMethods() {
        HashMap h = new HashMap();
        getMethods0(h, this);
        return (CtMethod[])h.values().toArray(new CtMethod[0]);
    }

    private static void getMethods0(HashMap h, CtClass cc) {
        try {
            CtClass[] ifs = cc.getInterfaces();
            int size = ifs.length;
            for (int i = 0; i < size; ++i)
                getMethods0(h, ifs[i]);
        }
        catch (NotFoundException e) {}

        try {
            CtClass s = cc.getSuperclass();
            if (s != null)
                getMethods0(h, s);
        }
        catch (NotFoundException e) {}

        if (cc instanceof CtClassType) {
            CtMethod cm = ((CtClassType)cc).getMethodsCache();
            while (cm != null) {
                if (Modifier.isPublic(cm.getModifiers()))
                    h.put(cm, cm);

                cm = cm.next;
            }
        }
    }

    public CtMethod getMethod(String name, String desc)
        throws NotFoundException
    {
        CtMethod m = getMethod0(this, name, desc);
        if (m != null)
            return m;
        else
            throw new NotFoundException(name + "(..) is not found in "
                                        + getName());
    }

    private static CtMethod getMethod0(CtClass cc,
                                       String name, String desc) {
        if (cc instanceof CtClassType) {
            CtMethod cm = ((CtClassType)cc).getMethodsCache();
            while (cm != null) {
                if (cm.getName().equals(name)
                    && cm.getMethodInfo2().getDescriptor().equals(desc))
                    return cm;

                cm = cm.next;
            }
        }

        try {
            CtClass s = cc.getSuperclass();
            if (s != null) {
                CtMethod m = getMethod0(s, name, desc);
                if (m != null)
                    return m;
            }
        }
        catch (NotFoundException e) {}

        try {
            CtClass[] ifs = cc.getInterfaces();
            int size = ifs.length;
            for (int i = 0; i < size; ++i) {
                CtMethod m = getMethod0(ifs[i], name, desc);
                if (m != null)
                    return m;
            }
        }
        catch (NotFoundException e) {}
        return null;
    }

    public CtMethod[] getDeclaredMethods() {
        CtMethod cm = getMethodsCache();
        int num = CtMethod.count(cm);
        CtMethod[] cms = new CtMethod[num];
        int i = 0;
        while (cm != null) {
            cms[i++] = cm;
            cm = cm.next;
        }
        
        return cms;
    }

    public CtMethod getDeclaredMethod(String name) throws NotFoundException {
        CtMethod m = getMethodsCache();
        while (m != null) {
            if (m.getName().equals(name))
                return m;

            m = m.next;
        }

        throw new NotFoundException(name + "(..) is not found in "
                                    + getName());
    }

    public CtMethod getDeclaredMethod(String name, CtClass[] params)
        throws NotFoundException
    {
        String desc = Descriptor.ofParameters(params);
        CtMethod m = getMethodsCache();
        while (m != null) {
            if (m.getName().equals(name)
                && m.getMethodInfo2().getDescriptor().startsWith(desc))
                return m;

            m = m.next;
        }

        throw new NotFoundException(name + "(..) is not found in "
                                    + getName());
    }

    protected CtMethod getMethodsCache() {
        if (methodsCache == null) {
            List list = getClassFile2().getMethods();
            int n = list.size();
            for (int i = 0; i < n; ++i) {
                MethodInfo minfo = (MethodInfo)list.get(i);
                if (minfo.isMethod())
                    methodsCache = CtMethod.append(methodsCache,
                                                new CtMethod(minfo, this));
            }
        }

        return methodsCache;
    }

    public void addField(CtField f, String init)
        throws CannotCompileException
    {
        addField(f, CtField.Initializer.byExpr(init));
    }

    public void addField(CtField f, CtField.Initializer init)
        throws CannotCompileException
    {
        checkModify();
        if (f.getDeclaringClass() != this)
            throw new CannotCompileException("cannot add");

        if (init == null)
            init = f.getInit();

        getFieldsCache();
        fieldsCache = CtField.append(fieldsCache, f);
        getClassFile2().addField(f.getFieldInfo2());

        if (init != null) {
            FieldInitLink fil = new FieldInitLink(f, init);
            FieldInitLink link = fieldInitializers;
            if (link == null)
                fieldInitializers = fil;
            else {
                while (link.next != null)
                    link = link.next;

                link.next = fil;
            }
        }
    }

    public CtConstructor makeClassInitializer()
        throws CannotCompileException
    {
        CtConstructor clinit = getClassInitializer();
        if (clinit != null)
            return clinit;

        checkModify();
        ClassFile cf = getClassFile2();
        Bytecode code = new Bytecode(cf.getConstPool(), 0, 0);
        modifyClassConstructor(cf, code, 0, 0);
        return getClassInitializer();
    }

    public void addConstructor(CtConstructor c)
        throws CannotCompileException
    {
        checkModify();
        if (c.getDeclaringClass() != this)
            throw new CannotCompileException("cannot add");

        getConstructorsCache();
        constructorsCache = CtConstructor.append(constructorsCache, c);
        getClassFile2().addMethod(c.getMethodInfo2());
    }

    public void addMethod(CtMethod m) throws CannotCompileException {
        checkModify();
        if (m.getDeclaringClass() != this)
            throw new CannotCompileException("cannot add");

        getMethodsCache();
        methodsCache = CtMethod.append(methodsCache, m);
        getClassFile2().addMethod(m.getMethodInfo2());
        if ((m.getModifiers() & Modifier.ABSTRACT) != 0)
            setModifiers(getModifiers() | Modifier.ABSTRACT);
    }

    public byte[] getAttribute(String name) {
        AttributeInfo ai = getClassFile2().getAttribute(name);
        if (ai == null)
            return null;
        else
            return ai.get();
    }

    public void setAttribute(String name, byte[] data) {
        checkModify();
        ClassFile cf = getClassFile2();
        cf.addAttribute(new AttributeInfo(cf.getConstPool(), name, data));
    }

    public void instrument(CodeConverter converter)
        throws CannotCompileException
    {
        checkModify();
        ClassFile cf = getClassFile2();
        ConstPool cp = cf.getConstPool();
        List list = cf.getMethods();
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            converter.doit(this, minfo, cp);
        }
    }

    public void instrument(ExprEditor editor)
        throws CannotCompileException
    {
        checkModify();
        ClassFile cf = getClassFile2();
        List list = cf.getMethods();
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            editor.doit(this, minfo);
        }
    }

    void toBytecode(DataOutputStream out)
        throws CannotCompileException, IOException
    {
        ClassFile cf = getClassFile2();
        try {
            modifyClassConstructor(cf);
            modifyConstructors(cf);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }

        wasFrozen = true;
        try {
            cf.write(out);
            out.flush();
        }
        catch (IOException e) {
            throw new CannotCompileException(e);
        }
    }

    protected void modifyClassConstructor(ClassFile cf)
        throws CannotCompileException, NotFoundException
    {
        Bytecode code = new Bytecode(cf.getConstPool(), 0, 0);
        Javac jv = new Javac(code, this);
        int stacksize = 0;
        boolean doInit = false;
        for (FieldInitLink fi = fieldInitializers; fi != null; fi = fi.next) {
            CtField f = fi.field;
            if (Modifier.isStatic(f.getModifiers())) {
                doInit = true;
                int s = fi.init.compileIfStatic(f.getType(), f.getName(),
                                                code, jv);
                if (stacksize < s)
                    stacksize = s;
            }
        }

        if (doInit)    // need an initializer for static fileds.
            modifyClassConstructor(cf, code, stacksize, 0);
    }

    private void modifyClassConstructor(ClassFile cf, Bytecode code,
                                        int stacksize, int localsize)
        throws CannotCompileException
    {
        MethodInfo m = cf.getStaticInitializer();
        if (m == null) {
            code.add(Bytecode.RETURN);
            code.setMaxStack(stacksize);
            code.setMaxLocals(localsize);
            m = new MethodInfo(cf.getConstPool(), "<clinit>", "()V");
            m.setAccessFlags(AccessFlag.STATIC);
            m.setCodeAttribute(code.toCodeAttribute());
            cf.addMethod(m);
        }
        else {
            CodeAttribute codeAttr = m.getCodeAttribute();
            if (codeAttr == null)
                throw new CannotCompileException("empty <clinit>");

            try {
                CodeIterator it = codeAttr.iterator();
                int pos = it.insertEx(code.get());
                it.insert(code.getExceptionTable(), pos);
                int maxstack = codeAttr.getMaxStack();
                if (maxstack < stacksize)
                    codeAttr.setMaxStack(stacksize);

                int maxlocals = codeAttr.getMaxLocals();
                if (maxlocals < localsize)
                    codeAttr.setMaxLocals(localsize);
            }
            catch (BadBytecode e) {
                throw new CannotCompileException(e);
            }
        }
    }

    protected void modifyConstructors(ClassFile cf)
        throws CannotCompileException, NotFoundException
    {
        if (fieldInitializers == null)
            return;

        ConstPool cp = cf.getConstPool();
        List list = cf.getMethods();
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            if (minfo.isConstructor()) {
                CodeAttribute codeAttr = minfo.getCodeAttribute();
                if (codeAttr != null)
                    try {
                        Bytecode init = new Bytecode(cp, 0,
                                             codeAttr.getMaxLocals());
                        CtClass[] params
                            = Descriptor.getParameterTypes(
                                                minfo.getDescriptor(),
                                                classPool);
                        int stacksize = makeFieldInitializer(init, params);
                        insertAuxInitializer(codeAttr, init, stacksize);
                    }
                    catch (BadBytecode e) {
                        throw new CannotCompileException(e);
                    }
            }
        }
    }

    private static void insertAuxInitializer(CodeAttribute codeAttr,
                                             Bytecode initializer,
                                             int stacksize)
        throws BadBytecode
    {
        CodeIterator it = codeAttr.iterator();
        int index = it.skipSuperConstructor();
        if (index < 0) {
            index = it.skipThisConstructor();
            if (index >= 0)
                return;         // this() is called.

            // Neither this() or super() is called.
        }

        int pos = it.insertEx(initializer.get());
        it.insert(initializer.getExceptionTable(), pos);
        int maxstack = codeAttr.getMaxStack();
        if (maxstack < stacksize)
            codeAttr.setMaxStack(stacksize);
    }

    private int makeFieldInitializer(Bytecode code, CtClass[] parameters)
        throws CannotCompileException, NotFoundException
    {
        int stacksize = 0;
        Javac jv = new Javac(code, this);
        try {
            jv.recordParams(parameters, false);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }

        for (FieldInitLink fi = fieldInitializers; fi !=  null; fi = fi.next) {
            CtField f = fi.field;
            if (!Modifier.isStatic(f.getModifiers())) {
                int s = fi.init.compile(f.getType(), f.getName(), code,
                                        parameters, jv);
                if (stacksize < s)
                    stacksize = s;
            }
        }

        return stacksize;
    }

    // Methods used by CtNewWrappedMethod

    Hashtable getHiddenMethods() {
        if (hiddenMethods == null)
            hiddenMethods = new Hashtable();

        return hiddenMethods;
    }

    int getUniqueNumber() { return uniqueNumberSeed++; }
}

class FieldInitLink {
    FieldInitLink next;
    CtField field;
    CtField.Initializer init;

    FieldInitLink(CtField f, CtField.Initializer i) {
        next = null;
        field = f;
        init = i;
    }
}
