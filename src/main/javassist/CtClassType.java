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

package javassist;

import java.lang.ref.WeakReference;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
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
import javassist.bytecode.ParameterAnnotationsAttribute;
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
    boolean gcConstPool;    // if true, the constant pool entries will be garbage collected. 
    ClassFile classfile;
    byte[] rawClassfile;    // backup storage

    private WeakReference memberCache;
    private AccessorMaker accessors;

    private FieldInitLink fieldInitializers;
    private Hashtable hiddenMethods;    // must be synchronous
    private int uniqueNumberSeed;

    private boolean doPruning = ClassPool.doPruning;
    private int getCount;
    private static final int GET_THRESHOLD = 2;     // see compress()

    CtClassType(String name, ClassPool cp) {
        super(name);
        classPool = cp;
        wasChanged = wasFrozen = wasPruned = gcConstPool = false;
        classfile = null;
        rawClassfile = null;
        memberCache = null;
        accessors = null;
        fieldInitializers = null;
        hiddenMethods = null;
        uniqueNumberSeed = 0;
        getCount = 0;
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

        CtMember.Cache memCache = getMembers();
        exToString(buffer, " fields=",
                memCache.fieldHead(), memCache.lastField());
        exToString(buffer, " constructors=",
                memCache.consHead(), memCache.lastCons());
        exToString(buffer, " methods=",
                   memCache.methodHead(), memCache.lastMethod());
    }

    private void exToString(StringBuffer buffer, String msg,
                            CtMember head, CtMember tail) {
        buffer.append(msg);
        while (head != tail) {
            head = head.next();
            buffer.append(head);
            buffer.append(", ");
        }
    }

    public AccessorMaker getAccessorMaker() {
        if (accessors == null)
            accessors = new AccessorMaker(this);

        return accessors;
    }

    public ClassFile getClassFile2() {
        ClassFile cfile = classfile;
        if (cfile != null)
            return cfile;

        classPool.compress();
        if (rawClassfile != null) {
            try {
                classfile = new ClassFile(new DataInputStream(
                                            new ByteArrayInputStream(rawClassfile)));
                rawClassfile = null;
                getCount = GET_THRESHOLD;
                return classfile;
            }
            catch (IOException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        InputStream fin = null;
        try {
            fin = classPool.openClassfile(getName());
            if (fin == null)
                throw new NotFoundException(getName());

            fin = new BufferedInputStream(fin);
            ClassFile cf = new ClassFile(new DataInputStream(fin));
            if (!cf.getName().equals(qualifiedName))
                throw new RuntimeException("cannot find " + qualifiedName + ": " 
                        + cf.getName() + " found in "
                        + qualifiedName.replace('.', '/') + ".class");

            classfile = cf;
            return cf;
        }
        catch (NotFoundException e) {
            throw new RuntimeException(e.toString(), e);
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
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
    * @see #toBytecode(DataOutputStream)
    */
   final void incGetCounter() { ++getCount; }

   /**
    * Invoked from ClassPool#compress().
    * It releases the class files that have not been recently used
    * if they are unmodified. 
    */
   void compress() {
       if (getCount < GET_THRESHOLD)
           if (!isModified() && ClassPool.releaseUnmodifiedClassFile)
               removeClassFile();
           else if (isFrozen() && !wasPruned)
               saveClassFile();

       getCount = 0;
   }

   /**
     * Converts a ClassFile object into a byte array
     * for saving memory space.
     */
    private synchronized void saveClassFile() {
        /* getMembers() and releaseClassFile() are also synchronized.
         */
        if (classfile == null || hasMemberCache() != null)
            return;

        ByteArrayOutputStream barray = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(barray);
        try {
            classfile.write(out);
            barray.close();
            rawClassfile = barray.toByteArray();
            classfile = null;
        }
        catch (IOException e) {}
    }

    private synchronized void removeClassFile() {
        if (classfile != null && !isModified() && hasMemberCache() == null)
            classfile = null;
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

    public void freeze() { wasFrozen = true; }

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
        nameReplaced();
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
        nameReplaced();

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
            nameReplaced();
        }
    }

    public boolean isInterface() {
        return Modifier.isInterface(getModifiers());
    }

    public boolean isAnnotation() {
        return Modifier.isAnnotation(getModifiers());
    }

    public boolean isEnum() {
       return Modifier.isEnum(getModifiers());
    }

    public int getModifiers() {
        ClassFile cf = getClassFile2();
        int acc = cf.getAccessFlags();
        acc = AccessFlag.clear(acc, AccessFlag.SUPER);
        int inner = cf.getInnerAccessFlags();
        if (inner != -1 && (inner & AccessFlag.STATIC) != 0)
            acc |= AccessFlag.STATIC;

        return AccessFlag.toModifier(acc);
    }

    public CtClass[] getNestedClasses() throws NotFoundException {
        ClassFile cf = getClassFile2();
        InnerClassesAttribute ica
            = (InnerClassesAttribute)cf.getAttribute(InnerClassesAttribute.tag);
        if (ica == null)
            return new CtClass[0];

        String thisName = cf.getName() + "$";
        int n = ica.tableLength();
        ArrayList list = new ArrayList(n);
        for (int i = 0; i < n; i++) {
            String name = ica.innerClass(i);
            if (name != null)
                if (name.startsWith(thisName)) {
                    // if it is an immediate nested class
                    if (name.lastIndexOf('$') < thisName.length())
                        list.add(classPool.get(name));
                }
        }

        return (CtClass[])list.toArray(new CtClass[list.size()]);
    }

    public void setModifiers(int mod) {
        ClassFile cf = getClassFile2();
        if (Modifier.isStatic(mod)) {
            int flags = cf.getInnerAccessFlags();
            if (flags != -1 && (flags & AccessFlag.STATIC) != 0)
                mod = mod & ~Modifier.STATIC;
            else
                throw new RuntimeException("cannot change " + getName() + " into a static class");
        }

        checkModify();
        cf.setAccessFlags(AccessFlag.of(mod));
    }

    public boolean hasAnnotation(Class clz) {
        ClassFile cf = getClassFile2();
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
                cf.getAttribute(AnnotationsAttribute.invisibleTag);  
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                cf.getAttribute(AnnotationsAttribute.visibleTag);  
        return hasAnnotationType(clz, getClassPool(), ainfo, ainfo2);
    }

    static boolean hasAnnotationType(Class clz, ClassPool cp,
                                     AnnotationsAttribute a1, AnnotationsAttribute a2)
    {
        Annotation[] anno1, anno2;

        if (a1 == null)
            anno1 = null;
        else
            anno1 = a1.getAnnotations();

        if (a2 == null)
            anno2 = null;
        else
            anno2 = a2.getAnnotations();

        String typeName = clz.getName();
        if (anno1 != null)
           for (int i = 0; i < anno1.length; i++)
              if (anno1[i].getTypeName().equals(typeName))
                  return true;

        if (anno2 != null)
           for (int i = 0; i < anno2.length; i++)
              if (anno2[i].getTypeName().equals(typeName))
                  return true;

        return false;
    }

    public Object getAnnotation(Class clz) throws ClassNotFoundException {
        ClassFile cf = getClassFile2();
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
                cf.getAttribute(AnnotationsAttribute.invisibleTag);  
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                cf.getAttribute(AnnotationsAttribute.visibleTag);  
        return getAnnotationType(clz, getClassPool(), ainfo, ainfo2);
    }

    static Object getAnnotationType(Class clz, ClassPool cp,
                                    AnnotationsAttribute a1, AnnotationsAttribute a2)
        throws ClassNotFoundException
    {
        Annotation[] anno1, anno2;

        if (a1 == null)
            anno1 = null;
        else
            anno1 = a1.getAnnotations();

        if (a2 == null)
            anno2 = null;
        else
            anno2 = a2.getAnnotations();

        String typeName = clz.getName();
        if (anno1 != null)
           for (int i = 0; i < anno1.length; i++)
              if (anno1[i].getTypeName().equals(typeName))
                  return toAnnoType(anno1[i], cp);

        if (anno2 != null)
           for (int i = 0; i < anno2.length; i++)
              if (anno2[i].getTypeName().equals(typeName))
                  return toAnnoType(anno2[i], cp);

        return null;
    }

    public Object[] getAnnotations() throws ClassNotFoundException {
       return getAnnotations(false);
    }

    public Object[] getAvailableAnnotations(){
       try {
           return getAnnotations(true);
       }
       catch (ClassNotFoundException e) {
           throw new RuntimeException("Unexpected exception ", e);
       }
    }

    private Object[] getAnnotations(boolean ignoreNotFound)
        throws ClassNotFoundException
    {
        ClassFile cf = getClassFile2();
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
                cf.getAttribute(AnnotationsAttribute.invisibleTag);  
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                cf.getAttribute(AnnotationsAttribute.visibleTag);  
        return toAnnotationType(ignoreNotFound, getClassPool(), ainfo, ainfo2);
    }

    static Object[] toAnnotationType(boolean ignoreNotFound, ClassPool cp,
                             AnnotationsAttribute a1, AnnotationsAttribute a2)
        throws ClassNotFoundException
    {
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

        if (!ignoreNotFound){
           Object[] result = new Object[size1 + size2];
           for (int i = 0; i < size1; i++)
               result[i] = toAnnoType(anno1[i], cp);
   
           for (int j = 0; j < size2; j++)
               result[j + size1] = toAnnoType(anno2[j], cp);
   
           return result;
        }
        else{
           ArrayList annotations = new ArrayList();
           for (int i = 0 ; i < size1 ; i++){
              try{
                 annotations.add(toAnnoType(anno1[i], cp));
              }
              catch(ClassNotFoundException e){}
           }
           for (int j = 0; j < size2; j++) {
              try{
                 annotations.add(toAnnoType(anno2[j], cp));
              }
              catch(ClassNotFoundException e){}
           }

           return annotations.toArray();
        }
    }

    static Object[][] toAnnotationType(boolean ignoreNotFound, ClassPool cp,
                                       ParameterAnnotationsAttribute a1,
                                       ParameterAnnotationsAttribute a2,
                                       MethodInfo minfo)
        throws ClassNotFoundException
    {
        int numParameters = 0;
        if (a1 != null) 
            numParameters = a1.numParameters();
        else if (a2 != null)
            numParameters = a2.numParameters();
        else
            numParameters = Descriptor.numOfParameters(minfo.getDescriptor());

        Object[][] result = new Object[numParameters][];
        for (int i = 0; i < numParameters; i++) {
            Annotation[] anno1, anno2;
            int size1, size2;

            if (a1 == null) {
                anno1 = null;
                size1 = 0;
            }
            else {
                anno1 = a1.getAnnotations()[i];
                size1 = anno1.length;
            }

            if (a2 == null) {
                anno2 = null;
                size2 = 0;
            }
            else {
                anno2 = a2.getAnnotations()[i];
                size2 = anno2.length;
            }

            if (!ignoreNotFound){
                result[i] = new Object[size1 + size2];
                for (int j = 0; j < size1; ++j)
                    result[i][j] = toAnnoType(anno1[j], cp);
   
                for (int j = 0; j < size2; ++j)
                    result[i][j + size1] = toAnnoType(anno2[j], cp);
            }
            else{
                ArrayList annotations = new ArrayList();
                for (int j = 0 ; j < size1 ; j++){
                    try{
                        annotations.add(toAnnoType(anno1[j], cp));
                    }
                    catch(ClassNotFoundException e){}
                }
                for (int j = 0; j < size2; j++){
                    try{
                        annotations.add(toAnnoType(anno2[j], cp));
                    }
                    catch(ClassNotFoundException e){}
                }

                result[i] = annotations.toArray();
            }
        }

        return result;
    }

    private static Object toAnnoType(Annotation anno, ClassPool cp)
        throws ClassNotFoundException
    {
        try {
            ClassLoader cl = cp.getClassLoader();
            return anno.toAnnotationType(cl, cp);
        }
        catch (ClassNotFoundException e) {
            ClassLoader cl2 = cp.getClass().getClassLoader();
            return anno.toAnnotationType(cl2, cp);
        }
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

    /* flush cached names.
     */
    private void nameReplaced() {
        CtMember.Cache cache = hasMemberCache();
        if (cache != null) {
            CtMember mth = cache.methodHead();
            CtMember tail = cache.lastMethod();
            while (mth != tail) {
                mth = mth.next();
                mth.nameReplaced();
            }
        }
    }

    /**
     * Returns null if members are not cached.
     */
    protected CtMember.Cache hasMemberCache() {
        if (memberCache != null)
            return (CtMember.Cache)memberCache.get();
        else
            return null;
    }

    protected synchronized CtMember.Cache getMembers() {
        CtMember.Cache cache = null;
        if (memberCache == null
            || (cache = (CtMember.Cache)memberCache.get()) == null) {
            cache = new CtMember.Cache(this);
            makeFieldCache(cache);
            makeBehaviorCache(cache);
            memberCache = new WeakReference(cache);
        }

        return cache;
    }

    private void makeFieldCache(CtMember.Cache cache) {
        List list = getClassFile2().getFields();
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            FieldInfo finfo = (FieldInfo)list.get(i);
            CtField newField = new CtField(finfo, this);
            cache.addField(newField);
        }
    }

    private void makeBehaviorCache(CtMember.Cache cache) {
        List list = getClassFile2().getMethods();
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            if (minfo.isMethod()) {
                CtMethod newMethod = new CtMethod(minfo, this);
                cache.addMethod(newMethod);
            }
            else {
                CtConstructor newCons = new CtConstructor(minfo, this);
                cache.addConstructor(newCons);
            }
        }
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

        CtMember.Cache memCache = ((CtClassType)cc).getMembers();
        CtMember field = memCache.fieldHead();
        CtMember tail = memCache.lastField();
        while (field != tail) {
            field = field.next();
            if (!Modifier.isPrivate(field.getModifiers()))
                alist.add(field);
        }
    }

    public CtField getField(String name, String desc) throws NotFoundException {
        CtField f = getField2(name, desc);
        return checkGetField(f, name, desc);
    }

    private CtField checkGetField(CtField f, String name, String desc)
        throws NotFoundException
    {
        if (f == null) {
            String msg = "field: " + name;
            if (desc != null)
                msg += " type " + desc;

            throw new NotFoundException(msg + " in " + getName());
        }
        else
            return f;
    }

    CtField getField2(String name, String desc) {
        CtField df = getDeclaredField2(name, desc);
        if (df != null)
            return df;

        try {
            CtClass[] ifs = getInterfaces();
            int num = ifs.length;
            for (int i = 0; i < num; ++i) {
                CtField f = ifs[i].getField2(name, desc);
                if (f != null)
                    return f;
            }

            CtClass s = getSuperclass();
            if (s != null)
                return s.getField2(name, desc);
        }
        catch (NotFoundException e) {}
        return null;
    }

    public CtField[] getDeclaredFields() {
        CtMember.Cache memCache = getMembers();
        CtMember field = memCache.fieldHead();
        CtMember tail = memCache.lastField();
        int num = CtMember.Cache.count(field, tail);
        CtField[] cfs = new CtField[num];
        int i = 0;
        while (field != tail) {
            field = field.next();
            cfs[i++] = (CtField)field;
        }

        return cfs;
    }

    public CtField getDeclaredField(String name) throws NotFoundException {
        return getDeclaredField(name, null);
    }

    public CtField getDeclaredField(String name, String desc) throws NotFoundException {
        CtField f = getDeclaredField2(name, desc);
        return checkGetField(f, name, desc);
    }

    private CtField getDeclaredField2(String name, String desc) {
        CtMember.Cache memCache = getMembers();
        CtMember field = memCache.fieldHead();
        CtMember tail = memCache.lastField();
        while (field != tail) {
            field = field.next();
            if (field.getName().equals(name)
                && (desc == null || desc.equals(field.getSignature())))
                return (CtField)field;
        }

        return null;
    }

    public CtBehavior[] getDeclaredBehaviors() {
        CtMember.Cache memCache = getMembers();
        CtMember cons = memCache.consHead();
        CtMember consTail = memCache.lastCons();
        int cnum = CtMember.Cache.count(cons, consTail);
        CtMember mth = memCache.methodHead();
        CtMember mthTail = memCache.lastMethod();
        int mnum = CtMember.Cache.count(mth, mthTail);

        CtBehavior[] cb = new CtBehavior[cnum + mnum];
        int i = 0;
        while (cons != consTail) {
            cons = cons.next();
            cb[i++] = (CtBehavior)cons;
        }

        while (mth != mthTail) {
            mth = mth.next();
            cb[i++] = (CtBehavior)mth;
        }

        return cb;
    }

    public CtConstructor[] getConstructors() {
        CtMember.Cache memCache = getMembers();
        CtMember cons = memCache.consHead();
        CtMember consTail = memCache.lastCons();

        int n = 0;
        CtMember mem = cons;
        while (mem != consTail) {
            mem = mem.next();
            if (isPubCons((CtConstructor)mem))
                n++;
        }

        CtConstructor[] result = new CtConstructor[n];
        int i = 0;
        mem = cons;
        while (mem != consTail) {
            mem = mem.next();
            CtConstructor cc = (CtConstructor)mem;
            if (isPubCons(cc))
                result[i++] = cc;
        }

        return result;
    }

    private static boolean isPubCons(CtConstructor cons) {
        return !Modifier.isPrivate(cons.getModifiers())
                && cons.isConstructor();
    }

    public CtConstructor getConstructor(String desc)
        throws NotFoundException
    {
        CtMember.Cache memCache = getMembers();
        CtMember cons = memCache.consHead();
        CtMember consTail = memCache.lastCons();

        while (cons != consTail) {
            cons = cons.next();
            CtConstructor cc = (CtConstructor)cons;
            if (cc.getMethodInfo2().getDescriptor().equals(desc)
                && cc.isConstructor())
                return cc;
        }

        return super.getConstructor(desc);
    }

    public CtConstructor[] getDeclaredConstructors() {
        CtMember.Cache memCache = getMembers();
        CtMember cons = memCache.consHead();
        CtMember consTail = memCache.lastCons();

        int n = 0;
        CtMember mem = cons;
        while (mem != consTail) {
            mem = mem.next();
            CtConstructor cc = (CtConstructor)mem;
            if (cc.isConstructor())
                n++;
        }

        CtConstructor[] result = new CtConstructor[n];
        int i = 0;
        mem = cons;
        while (mem != consTail) {
            mem = mem.next();
            CtConstructor cc = (CtConstructor)mem;
            if (cc.isConstructor())
                result[i++] = cc;
        }

        return result;
    }

    public CtConstructor getClassInitializer() {
        CtMember.Cache memCache = getMembers();
        CtMember cons = memCache.consHead();
        CtMember consTail = memCache.lastCons();

        while (cons != consTail) {
            cons = cons.next();
            CtConstructor cc = (CtConstructor)cons;
            if (cc.isClassInitializer())
                return cc;
        }

        return null;
    }

    public CtMethod[] getMethods() {
        HashMap h = new HashMap();
        getMethods0(h, this);
        return (CtMethod[])h.values().toArray(new CtMethod[h.size()]);
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
            CtMember.Cache memCache = ((CtClassType)cc).getMembers();
            CtMember mth = memCache.methodHead();
            CtMember mthTail = memCache.lastMethod();

            while (mth != mthTail) {
                mth = mth.next();
                if (!Modifier.isPrivate(mth.getModifiers()))
                    h.put(((CtMethod)mth).getStringRep(), mth);
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
            CtMember.Cache memCache = ((CtClassType)cc).getMembers();
            CtMember mth = memCache.methodHead();
            CtMember mthTail = memCache.lastMethod();

            while (mth != mthTail) {
                mth = mth.next();
                if (mth.getName().equals(name)
                        && ((CtMethod)mth).getMethodInfo2().getDescriptor().equals(desc))
                    return (CtMethod)mth;
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
        CtMember.Cache memCache = getMembers();
        CtMember mth = memCache.methodHead();
        CtMember mthTail = memCache.lastMethod();
        int num = CtMember.Cache.count(mth, mthTail);
        CtMethod[] cms = new CtMethod[num];
        int i = 0;
        while (mth != mthTail) {
            mth = mth.next();
            cms[i++] = (CtMethod)mth;
        }

        return cms;
    }

    public CtMethod getDeclaredMethod(String name) throws NotFoundException {
        CtMember.Cache memCache = getMembers();
        CtMember mth = memCache.methodHead();
        CtMember mthTail = memCache.lastMethod();
        while (mth != mthTail) {
            mth = mth.next();
            if (mth.getName().equals(name))
                return (CtMethod)mth;
        }

        throw new NotFoundException(name + "(..) is not found in "
                                    + getName());
    }

    public CtMethod getDeclaredMethod(String name, CtClass[] params)
        throws NotFoundException
    {
        String desc = Descriptor.ofParameters(params);
        CtMember.Cache memCache = getMembers();
        CtMember mth = memCache.methodHead();
        CtMember mthTail = memCache.lastMethod();

        while (mth != mthTail) {
            mth = mth.next();
            if (mth.getName().equals(name)
                    && ((CtMethod)mth).getMethodInfo2().getDescriptor().startsWith(desc))
                return (CtMethod)mth;
        }

        throw new NotFoundException(name + "(..) is not found in "
                                    + getName());
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
            init.check(f.getSignature());
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

        getMembers().addField(f);
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
            getMembers().remove(f);
            gcConstPool = true;
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

        getMembers().addConstructor(c);
        getClassFile2().addMethod(c.getMethodInfo2());
    }

    public void removeConstructor(CtConstructor m) throws NotFoundException {
        checkModify();
        MethodInfo mi = m.getMethodInfo2();
        ClassFile cf = getClassFile2();
        if (cf.getMethods().remove(mi)) {
            getMembers().remove(m);
            gcConstPool = true;
        }
        else
            throw new NotFoundException(m.toString());
    }

    public void addMethod(CtMethod m) throws CannotCompileException {
        checkModify();
        if (m.getDeclaringClass() != this)
            throw new CannotCompileException("bad declaring class");

        int mod = m.getModifiers();
        if ((getModifiers() & Modifier.INTERFACE) != 0) {
            m.setModifiers(mod | Modifier.PUBLIC);
            if ((mod & Modifier.ABSTRACT) == 0)
                throw new CannotCompileException(
                        "an interface method must be abstract: " + m.toString());
        }

        getMembers().addMethod(m);
        getClassFile2().addMethod(m.getMethodInfo2());
        if ((mod & Modifier.ABSTRACT) != 0)
            setModifiers(getModifiers() | Modifier.ABSTRACT);
    }

    public void removeMethod(CtMethod m) throws NotFoundException {
        checkModify();
        MethodInfo mi = m.getMethodInfo2();
        ClassFile cf = getClassFile2();
        if (cf.getMethods().remove(mi)) {
            getMembers().remove(m);
            gcConstPool = true;
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

    public void rebuildClassFile() { gcConstPool = true; }

    public void toBytecode(DataOutputStream out)
        throws CannotCompileException, IOException
    {
        try {
            if (isModified()) {
                checkPruned("toBytecode");
                ClassFile cf = getClassFile2();
                if (gcConstPool) {
                    cf.compact();
                    gcConstPool = false;
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
                // classfile = null;
            }

            getCount = 0;
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

    public boolean stopPruning(boolean stop) {
        boolean prev = !doPruning;
        doPruning = !stop;
        return prev;
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
            CtMember.Cache cache = hasMemberCache();
            if (cache != null)
                cache.addConstructor(new CtConstructor(m, this));
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

        try {
            m.rebuildStackMapIf6(classPool, cf);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
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
                        minfo.rebuildStackMapIf6(classPool, cf);
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
