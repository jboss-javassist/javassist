/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2005 Shigeru Chiba. All Rights Reserved.
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstantAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.EnclosingMethodAttribute;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.InnerClassesAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.compiler.AccessorMaker;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import javassist.expr.ExprEditor;

/**
 * Class types.
 */
class CtClassType extends CtClass {
    ClassPool classPool;
    boolean wasChanged;
    private boolean wasFrozen;
    boolean wasPruned;
    boolean memberRemoved;
    ClassFile classfile;

    private CtMember fieldsCache;
    private CtMember methodsCache;
    private CtMember constructorsCache;
    private CtConstructor classInitializerCache;

    private AccessorMaker accessors;

    private FieldInitLink fieldInitializers;
    private Hashtable hiddenMethods;    // must be synchronous
    private int uniqueNumberSeed;

    private boolean doPruning = ClassPool.doPruning;
    int getCounter;
    private static int readCounter = 0;
    private static final int READ_THRESHOLD = 100;  // see getClassFile2()

    CtClassType(String name, ClassPool cp) {
        super(name);
        classPool = cp;
        wasChanged = wasFrozen = wasPruned = memberRemoved = false;
        classfile = null;
        accessors = null;
        fieldInitializers = null;
        hiddenMethods = null;
        uniqueNumberSeed = 0;
        eraseCache();
        getCounter = 0;
    }

    CtClassType(InputStream ins, ClassPool cp) throws IOException {
        this((String)null, cp);
        classfile = new ClassFile(new DataInputStream(ins));
        qualifiedName = classfile.getName();
    }

    protected void extendToString(StringBuffer buffer) {
        if (wasChanged)
            buffer.append("changed ");

        if (wasFrozen)
            buffer.append("frozen ");

        if (wasPruned)
            buffer.append("pruned ");

        buffer.append(Modifier.toString(getModifiers()));
        buffer.append(" class ");
        buffer.append(getName());

        try {
            CtClass ext = getSuperclass();
            if (ext != null) {
                String name = ext.getName();
                if (!name.equals("java.lang.Object"))
                    buffer.append(" extends " + ext.getName());
            }
        }
        catch (NotFoundException e) {
            buffer.append(" extends ??");
        }

        try {
            CtClass[] intf = getInterfaces();
            if (intf.length > 0)
                buffer.append(" implements ");

            for (int i = 0; i < intf.length; ++i) {
                buffer.append(intf[i].getName());
                buffer.append(", ");
            }
        }
        catch (NotFoundException e) {
            buffer.append(" extends ??");
        }

        CtMember field = getFieldsCache();
        buffer.append(" fields=");
        while (field != null) {
            buffer.append(field);
            buffer.append(", ");
            field = field.next;
        }

        CtMember c = getConstructorsCache();
        buffer.append(" constructors=");
        while (c != null) {
            buffer.append(c);
            buffer.append(", ");
            c = c.next;
        }

        CtMember m = getMethodsCache();
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

    public AccessorMaker getAccessorMaker() {
        if (accessors == null)
            accessors = new AccessorMaker(this);

        return accessors;
    }

    public ClassFile getClassFile2() {
        if (classfile != null)
            return classfile;

        if (readCounter++ > READ_THRESHOLD
                                    && ClassPool.releaseUnmodifiedClassFile) {
            releaseClassFiles();
            readCounter = 0;
        }

        InputStream fin = null;
        try {
            fin = classPool.openClassfile(getName());
            if (fin == null)
                throw new NotFoundException(getName());

            fin = new BufferedInputStream(fin);
            classfile = new ClassFile(new DataInputStream(fin));
            if (!classfile.getName().equals(qualifiedName))
                throw new RuntimeException(classfile.getName() + " in "
                                + qualifiedName.replace('.', '/') + ".java");

            return classfile;
        }
        catch (NotFoundException e) {
            throw new RuntimeException(e.toString());
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
        finally {
            if (fin != null)
                try {
                    fin.close();
                }
                catch (IOException e) {}
        }
    }

    /* Inherited from CtClass.  Called by get() in ClassPool.
     *
     * @see javassist.CtClass#incGetCounter()
     */
    void incGetCounter() { ++getCounter; }

    /**
     * Releases the class files and cached CtBehaviors
     * of the CtClasses that have not been recently used
     * if they are unmodified. 
     */
    private void releaseClassFiles() {
        Enumeration e = classPool.classes.elements();
        while (e.hasMoreElements()) {
            Object obj = e.nextElement();
            if (obj instanceof CtClassType) {
                CtClassType cct = (CtClassType)obj;
                if (cct.getCounter < 2 && !cct.isModified()) {
                    cct.eraseCache();
                    cct.classfile = null;
                }

                cct.getCounter = 0;
            }
        }
    }

    public ClassPool getClassPool() { return classPool; }

    void setClassPool(ClassPool cp) { classPool = cp; }

    public URL getURL() throws NotFoundException {
        URL url = classPool.find(getName());
        if (url == null)
            throw new NotFoundException(getName());
        else
            return url;
    }

    public boolean isModified() { return wasChanged; }

    public boolean isFrozen() { return wasFrozen; }

    void freeze() { wasFrozen = true; }

    void checkModify() throws RuntimeException {
        if (isFrozen()) {
            String msg = getName() + " class is frozen";
            if (wasPruned)
                msg += " and pruned";

            throw new RuntimeException(msg);
        }

        wasChanged = true;
    }

    public void defrost() {
        checkPruned("defrost");
        wasFrozen = false;
    }

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

        // check this in advance although classNameChanged() below does.
        classPool.checkNotFrozen(name);
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
            // check this in advance although classNameChanged() below does.
            classPool.checkNotFrozen(newClassName);
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

    public Object[] getAnnotations() throws ClassNotFoundException {
        ClassFile cf = getClassFile2();
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
                    cf.getAttribute(AnnotationsAttribute.invisibleTag);  
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                    cf.getAttribute(AnnotationsAttribute.visibleTag);  
        return toAnnotationType(getClassPool(), ainfo, ainfo2);
    }

    static Object[] toAnnotationType(ClassPool cp, AnnotationsAttribute a1,
                                     AnnotationsAttribute a2) throws ClassNotFoundException {
        Annotation[] anno1, anno2;
        int size1, size2;

        if (a1 == null) {
            anno1 = null;
            size1 = 0;
        }
        else {
            anno1 = a1.getAnnotations();
            size1 = anno1.length;
        }

        if (a2 == null) {
            anno2 = null;
            size2 = 0;
        }
        else {
            anno2 = a2.getAnnotations();
            size2 = anno2.length;
        }

        Object[] result = new Object[size1 + size2];
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (int i = 0; i < size1; i++)
            result[i] = anno1[i].toAnnotationType(cl, cp);

        for (int j = 0; j < size2; j++)
            result[j + size1] = anno2[j].toAnnotationType(cl, cp);

        return result;
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
        if (isInterface())
            addInterface(clazz);
        else
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

    public CtClass getDeclaringClass() throws NotFoundException {
        ClassFile cf = getClassFile2();
        InnerClassesAttribute ica = (InnerClassesAttribute)cf.getAttribute(
                                                InnerClassesAttribute.tag);
        if (ica == null)
            return null;

        String name = getName();
        int n = ica.tableLength();
        for (int i = 0; i < n; ++i)
            if (name.equals(ica.innerClass(i))) {
                String outName = ica.outerClass(i);
                if (outName != null)
                    return classPool.get(outName);
                else {
                    // maybe anonymous or local class.
                    EnclosingMethodAttribute ema
                        = (EnclosingMethodAttribute)cf.getAttribute(
                                                    EnclosingMethodAttribute.tag);
                    if (ema != null)
                        return classPool.get(ema.className());
                }
            }

        return null;
    }

    public CtMethod getEnclosingMethod() throws NotFoundException {
        ClassFile cf = getClassFile2();
        EnclosingMethodAttribute ema
                = (EnclosingMethodAttribute)cf.getAttribute(
                                                EnclosingMethodAttribute.tag);
        if (ema != null) {
            CtClass enc = classPool.get(ema.className());
            return enc.getMethod(ema.methodName(), ema.methodDescriptor());
        }

        return null;
    }

    public CtClass makeNestedClass(String name, boolean isStatic) {
        if (!isStatic)
            throw new RuntimeException(
                        "sorry, only nested static class is supported");

        checkModify();
        CtClass c = classPool.makeNestedClass(getName() + "$" + name);
        ClassFile cf = getClassFile2();
        ClassFile cf2 = c.getClassFile2();
        InnerClassesAttribute ica = (InnerClassesAttribute)cf.getAttribute(
                                                InnerClassesAttribute.tag);
        if (ica == null) {
            ica = new InnerClassesAttribute(cf.getConstPool());
            cf.addAttribute(ica);
        }

        ica.append(c.getName(), this.getName(), name,
                   (cf2.getAccessFlags() & ~AccessFlag.SUPER) | AccessFlag.STATIC);
        cf2.addAttribute(ica.copy(cf2.getConstPool(), null));
        return c;
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

        CtMember cf = ((CtClassType)cc).getFieldsCache();
        while (cf != null) {
            if (Modifier.isPublic(cf.getModifiers()))
                alist.add(cf);

            cf = cf.next;
        }
    }

    public CtField getField(String name) throws NotFoundException {
        CtField f = getField2(name);
        if (f == null)
            throw new NotFoundException("field: " + name + " in " + getName());
        else
            return f;
    }

    CtField getField2(String name) {
        CtField df = getDeclaredField2(name);
        if (df != null)
            return df;

        try {
            CtClass[] ifs = getInterfaces();
            int num = ifs.length;
            for (int i = 0; i < num; ++i) {
                CtField f = ifs[i].getField2(name);
                if (f != null)
                    return f;
            }

            CtClass s = getSuperclass();
            if (s != null)
                return s.getField2(name);
        }
        catch (NotFoundException e) {}
        return null;
    }

    public CtField[] getDeclaredFields() {
        CtMember cf = getFieldsCache();
        int num = CtField.count(cf);
        CtField[] cfs = new CtField[num];
        int i = 0;
        while (cf != null) {
            cfs[i++] = (CtField)cf;
            cf = cf.next;
        }

        return cfs;
    }

    protected CtMember getFieldsCache() {
        if (fieldsCache == null) {
            List list = getClassFile2().getFields();
            int n = list.size();
            CtMember allFields = null;
            CtField tail = null;
            for (int i = 0; i < n; ++i) {
                FieldInfo finfo = (FieldInfo)list.get(i);
                CtField newTail = new CtField(finfo, this);
                allFields = CtMember.append(allFields, tail, newTail);
                tail = newTail;
            }

            fieldsCache = allFields;
        }

        return fieldsCache;
    }

    public CtField getDeclaredField(String name) throws NotFoundException {
        CtField f = getDeclaredField2(name);
        if (f == null)
            throw new NotFoundException("field: " + name + " in " + getName());
        else
            return f;
    }

    private CtField getDeclaredField2(String name) {
        CtMember cf = getFieldsCache();
        while (cf != null) {
            if (cf.getName().equals(name))
                return (CtField)cf;

            cf = cf.next;
        }

        return null;
    }

    public CtBehavior[] getDeclaredBehaviors() {
        CtMember cc = getConstructorsCache();
        CtMember cm = getMethodsCache();
        int num = CtMember.count(cm) + CtMember.count(cc);
        CtBehavior[] cb = new CtBehavior[num];
        int i = 0;
        while (cc != null) {
            cb[i++] = (CtBehavior)cc;
            cc = cc.next;
        }

        while (cm != null) {
            cb[i++] = (CtBehavior)cm;
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
        CtConstructor cc = (CtConstructor)getConstructorsCache();
        while (cc != null) {
            if (cc.getMethodInfo2().getDescriptor().equals(desc))
                return cc;

            cc = (CtConstructor)cc.next;
        }

        return super.getConstructor(desc);
    }

    public CtConstructor[] getDeclaredConstructors() {
        CtMember cc = getConstructorsCache();
        int num = CtMember.count(cc);
        CtConstructor[] ccs = new CtConstructor[num];
        int i = 0;
        while (cc != null) {
            ccs[i++] = (CtConstructor)cc;
            cc = cc.next;
        }

        return ccs;
    }

    protected CtMember getConstructorsCache() {
        if (constructorsCache == null) {
            List list = getClassFile2().getMethods();
            int n = list.size();
            CtMember allConstructors = null;
            CtConstructor tail = null;
            for (int i = 0; i < n; ++i) {
                MethodInfo minfo = (MethodInfo)list.get(i);
                if (minfo.isConstructor()) {
                    CtConstructor newTail = new CtConstructor(minfo, this);
                    allConstructors = CtMember.append(allConstructors, tail, newTail);
                    tail = newTail;
                }
            }

            constructorsCache = allConstructors;
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
            CtMember cm = ((CtClassType)cc).getMethodsCache();
            while (cm != null) {
                if (Modifier.isPublic(cm.getModifiers()))
                    h.put(((CtMethod)cm).getStringRep(), cm);

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
            CtMethod cm = (CtMethod)((CtClassType)cc).getMethodsCache();
            while (cm != null) {
                if (cm.getName().equals(name)
                    && cm.getMethodInfo2().getDescriptor().equals(desc))
                    return cm;

                cm = (CtMethod)cm.next;
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
        CtMember cm = getMethodsCache();
        int num = CtMember.count(cm);
        CtMethod[] cms = new CtMethod[num];
        int i = 0;
        while (cm != null) {
            cms[i++] = (CtMethod)cm;
            cm = cm.next;
        }

        return cms;
    }

    public CtMethod getDeclaredMethod(String name) throws NotFoundException {
        CtMember m = getMethodsCache();
        while (m != null) {
            if (m.getName().equals(name))
                return (CtMethod)m;

            m = m.next;
        }

        throw new NotFoundException(name + "(..) is not found in "
                                    + getName());
    }

    public CtMethod getDeclaredMethod(String name, CtClass[] params)
        throws NotFoundException
    {
        String desc = Descriptor.ofParameters(params);
        CtMethod m = (CtMethod)getMethodsCache();
        while (m != null) {
            if (m.getName().equals(name)
                && m.getMethodInfo2().getDescriptor().startsWith(desc))
                return m;

            m = (CtMethod)m.next;
        }

        throw new NotFoundException(name + "(..) is not found in "
                                    + getName());
    }

    protected CtMember getMethodsCache() {
        if (methodsCache == null) {
            List list = getClassFile2().getMethods();
            int n = list.size();
            CtMember allMethods = null;
            CtMethod tail = null;
            for (int i = 0; i < n; ++i) {
                MethodInfo minfo = (MethodInfo)list.get(i);
                if (minfo.isMethod()) {
                    CtMethod newTail = new CtMethod(minfo, this);
                    allMethods = CtMember.append(allMethods, tail, newTail);
                    tail = newTail;
                }
            }

            methodsCache = allMethods;
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

        if (init != null) {
            int mod = f.getModifiers();
            if (Modifier.isStatic(mod) && Modifier.isFinal(mod))
                try {
                    ConstPool cp = getClassFile2().getConstPool();
                    int index = init.getConstantValue(cp, f.getType());
                    if (index != 0) {
                        f.getFieldInfo2().addAttribute(new ConstantAttribute(cp, index));
                        init = null;
                    }
                }
                catch (NotFoundException e) {}
        }

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

    public void removeField(CtField f) throws NotFoundException {
        checkModify();
        FieldInfo fi = f.getFieldInfo2();
        ClassFile cf = getClassFile2();
        if (cf.getFields().remove(fi)) {
            fieldsCache = CtMember.remove(fieldsCache, f);
            memberRemoved = true;
        }
        else
            throw new NotFoundException(f.toString());
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
        constructorsCache = (CtConstructor)CtMember.append(constructorsCache, c);
        getClassFile2().addMethod(c.getMethodInfo2());
    }

    public void removeConstructor(CtConstructor m) throws NotFoundException {
        checkModify();
        MethodInfo mi = m.getMethodInfo2();
        ClassFile cf = getClassFile2();
        if (cf.getMethods().remove(mi)) {
            constructorsCache = CtMember.remove(constructorsCache, m);
            memberRemoved = true;
        }
        else
            throw new NotFoundException(m.toString());
    }

    public void addMethod(CtMethod m) throws CannotCompileException {
        checkModify();
        if (m.getDeclaringClass() != this)
            throw new CannotCompileException("cannot add");

        getMethodsCache();
        methodsCache = CtMember.append(methodsCache, m);
        getClassFile2().addMethod(m.getMethodInfo2());
        if ((m.getModifiers() & Modifier.ABSTRACT) != 0)
            setModifiers(getModifiers() | Modifier.ABSTRACT);
    }

    public void removeMethod(CtMethod m) throws NotFoundException {
        checkModify();
        MethodInfo mi = m.getMethodInfo2();
        ClassFile cf = getClassFile2();
        if (cf.getMethods().remove(mi)) {
            methodsCache = CtMember.remove(methodsCache, m);
            memberRemoved = true;
        }
        else
            throw new NotFoundException(m.toString());
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

    /**
     * @see javassist.CtClass#prune()
     * @see javassist.CtClass#stopPruning(boolean)
     */
    public void prune() {
        if (wasPruned)
            return;

        wasPruned = wasFrozen = true;
        getClassFile2().prune();
    }

    public void toBytecode(DataOutputStream out)
        throws CannotCompileException, IOException
    {
        try {
            if (isModified()) {
                checkPruned("toBytecode");
                ClassFile cf = getClassFile2();
                if (memberRemoved) {
                    cf.compact();
                    memberRemoved = false;
                }

                modifyClassConstructor(cf);
                modifyConstructors(cf);
                cf.write(out);
                out.flush();
                fieldInitializers = null;
                if (doPruning) {
                    // to save memory
                    cf.prune();
                    wasPruned = true;
                }
            }
            else {
                classPool.writeClassfile(getName(), out);
                // to save memory
                eraseCache();
                classfile = null;
            }

            wasFrozen = true;
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (IOException e) {
            throw new CannotCompileException(e);
        }
    }

    /* See also checkModified()
     */
    private void checkPruned(String method) {
        if (wasPruned)
            throw new RuntimeException(method + "(): " + getName()
                                       + " was pruned.");
    }

    public void stopPruning(boolean stop) {
        doPruning = !stop;
    }

    private void modifyClassConstructor(ClassFile cf)
        throws CannotCompileException, NotFoundException
    {
        if (fieldInitializers == null)
            return;

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

    private void modifyConstructors(ClassFile cf)
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

        for (FieldInitLink fi = fieldInitializers; fi != null; fi = fi.next) {
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

    public String makeUniqueName(String prefix) {
        HashMap table = new HashMap();
        makeMemberList(table);
        Set keys = table.keySet();
        String[] methods = new String[keys.size()];
        keys.toArray(methods);

        if (notFindInArray(prefix, methods))
            return prefix;

        int i = 100;
        String name;
        do {
            if (i > 999)
                throw new RuntimeException("too many unique name");

            name = prefix + i++;
        } while (!notFindInArray(name, methods));
        return name;
    }

    private static boolean notFindInArray(String prefix, String[] values) {
        int len = values.length;
        for (int i = 0; i < len; i++)
            if (values[i].startsWith(prefix))
                return false;

        return true;
    }

    private void makeMemberList(HashMap table) {
        int mod = getModifiers();
        if (Modifier.isAbstract(mod) || Modifier.isInterface(mod))
            try {
                CtClass[] ifs = getInterfaces();
                int size = ifs.length;
                for (int i = 0; i < size; i++) {
                    CtClass ic =ifs[i];
                    if (ic != null && ic instanceof CtClassType)
                        ((CtClassType)ic).makeMemberList(table);
                }
            }
            catch (NotFoundException e) {}

        try {
            CtClass s = getSuperclass();
            if (s != null && s instanceof CtClassType)
                ((CtClassType)s).makeMemberList(table);
        }
        catch (NotFoundException e) {}

        List list = getClassFile2().getMethods();
        int n = list.size();
        for (int i = 0; i < n; i++) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            table.put(minfo.getName(), this);
        }

        list = getClassFile2().getFields();
        n = list.size();
        for (int i = 0; i < n; i++) {
            FieldInfo finfo = (FieldInfo)list.get(i);
            table.put(finfo.getName(), this);
        }
    }
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
