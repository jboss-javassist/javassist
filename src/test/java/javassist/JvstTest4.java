package javassist;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import javassist.bytecode.*;
import javassist.bytecode.annotation.Annotation;
import javassist.expr.*;

@SuppressWarnings({"rawtypes","unchecked","unused"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JvstTest4 extends JvstTestRoot {
    public JvstTest4(String name) {
        super(name);
    }

    public void testInsertLocalVars() throws Exception {
        CtClass cc = sloader.get("test4.LocalVars");
        
        CtMethod m1 = cc.getDeclaredMethod("run");
        m1.getMethodInfo().getCodeAttribute().insertLocalVar(2, 20);
        m1.getMethodInfo().rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile());
        CtMethod m2 = cc.getDeclaredMethod("run2");
        m2.getMethodInfo().getCodeAttribute().insertLocalVar(2, 0x101);
        m2.getMethodInfo().rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile());

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(10, invoke(obj, "run"));
        assertEquals(10, invoke(obj, "run2"));
    }

    public void testCodeConv() throws Exception {
        CtClass cc = sloader.get("test4.CodeConv");
        CtMethod m1 = cc.getDeclaredMethod("m1");
        CtMethod m2 = cc.getDeclaredMethod("m2");
        CtMethod m3 = cc.getDeclaredMethod("m3");
        CodeConverter conv = new CodeConverter();
        conv.insertAfterMethod(m1, m3);
        conv.insertBeforeMethod(m2, m3);
        cc.instrument(conv);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(111033, invoke(obj, "run"));
    }

    public void testCodeConv2() throws Exception {
        CtClass cc = sloader.get("test4.CodeConv2");
        CtField f = cc.getDeclaredField("field");
        CtField f2 = cc.getDeclaredField("sf");
        CtMethod run = cc.getDeclaredMethod("run");
        CodeConverter conv = new CodeConverter();
        conv.replaceFieldRead(f, cc, "read");
        conv.replaceFieldWrite(f, cc, "write");
        conv.replaceFieldRead(f2, cc, "read");
        conv.replaceFieldWrite(f2, cc, "write");
        run.instrument(conv);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(14001600, invoke(obj, "run"));
    }

    public void testInsGap() throws Exception {
        CtClass cc = sloader.get("test4.GapSwitch");
        ExprEditor ed = new ExprEditor() {
            public void edit(MethodCall c) throws CannotCompileException {
                c.replace("{ value++; $_ = $proceed($$); }");
            }
        };

        CtMethod m1 = cc.getDeclaredMethod("run");
        m1.instrument(ed);
        CtMethod m2 = cc.getDeclaredMethod("run2");
        m2.instrument(ed);

        final CtMethod m3 = cc.getDeclaredMethod("run3");
        m3.instrument(new ExprEditor() {
            public void edit(MethodCall c) throws CannotCompileException {
                CodeIterator it = m3.getMethodInfo().getCodeAttribute().iterator();
                try {
                    it.insertGap(c.indexOfBytecode(), 5000);
                } catch (BadBytecode e) {
                    throw new CannotCompileException(e);
                }
            }
        });
        m3.getMethodInfo().rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile());

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1010, invoke(obj, "run"));
        assertEquals(1100, invoke(obj, "run2"));
        assertEquals(12222, invoke(obj, "run3"));
    }

    public void testAnnotationCheck() throws Exception {
        CtClass cc = sloader.get("test4.Anno");
        CtMethod m1 = cc.getDeclaredMethod("foo");
        CtField f = cc.getDeclaredField("value");

        assertTrue(cc.hasAnnotation(test4.Anno1.class));
        assertFalse(cc.hasAnnotation(java.lang.annotation.Documented.class));
        
        assertTrue(cc.hasAnnotation(test4.Anno1.class.getName()));
        assertFalse(cc.hasAnnotation(java.lang.annotation.Documented.class.getName()));
        
        assertEquals("empty", ((test4.Anno1)cc.getAnnotation(test4.Anno1.class)).value());
        assertNull(cc.getAnnotation(Deprecated.class));

        assertTrue(m1.hasAnnotation(test4.Anno1.class));
        assertFalse(m1.hasAnnotation(java.lang.annotation.Documented.class));
        assertTrue(m1.getAnnotation(test4.Anno1.class) != null);
        assertNull(m1.getAnnotation(Deprecated.class));

        assertTrue(f.hasAnnotation(test4.Anno1.class));
        assertFalse(f.hasAnnotation(java.lang.annotation.Documented.class));
        assertTrue(f.getAnnotation(test4.Anno1.class) != null);
        assertNull(f.getAnnotation(Deprecated.class));
    }

    public void testRename() throws Exception {
        CtClass cc = sloader.get("test4.Rename");
        cc.setName("test4.Rename2");
        cc.rebuildClassFile();
        cc.writeFile();
        CtClass cc2 = sloader.get("test4.IRename");
        cc2.replaceClassName("test4.Rename", "test4.Rename2");
        cc2.rebuildClassFile();
        cc2.writeFile();
        Object obj = make(cc.getName());
        assertEquals("test4.Rename2", obj.getClass().getName());
        assertEquals(14, invoke(obj, "run"));
    }

    public void testRename2() throws Exception {
        CtClass cc = sloader.get("test4.Signature");
        cc.setName("test4.Sig");
        cc.rebuildClassFile();
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "run"));
    }

    public void testJIRA93() throws Exception {
        ClassPool cp = ClassPool.getDefault();
        CtClass cc = sloader.getCtClass("test4.JIRA93");
        CtMethod m = cc.getDeclaredMethod("foo");

        m.addLocalVariable("bar", CtClass.longType);
        // The original bug report includes the next line.
        // But this is not a bug.
        //m.insertAfter("bar;", true);
        // Instead, the following code is OK. 
        m.insertBefore("bar = 0;");
        m.insertAfter("bar;", false);

        cc.writeFile();
        Object obj = make(cc.getName());
    }

    public void testNewRemover() throws Exception {
        CtClass cc = sloader.get("test4.NewRemover");
        CtMethod mth = cc.getDeclaredMethod("make");
        mth.getMethodInfo().rebuildStackMap(cc.getClassPool());
        mth.getMethodInfo().rebuildStackMapForME(cc.getClassPool());
        //cc.debugWriteFile("debug");
        CodeConverter conv = new CodeConverter();
        conv.replaceNew(cc, cc, "make2");
        mth.instrument(conv);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(10, invoke(obj, "run"));
    }

    public void testClassFileWriter() throws Exception {
        ClassFileWriter cfw = new ClassFileWriter(ClassFile.JAVA_4, 0);
        ClassFileWriter.ConstPoolWriter cpw = cfw.getConstPool();

        ClassFileWriter.FieldWriter fw = cfw.getFieldWriter();
        fw.add(AccessFlag.PUBLIC, "value", "J", null);
        fw.add(AccessFlag.PROTECTED | AccessFlag.STATIC, "value2", "Ljava/lang/String;", null);

        ClassFileWriter.MethodWriter mw = cfw.getMethodWriter();

        mw.begin(AccessFlag.PUBLIC, MethodInfo.nameInit, "()V", null, null);
        assertEquals(0, mw.size());
        mw.add(Opcode.ALOAD_0);
        assertEquals(1, mw.size());
        mw.addInvoke(Opcode.INVOKESPECIAL, "java/lang/Object", MethodInfo.nameInit, "()V");
        mw.add(Opcode.RETURN);
        mw.codeEnd(1, 1);
        mw.end(null, null);

        mw.begin(AccessFlag.PUBLIC, "move", "(II)V", null, null);
        assertEquals(0, mw.size());
        mw.add(Opcode.ALOAD_0);
        mw.addInvoke(Opcode.INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
        assertEquals(4, mw.size());
        mw.add(Opcode.POP);
        mw.add(Opcode.RETURN);
        mw.add(Opcode.POP);
        mw.add(Opcode.RETURN);
        mw.codeEnd(1, 3);
        mw.addCatch(0, 4, 6, cpw.addClassInfo("java/lang/Exception"));
        mw.addCatch(0, 4, 6, cpw.addClassInfo("java/lang/Throwable"));
        mw.end(null, null);

        String[] exceptions = { "java/lang/Exception", "java/lang/NullPointerException" }; 
        mw.begin(AccessFlag.PUBLIC, "move2", "()V", exceptions, null);
        mw.add(Opcode.RETURN);
        mw.codeEnd(0, 1);
        StackMapTable.Writer stack = new StackMapTable.Writer(32);
        stack.sameFrame(1);
        mw.end(stack, null);

        mw.begin(AccessFlag.PUBLIC, "foo", "()I", null, null);
        mw.add(Opcode.ICONST_2);
        mw.add(Opcode.IRETURN);
        mw.codeEnd(1, 1);
        mw.end(null, null);

        byte[] out = cfw.end(AccessFlag.PUBLIC, cpw.addClassInfo("test4/WrittenFile"),
                             cpw.addClassInfo("java/lang/Object"),
                             null, null);
        FileOutputStream fos = new FileOutputStream("test4/WrittenFile.class");
        fos.write(out);
        fos.close();
        Object obj = make("test4.WrittenFile");
        assertNotNull(obj);
        assertEquals(2, invoke(obj, "foo"));
    }

    public void testClassFileWriter2() throws Exception {
        ClassFileWriter cfw = new ClassFileWriter(ClassFile.JAVA_4, 0);
        ClassFileWriter.ConstPoolWriter cpw = cfw.getConstPool();

        ClassFileWriter.FieldWriter fw = cfw.getFieldWriter();
        fw.add(AccessFlag.PUBLIC | AccessFlag.STATIC, "value", "I", null);

        ClassFileWriter.MethodWriter mw = cfw.getMethodWriter();

        mw.begin(AccessFlag.PUBLIC, MethodInfo.nameInit, "()V", null, null);
        mw.add(Opcode.ALOAD_0);
        mw.addInvoke(Opcode.INVOKESPECIAL, "java/lang/Object", MethodInfo.nameInit, "()V");
        mw.add(Opcode.RETURN);
        mw.codeEnd(1, 1);
        mw.end(null, null);

        String[] exceptions = { "java/lang/Exception" };
        mw.begin(AccessFlag.PUBLIC | AccessFlag.ABSTRACT, "move", "(II)V", exceptions, null);
        mw.end(null, null);

        int thisClass = cpw.addClassInfo("test4/WrittenFile2");
        int superClass = cpw.addClassInfo("java/lang/Object");

        cfw.end(new DataOutputStream(new FileOutputStream("test4/WrittenFile2.class")),
                AccessFlag.PUBLIC | AccessFlag.ABSTRACT, thisClass, superClass,
                null, null);

        File f = new File("test4/WrittenFile2.class");
        byte[] file = new byte[(int)f.length()];
        FileInputStream fis = new FileInputStream(f);
        fis.read(file);
        fis.close();

        byte[] out = cfw.end(AccessFlag.PUBLIC | AccessFlag.ABSTRACT, thisClass,
                             superClass, null, null);

        assertEquals(out.length, file.length);
        for (int i = 0; i < out.length; i++)
            assertEquals(out[i], file[i]);

        CtClass sub = dloader.makeClass("test4.WrittenFile2sub", dloader.get("test4.WrittenFile2"));
        sub.addMethod(CtMethod.make("public void move(int i, int j) {}", sub));
        sub.addMethod(CtMethod.make("public int foo() { move(0, 1); return 1; }", sub));
        sub.writeFile();
        Object obj = make("test4.WrittenFile2sub");
        assertEquals(1, invoke(obj, "foo"));
    }

    public void testClassFileWriter3() throws Exception {
        ClassFileWriter cfw = new ClassFileWriter(ClassFile.JAVA_4, 0);
        ClassFileWriter.ConstPoolWriter cpw = cfw.getConstPool();
        int superClass = cpw.addClassInfo("java/lang/Object");

        final int syntheticTag = cpw.addUtf8Info("Synthetic");
        ClassFileWriter.AttributeWriter attribute = new ClassFileWriter.AttributeWriter() {
            public void write(DataOutputStream out) throws java.io.IOException {
                out.writeShort(syntheticTag);
                out.writeInt(0);
            }

            public int size() {
                return 1;
            }
        };

        ClassFileWriter.FieldWriter fw = cfw.getFieldWriter();
        fw.add(AccessFlag.PUBLIC, "value", "J", null);
        fw.add(AccessFlag.PROTECTED | AccessFlag.STATIC, "value2", "Ljava/lang/String;", attribute);

        ClassFileWriter.MethodWriter mw = cfw.getMethodWriter();

        mw.begin(AccessFlag.PUBLIC, MethodInfo.nameInit, "()V", null, attribute);
        mw.add(Opcode.ALOAD_0);
        mw.add(Opcode.INVOKESPECIAL);
        mw.add16(cpw.addMethodrefInfo(superClass, cpw.addNameAndTypeInfo(MethodInfo.nameInit, "()V")));
        // mw.addInvoke(Opcode.INVOKESPECIAL, "java/lang/Object", MethodInfo.nameInit, "()V");
        mw.add(Opcode.RETURN);
        mw.codeEnd(1, 1);
        mw.end(null, null);

        mw.begin(AccessFlag.PUBLIC, "foo", "()I", null, attribute);
        mw.add(Opcode.ICONST_2);
        mw.add(Opcode.IRETURN);
        mw.codeEnd(1, 1);
        mw.end(null, null);

        int thisClass = cpw.addClassInfo("test4/WrittenFile3");
        cfw.end(new DataOutputStream(new FileOutputStream("test4/WrittenFile3.class")),
                AccessFlag.PUBLIC, thisClass, superClass,
                null, attribute);

        File f = new File("test4/WrittenFile3.class");
        byte[] file = new byte[(int)f.length()];
        FileInputStream fis = new FileInputStream(f);
        fis.read(file);
        fis.close();

        byte[] out = cfw.end(AccessFlag.PUBLIC, thisClass, superClass,
                             null, attribute);

        assertEquals(out.length, file.length);
        for (int i = 0; i < out.length; i++)
            assertEquals(out[i], file[i]);

        Object obj = make("test4.WrittenFile3");
        assertNotNull(obj);
        assertEquals(2, invoke(obj, "foo"));
    }

    public void testCtArray() throws Exception {
        CtClass cc = sloader.get("int");
        assertEquals(Modifier.FINAL | Modifier.PUBLIC, cc.getModifiers());
        cc = sloader.get("int[]");
        assertEquals(Modifier.FINAL | Modifier.PUBLIC, cc.getModifiers());
        cc = sloader.get("java.lang.String[]");
        assertEquals(Modifier.FINAL | Modifier.PUBLIC, cc.getModifiers());
        CtClass[] intfs = cc.getInterfaces();
        assertEquals(Cloneable.class.getName(), intfs[0].getName());
        assertEquals(java.io.Serializable.class.getName(), intfs[1].getName());
        cc = sloader.get("test4.CtArrayTest[]");
        assertEquals(Modifier.FINAL | Modifier.PUBLIC, cc.getModifiers());
    }

    public void testAnalysisType() throws Exception {
        testAnalysisType2(sloader.get("int[]"), 1);
        testAnalysisType2(sloader.get("java.lang.String[][]"), 2);
        sloader.makeClass("A");
        testAnalysisType2(sloader.getCtClass("A"), 0);
        testAnalysisType2(sloader.getCtClass("A[]"), 1);
        testAnalysisType2(sloader.getCtClass("A[][]"), 2);
    }

    private void testAnalysisType2(CtClass cc, int size) throws Exception {
        javassist.bytecode.analysis.Type t = javassist.bytecode.analysis.Type.get(cc);
        assertEquals(cc.getName(), size, t.getDimensions());
    }

    public void testArrayType() throws Exception {
        CtClass at = sloader.get("java.lang.Object[]");
        CtClass[] intfs = at.getInterfaces();
        assertEquals(intfs.length, 2);
        assertEquals(intfs[0].getName(), java.lang.Cloneable.class.getName());
        assertEquals(intfs[1].getName(), java.io.Serializable.class.getName());

        assertTrue(at.subtypeOf(sloader.get(java.lang.Object.class.getName())));
        assertTrue(at.subtypeOf(intfs[0]));
        assertTrue(at.subtypeOf(intfs[1]));
        assertTrue(at.subtypeOf(intfs[1]));
        CtClass subt = sloader.get(java.text.CharacterIterator.class.getName());
        assertFalse(at.subtypeOf(subt));
    }

    public void testGetFieldDesc() throws Exception {
        CtClass cc = sloader.get("test4.GetFieldDesc");
        cc.getDeclaredField("f", "I");
        cc.getField("s", "Ljava/lang/String;");
        CtClass cc2 = sloader.get("test4.GetFieldDescSub");
        assertEquals(cc2.getField("s", "Ljava/lang/String;").getDeclaringClass().getName(),
                "test4.GetFieldDesc");
        assertEquals(cc2.getField("s", "I").getDeclaringClass().getName(),
                "test4.GetFieldDescSub");
    }

    public void testMakeMethod() throws CannotCompileException {
        CtClass ctClass = sloader.makeClass("test4.MakeMethod2");
        CtNewMethod.make("public String foox(){return test4.MakeMethod.foo();}", ctClass);
        CtNewMethod.make("public String foo(){return test4.MakeMethod.foo();}", ctClass);
    }

    public void testVarArgs() throws Exception {
        CtClass cc = sloader.get("test4.VarArgs");
        CtMethod m = CtMethod.make("public int foo(int i, String[] args) { return args.length; }", cc);
        m.setModifiers(m.getModifiers() | Modifier.VARARGS);
        cc.addMethod(m);
        m = CtMethod.make("public int run() { return goo(7, new int[] { 1, 2, 3 }); }", cc);
        cc.addMethod(m);
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(3, invoke(obj, "run"));
    }

    public void testGetAllRef() throws Exception {
        CtClass cc = sloader.get("test4.GetAllRef");
        ClassFile cf = cc.getClassFile();
        AttributeInfo ainfo
            = cf.getAttribute(AnnotationsAttribute.visibleTag);
        ClassMap map = new ClassMap();
        map.put("test4.GetAllRefAnno", "test4.GetAllRefAnno2");
        map.put("test4.GetAllRefEnum", "test4.GetAllRefEnum2");
        map.put("java.lang.String", "java.lang.StringBuilder");
        cf.addAttribute(ainfo.copy(cf.getConstPool(), map));
        cc.writeFile();
        cc.detach();

        cc = dloader.get(cc.getName());
        test4.GetAllRefAnno2 anno
            = (test4.GetAllRefAnno2)cc.getAnnotation(test4.GetAllRefAnno2.class);
        assertEquals(test4.GetAllRefEnum2.A, anno.getA());
        assertEquals(StringBuilder.class, anno.getC());
    }

    public void testGetAllRefB() throws Exception {
        CtClass cc = sloader.get("test4.GetAllRefB");
        ClassMap map = new ClassMap();
        map.put("test4.GetAllRefAnno", "test4.GetAllRefAnno2");
        map.put("test4.GetAllRefEnum", "test4.GetAllRefEnum2");
        map.put("java.lang.String", "java.lang.StringBuilder");
        cc.replaceClassName(map);
        //cc.replaceClassName("test4.GetAllRefAnno", "test4.GetAllRefAnno2");
        cc.writeFile();

        cc = dloader.get(cc.getName());
        test4.GetAllRefAnno2 anno
            = (test4.GetAllRefAnno2)cc.getAnnotation(test4.GetAllRefAnno2.class);
        assertEquals(test4.GetAllRefEnum2.A, anno.getA());
        assertEquals(StringBuilder.class, anno.getC());
        /*
        AnnotationsAttribute aainfo = (AnnotationsAttribute)
            cc.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
        Annotation[] a = aainfo.getAnnotations();
        System.err.println(a[0].getTypeName());
        System.err.println(a[0]);
        */
    }

    public void testGetAllRefC() throws Exception {
        CtClass cc = sloader.get("test4.GetAllRefC");
        HashSet set = new HashSet();
        set.add("java.lang.Object");
        set.add("java.lang.String");
        set.add("test4.GetAllRefC");
        set.add("test4.GetAllRefAnno");
        set.add("test4.GetAllRefEnum");
        set.add("test4.GetAllRefAnnoC");
        set.add("test4.GetAllRefAnnoC2");
        set.add("test4.GetAllRefAnnoC3");
        set.add("test4.GetAllRefAnnoC4");
        java.util.Collection<String> refs
            = (java.util.Collection<String>)cc.getRefClasses();
        assertEquals(set.size(), refs.size());
        for (String s: refs) {
            assertTrue(set.contains(s));
        }
    }

    public void testGetAllRefInner() throws Exception {
        HashSet set = new HashSet();
        set.add("java.lang.Object");
        set.add("test4.GetAllRefInnerTest");
        set.add("test4.GetAllRefInnerTest$1");
        set.add("test4.GetAllRefInnerTest$2");
        CtClass cc = sloader.get("test4.GetAllRefInnerTest");
        int size = 0;
        for (Object s: cc.getRefClasses()) {
            assertTrue((String)s, set.contains(s));
            ++size;
        }

        assertEquals(set.size(), size);
    }

    public void testNestedClass() throws Exception {
        CtClass cc = sloader.get("test4.NestedClass$1");
        CtClass[] tab = cc.getNestedClasses();
        assertEquals(1, tab.length);
        assertEquals("test4.NestedClass$1$1", tab[0].getName());

        cc = sloader.get("test4.NestedClass$1$1");
        tab = cc.getNestedClasses();
        assertEquals(0, tab.length);

        cc = sloader.get("test4.NestedClass");
        tab = cc.getNestedClasses();
        for (CtClass c: tab) {
            System.err.println(c.getName());
        }

        // Eclipse compiler sets tab.length to 4 but javac sets to 3. 
        assertTrue(tab.length == 4 || tab.length == 3);
        for (CtClass c: tab) {
            String name = c.getName();
            assertTrue(name.equals("test4.NestedClass$N")
                       || name.equals("test4.NestedClass$S")
                       || name.equals("test4.NestedClass$1")
                       || name.equals("test4.NestedClass$1In"));
        }
            
        cc = sloader.get("test4.NestedClass$1In");
        tab = cc.getNestedClasses();
        assertEquals(0, tab.length);
    }

    public void testGetClasses() throws Exception {
        CtClass cc = sloader.get("test4.NestedClass");
        CtClass[] tab = cc.getDeclaredClasses();

        // Eclipse compiler sets tab.length to 4 but javac sets to 3. 
        assertTrue(tab.length == 4 || tab.length == 3);
        for (CtClass c: tab) {
            String name = c.getName();
            assertTrue(name.equals("test4.NestedClass$N")
                       || name.equals("test4.NestedClass$S")
                       || name.equals("test4.NestedClass$1")
                       || name.equals("test4.NestedClass$1In"));
        }

        cc = sloader.get("test4.NestedClass$1In");
        tab = cc.getDeclaredClasses();
        assertEquals(0, tab.length);
    }

    public void testImportPac() throws Exception {
        CtClass cc = sloader.makeClass("test4.TestImpP");
        sloader.importPackage("test4.NewImportPac");
        try {
            cc.addMethod(CtNewMethod.make(
                "public int foo(){ " + 
                "  ImportPac obj = new ImportPac();" +
                "  return obj.getClass().getName().length(); }", cc));
            fail("ImportPac was found");
        }
        catch (CannotCompileException e) {}

        cc.addMethod(CtNewMethod.make(
                "public int bar(){ " + 
                        "  NewImportPac obj = new NewImportPac();" +
                        "  return obj.getClass().getName().length(); }", cc));
        sloader.clearImportedPackages();
    }

    public void testLength() throws Exception {
        CtClass cc = sloader.makeClass("test4.LengthTest");
        cc.addMethod(CtNewMethod.make(
                "public int len(String s){ " + 
                        "  return s.length(); }", cc));
        cc.addField(CtField.make("int length = 100;", cc));
        cc.addConstructor(CtNewConstructor.defaultConstructor(cc));
        cc.addMethod(CtNewMethod.make(
                "public int run(){ " +
                        "  test4.LengthTest t = new test4.LengthTest();" +
                        "  return len(\"foo\") + t.length + test4.length.m(); }", cc));
        try {
            cc.addMethod(CtNewMethod.make(
                    "public int run(){ " + 
                            "  return test4no.length.m(); }", cc));
            fail("test4no was found!");
        }
        catch (CannotCompileException e) {
            System.out.println(e);
        }
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(110, invoke(obj, "run"));
    }

    public void testAaload() throws Exception {
        CtClass clazz = sloader.get("test4.Aaload");
        CtMethod method = clazz.getMethod("narf", "()V");
        method.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws CannotCompileException {
                String name = call.getMethodName();
                if (name.equals("addActionListener"))
                    call.replace("$0." + name + "($$);");
            }
        });
    }

    @SuppressWarnings("deprecation")
    public void testMakePackage() throws Exception {
        if (ClassFile.MAJOR_VERSION >= ClassFile.JAVA_9) {
            ClassPool pool = ClassPool.getDefault();
            try {
                pool.makePackage(pool.getClassLoader(), "test4.pack2");
                fail("makePackage() ran");
            }
            catch (CannotCompileException e) {}
        }
    }

    @SuppressWarnings("deprecation")
    public void testPackage() throws Throwable {    // JASSIST-147
        String packageName = "test4.pack";
        ClassPool pool = ClassPool.getDefault();
        try {
            pool.makePackage(pool.getClassLoader(), packageName);
            pool.makePackage(pool.getClassLoader(), packageName);
        }
        catch (CannotCompileException e) {
            if (ClassFile.MAJOR_VERSION >= ClassFile.JAVA_9)
                return;         // makePackage() does not work in Java 9.
        }

        CtClass ctcl = pool.makeClass("test4.pack.Clazz");
        Class cl = ctcl.toClass();
        Object obj = cl.getConstructor().newInstance();
        assertEquals(packageName, obj.getClass().getPackage().getName());
    }

    public static final String BASE_PATH = "../../";
    public static final String JAVASSIST_JAR = BASE_PATH + "javassist.jar";
    public static final String CLASSES_FOLDER = BASE_PATH + "build/classes";
    public static final String TEST_CLASSES_FOLDER = BASE_PATH + "build/test-classes";

    public static class Inner1 {
        public static int get() {
            return 0;
        }
    }

    public void testJIRA150() throws Exception {
        ClassPool pool = new ClassPool(true);
        for(int paths=0; paths<50; paths++) {
            pool.appendClassPath(JAVASSIST_JAR);
            pool.appendClassPath(CLASSES_FOLDER);
            pool.appendClassPath(TEST_CLASSES_FOLDER);
        }
        CtClass cc = pool.get("Jassist150$Inner1");
        CtMethod ccGet = cc.getDeclaredMethod("get");
        long startTime = System.currentTimeMillis();
        for(int replace=0; replace<1000; replace++) {
            ccGet.setBody(
                    "{ int n1 = java.lang.Integer#valueOf(1); " +
                    "  int n2 = java.lang.Integer#valueOf(2); " +
                    "  int n3 = java.lang.Integer#valueOf(3); " +
                    "  int n4 = java.lang.Integer#valueOf(4); " +
                    "  int n5 = java.lang.Integer#valueOf(5); " +
                    "  return n1+n2+n3+n4+n5; }");
        }
        long endTime = System.currentTimeMillis();
        for(int replace=0; replace<1000; replace++) {
            ccGet.setBody(
                    "{ int n1 = java.lang.Integer.valueOf(1); " +
                    "  int n2 = java.lang.Integer.valueOf(2); " +
                    "  int n3 = java.lang.Integer.valueOf(3); " +
                    "  int n4 = java.lang.Integer.valueOf(4); " +
                    "  int n5 = java.lang.Integer.valueOf(5); " +
                    "  return n1+n2+n3+n4+n5; }");
        }
        long endTime2 = System.currentTimeMillis();
        for(int replace=0; replace<1000; replace++) {
            ccGet.setBody(
                "{ int n1 = Integer.valueOf(1); " +
                "  int n2 = Integer.valueOf(2); " +
                "  int n3 = Integer.valueOf(3); " +
                "  int n4 = Integer.valueOf(4); " +
                "  int n5 = Integer.valueOf(5); " +
                "  return n1+n2+n3+n4+n5; }");
        }
        long endTime3 = System.currentTimeMillis();
        long t1 = endTime - startTime;
        long t2 = endTime2 - endTime;
        long t3 = endTime3 - endTime2;
        System.out.println("JIRA150: " + t1 + ", " + t2 + ", " + t3);
        assertTrue("performance test (the next try may succeed): " + t2 + " < 6 * " + t1,
                   t2 < t1 * 6);
        assertTrue("performance test (the next try may succeed): " + t3 + " < 3 * " + t1,
                   t3 < t1 * 3);
    }

    public void testJIRA150b() throws Exception {
        int origSize = javassist.compiler.MemberResolver.getInvalidMapSize();
        int N = 100;
        for (int k = 0; k < N; k++) {
            ClassPool pool = new ClassPool(true);
            for(int paths=0; paths<50; paths++) {
                pool.appendClassPath(JAVASSIST_JAR);
                pool.appendClassPath(CLASSES_FOLDER);
                pool.appendClassPath(TEST_CLASSES_FOLDER);
            }
            CtClass cc = pool.get("Jassist150$Inner1");
            CtMethod ccGet = cc.getDeclaredMethod("get");
            for(int replace=0; replace < 5; replace++) {
                ccGet.setBody(
                    "{ int n1 = java.lang.Integer#valueOf(1); " +
                    "  int n2 = java.lang.Integer#valueOf(2); " +
                    "  int n3 = java.lang.Integer#valueOf(3); " +
                    "  int n4 = java.lang.Integer#valueOf(4); " +
                    "  int n5 = java.lang.Integer#valueOf(5); " +
                    "  return n1+n2+n3+n4+n5; }");
            }
            pool = null;
        }

        // try to run garbage collection.
        int[][] mem = new int[100][];
        int[] large;
        for (int i = 0; i < 100; i++) {
            large = new int[1000000];
            large[large.length - 2] = 9 + i;
            mem[i] = large;
        }
        System.gc();
        System.gc();
        int size = javassist.compiler.MemberResolver.getInvalidMapSize();
        System.out.println("JIRA150b " + size + " " + mem[mem.length - 1][mem[0].length - 2]);
        // Now this seems obsolete.
        // assertTrue("JIRA150b size: " + origSize + " " + size, size < origSize + N);
    }

    public void testJIRA152() throws Exception {
        CtClass cc = sloader.get("test4.JIRA152");
        CtMethod mth = cc.getDeclaredMethod("buildColumnOverride");
        //CtMethod mth = cc.getDeclaredMethod("tested");
        mth.instrument(new ExprEditor() {
            public void edit(MethodCall c) throws CannotCompileException {
                c.replace("try{ $_ = $proceed($$); } catch (Throwable t) { throw t; }");
            }
        });
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "test"));
    }

    public void testJIRA151() {
        // try it using classloader of TestDescForName Desc.useContextClassLoader = false;
        assertTrue(javassist.runtime.Desc.getClazz("[Ljava.lang.String;") != null);
        //Thread.currentThread().setContextClassLoader(TestDescForName.class.getClassLoader());
        boolean old = javassist.runtime.Desc.useContextClassLoader;
        javassist.runtime.Desc.useContextClassLoader = true;
        assertTrue(javassist.runtime.Desc.getClazz("[Ljava.lang.String;") != null);
        javassist.runtime.Desc.useContextClassLoader = old;
    }

    public void testJIRA166() throws Exception {
        CtClass cc = sloader.get("test4.JIRA166");
        cc.instrument(new ExprEditor() {
            public void edit(FieldAccess fa) throws CannotCompileException {
                if (fa.isReader() && fa.getFieldName().equals("length"))
                    fa.replace("length = self().length + \"!\"; $_ = ($r) this.length.substring(1);");
            }
        });
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "run"));
    }

    public void testGenericSignature() throws Exception {
        CtClass cc = sloader.makeClass("test4.GenSig");
        CtClass objClass = sloader.get(CtClass.javaLangObject);
        SignatureAttribute.ClassSignature cs
            = new SignatureAttribute.ClassSignature(
                    new SignatureAttribute.TypeParameter[] {
                            new SignatureAttribute.TypeParameter("T") });
        cc.setGenericSignature(cs.encode());    // <T:Ljava/lang/Object;>Ljava/lang/Object;

        CtField f = new CtField(objClass, "value", cc);
        SignatureAttribute.TypeVariable tvar = new SignatureAttribute.TypeVariable("T");
        f.setGenericSignature(tvar.encode());   // TT;
        cc.addField(f);

        CtMethod m = CtNewMethod.make("public Object get(){return value;}", cc);
        SignatureAttribute.MethodSignature ms
            = new SignatureAttribute.MethodSignature(null, null, tvar, null);
        m.setGenericSignature(ms.encode());     // ()TT;
        cc.addMethod(m);

        CtMethod m2 = CtNewMethod.make("public void set(Object v){value = v;}", cc);
        SignatureAttribute.MethodSignature ms2
            = new SignatureAttribute.MethodSignature(null, new SignatureAttribute.Type[] { tvar },
                                                     new SignatureAttribute.BaseType("void"), null);
        m2.setGenericSignature(ms2.encode());   // (TT;)V;
        cc.addMethod(m2);

        cc.writeFile();
        Object obj = make(cc.getName());
        Class clazz = obj.getClass();
        assertEquals("T", clazz.getTypeParameters()[0].getName());
        assertEquals("T", ((java.lang.reflect.TypeVariable)clazz.getDeclaredField("value").getGenericType()).getName());
        java.lang.reflect.Method rm = clazz.getDeclaredMethod("get", new Class[0]);
        assertEquals("T", ((java.lang.reflect.TypeVariable)rm.getGenericReturnType()).getName());
        java.lang.reflect.Method rm2 = clazz.getDeclaredMethod("set", new Class[] { Object.class });
        assertEquals("T", ((java.lang.reflect.TypeVariable)rm2.getGenericParameterTypes()[0]).getName());
    }

    public void testJIRA171() throws Exception {
        SignatureAttribute.MethodSignature ms
            = SignatureAttribute.toMethodSignature("(Ljava/lang/Object;Lorg/apache/hadoop/io/Text;"
              + "Lorg/apache/hadoop/mapreduce/Mapper<Ljava/lang/Object;Lorg/apache/hadoop/io/Text;"
              + "Lorg/apache/hadoop/io/Text;Lorg/apache/hadoop/io/IntWritable;>.Context;)V");
        String s = ms.toString();
        System.out.println(s);
        assertEquals("<> (java.lang.Object, org.apache.hadoop.io.Text, "
                     + "org.apache.hadoop.mapreduce.Mapper<java.lang.Object, org.apache.hadoop.io.Text, "
                     + "org.apache.hadoop.io.Text, org.apache.hadoop.io.IntWritable>.Context) void", s);
    }

    public void testAfter() throws Exception {
        CtClass cc = sloader.get("test4.AfterTest");
        CtMethod m1 = cc.getDeclaredMethod("m1");
        m1.insertAfter("print();");
        CtMethod m2 = cc.getDeclaredMethod("m2");
        m2.insertAfter("print();");
        CtMethod m3 = cc.getDeclaredMethod("m3");
        m3.insertAfter("print();");
        CtMethod m4 = cc.getDeclaredMethod("m4");
        m4.insertAfter("print();");

        CtMethod mm1 = cc.getDeclaredMethod("mm1");
        mm1.insertAfter("print();", true);
        CtMethod mm2 = cc.getDeclaredMethod("mm2");
        mm2.insertAfter("print();", true);
        CtMethod mm3 = cc.getDeclaredMethod("mm3");
        mm3.insertAfter("print();", true);
        CtMethod mm4 = cc.getDeclaredMethod("mm4");
        mm4.insertAfter("print();", true);

        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(131, invoke(obj, "test1"));
        assertEquals(112, invoke(obj, "test2"));
        assertEquals(10, invoke(obj, "test3"));
        assertEquals(100, invoke(obj, "test4"));

        assertEquals(131, invoke(obj, "test11"));
        assertEquals(112, invoke(obj, "test22"));
        assertEquals(10, invoke(obj, "test33"));
        assertEquals(100, invoke(obj, "test44"));
    }

    public void testJIRA186() throws Exception {
    	CtClass cc = sloader.get("test4.JIRA186");
    	cc.getDeclaredMethod("test").insertBefore("{" +
    			  "  java.util.List l = new java.util.ArrayList();" +
    			  "  l.add(this.toString());" +
    			  "}");
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "test"));
    }

    // JASSIST-190
    public void testMultipleCatch() throws Exception {
        CtClass cc = sloader.get("test4.MultiCatch");
        CtMethod m1 = cc.getDeclaredMethod("m1");
        m1.insertAfter("print();");
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(12, invoke(obj, "test1"));
    }

    // JASSIST-185
    public void testLocalVariableTypeTable() throws Exception {
        CtClass cc = sloader.get("test4.Lvtt");
        CtMethod m = cc.getDeclaredMethod("run");
        m.addParameter(CtClass.intType);
        cc.writeFile();
        Object obj = make(cc.getName());
    }

    // JASSISt-181
    public void testAnnotationWithGenerics() throws Exception {
    	CtClass cc0 = sloader.get("test4.JIRA181b");
        CtField field0 = cc0.getField("aField");
        String s0 = field0.getAnnotation(test4.JIRA181b.Condition.class).toString();
        assertEquals("@test4.JIRA181b$Condition(condition=java.lang.String.class)", s0);
        CtField field01 = cc0.getField("aField2");
        String s01 = field01.getAnnotation(test4.JIRA181b.Condition.class).toString();
        assertEquals("@test4.JIRA181b$Condition(condition=void.class)", s01);

        CtClass cc = sloader.get("test4.JIRA181");
        CtField field = cc.getField("aField");
        String s = field.getAnnotation(test4.JIRA181.Condition.class).toString();
        if (ClassFile.MAJOR_VERSION < ClassFile.JAVA_9)
            assertEquals("@test4.JIRA181$Condition(condition=test4.JIRA181<T>.B.class)", s);
        else
            assertEquals("@test4.JIRA181$Condition(condition=test4.JIRA181.B.class)", s);

        CtField field2 = cc.getField("aField2");
        String s2 = field2.getAnnotation(test4.JIRA181.Condition2.class).toString();
        if (ClassFile.MAJOR_VERSION < ClassFile.JAVA_9)
            assertEquals("@test4.JIRA181$Condition2(condition=test4.JIRA181<T>.B[].class)", s2);
        else
            assertEquals("@test4.JIRA181$Condition2(condition=test4.JIRA181.B[].class)", s2);
    }

    public void testJIRA195() throws Exception {
        CtClass cc = sloader.get("test4.JIRA195");
        CtMethod mth = cc.getDeclaredMethod("test");
        mth.getMethodInfo().rebuildStackMap(cc.getClassPool());
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(4, invoke(obj, "run"));        
    }

    public void testJIRA188() throws Exception {
        CtClass cc = sloader.makeClass("test4.JIRA188");
        CtField f = new CtField(CtClass.intType, "f", cc);
        f.setModifiers(Modifier.PRIVATE);
        cc.addField(f);
        cc.addMethod(CtNewMethod.make(
                "public int getf(test4.JIRA188 p){ return p.f; }", cc));
        cc.detach();
        // System.gc();
        try {
            cc = sloader.get("test4.JIRA188");
            fail("test4.JIRA188 found");
        }
        catch (NotFoundException e) {}
        cc = sloader.makeClass("test4.JIRA188");
        cc.addField(new CtField(CtClass.intType, "g", cc));
        cc.addMethod(CtNewMethod.make(
                "public int getf(test4.JIRA188 p){ return p.g; }", cc));
    }

    public void testJIRA158() throws Exception {
        CtClass cc = sloader.get("test4.JIRA158");
        cc.addMethod(CtMethod.make("public int run() { return obj.foo(jj, dd) + obj.bar(j, d); }", cc));
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(15, invoke(obj, "run"));
    }

    public void testJIRA207() throws Exception {
        CtClass cc = sloader.get("test4.JIRA207");
        CtMethod cm = cc.getDeclaredMethod("foo");
        cm.insertBefore("throw new Exception();");

        CtMethod cm2 = cc.getDeclaredMethod("run2");
        cm2.insertBefore("throw new Exception();");
        cc.writeFile();
        Object obj = make(cc.getName());
        try {
            invoke(obj, "run2");
            fail("run2");
        }
        catch (Exception e) {}
    }

    public void testJIRA212() throws Exception {
        CtClass cc = sloader.get("test4.JIRA212");
        for (final CtBehavior behavior : cc.getDeclaredBehaviors()) {
            behavior.instrument(new ExprEditor() {
                    public void edit(FieldAccess fieldAccess) throws CannotCompileException {
                        String fieldName = fieldAccess.getFieldName().substring(0,1).toUpperCase() + fieldAccess.getFieldName().substring(1);
                        if (fieldAccess.isReader()) {
                            // Rewrite read access
                            fieldAccess.replace("$_ = $0.get" + fieldName + "();");
                        } else if (fieldAccess.isWriter()) {
                            // Rewrite write access
                            fieldAccess.replace("$0.set" + fieldName + "($1);");
                        }
                    }});
        }
        cc.writeFile();
        Object obj = make(cc.getName());
    }

    public void testWhileTrueKO() throws Exception {
        final ClassPool pool = ClassPool.getDefault();
        final CtClass cc = pool.makeClass("test4.TestWhileTrueKO");
        String source = "public void testWhile() { while(true) { break; } }";
        cc.addMethod(CtMethod.make(source, cc));
        cc.writeFile();
        make(cc.getName());
    }

    public void testWhileTrueOK() throws Exception {
        final ClassPool pool = ClassPool.getDefault();
        final CtClass cc = pool.makeClass("test4.TestWhileTrueOK");
        String source = "public void testWhile() { while(0==0) { break; }}";
        cc.addMethod(CtMethod.make(source, cc));
        cc.writeFile();
        make(cc.getName());
    }

    // JIRA JASSIST-224
    public void testMethodParameters() throws Exception {
        Class rc = test4.MethodParamTest.class;
        java.lang.reflect.Method m = rc.getDeclaredMethods()[0];
        java.lang.reflect.Parameter[] params = m.getParameters();
        assertEquals("i", params[0].getName());
        assertEquals("s", params[1].getName());

        CtClass cc = sloader.get("test4.MethodParamTest");
        ClassFile cf = cc.getClassFile2();
        ConstPool cp = cf.getConstPool();
        MethodInfo minfo = cf.getMethod("test");
        MethodParametersAttribute attr
            = (MethodParametersAttribute)minfo.getAttribute(MethodParametersAttribute.tag);
        assertEquals(2, attr.size());
        assertEquals("i", cp.getUtf8Info(attr.name(0)));
        assertEquals("s", cp.getUtf8Info(attr.name(1)));
        assertEquals("i", attr.parameterName(0));
        assertEquals("s", attr.parameterName(1));

        attr = (MethodParametersAttribute)attr.copy(cp, null);
        assertEquals(2, attr.size());
        assertEquals("i", cp.getUtf8Info(attr.name(0)));
        assertEquals("s", cp.getUtf8Info(attr.name(1)));
        assertEquals("i", attr.parameterName(0));
        assertEquals("s", attr.parameterName(1));
    }

    // JIRA JASSIST-220
    public void testStaticInterfaceMethods() throws Exception {
        CtClass cc = sloader.get("test4.JIRA220");

        cc.getMethod("foo", "()V").instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                try {
                    m.getClassName();
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        });
    }

    // JIRA-230
    public void testDeadcode() throws Exception {
        CtClass newClass = sloader.makeClass("test4.TestDeadcode");
        addDeadCode(newClass, "public void evaluate(){if (false) {int i = 0;}}");
        addDeadCode(newClass, "public void evaluate2(){if (false == true) {int i = 0;}}");
        addDeadCode(newClass, "public void evaluate3(){if (true) {} else {} int i = 0; if (false) {} else {} i++; }");
        addDeadCode(newClass, "public void evaluate4(){ for (;false;); int i = false ? 1 : 0; while (true) { return; }}");
        addDeadCode(newClass, "public void evaluate5(){ boolean b = !false; b = false && b; b = true && true;"
                            + "  b = true || b; b = b || false; }");
        addDeadCode(newClass, "public boolean evaluate6(){ return !false; }");
        addDeadCode(newClass, "public boolean evaluate7(){ return !true; }");

        newClass.debugWriteFile();
        Class<?> cClass = newClass.toClass(test4.DefineClassCapability.class);
        Object o = cClass.getConstructor().newInstance();
        java.lang.reflect.Method m = cClass.getMethod("evaluate");
        m.invoke(o);
        m = cClass.getMethod("evaluate2");
        m.invoke(o);
        m = cClass.getMethod("evaluate3");
        m.invoke(o);
        m = cClass.getMethod("evaluate4");
        m.invoke(o);
        m = cClass.getMethod("evaluate6");
        assertTrue((boolean)m.invoke(o));
        m = cClass.getMethod("evaluate7");
        assertFalse((boolean)m.invoke(o));
        m = cClass.getMethod("evaluate5");
        m.invoke(o);
    }

    private void addDeadCode(CtClass cc, String meth) throws Exception {
        CtMethod m = CtNewMethod.make(meth, cc);
        cc.addMethod(m);
    }

    public void testAnnArg() throws Exception {
        CtClass cc = sloader.get("test4.AnnoArg");
        CtMethod m = cc.getDeclaredMethod("foo");
        test4.AnnoArg.AnnoArgAt a = (test4.AnnoArg.AnnoArgAt)m.getAnnotations()[0];
        assertEquals("test4.AnnoArg$B", a.value().getName());
        System.out.println(a.value().getName());
    }

    public void testDeclaredMethods() throws Exception {
        CtClass cc = sloader.get("test4.DeclMethodsList");
        CtMethod[] meth = cc.getDeclaredMethods("foo");
        assertEquals(2, meth.length);
        assertEquals("()V", meth[0].getSignature());
        assertEquals("(I)I", meth[1].getSignature());
        meth = cc.getDeclaredMethods("bar");
        assertEquals(1, meth.length);
        assertEquals("()V", meth[0].getSignature());
        meth = cc.getDeclaredMethods("baz");
        assertEquals(0, meth.length);
    }

    public void testAnnotationLoader() throws Exception {
        CtClass anno = sloader.makeAnnotation("test4.AnnoLoadAnno");
        anno.debugWriteFile();
        CtClass cc = sloader.get("test4.AnnoLoad");
        CtMethod m = cc.getDeclaredMethod("foo");
        ClassFile cf = cc.getClassFile();
        ConstPool cp = cf.getConstPool();
        AnnotationsAttribute attr
            = new AnnotationsAttribute(cp, AnnotationsAttribute.visibleTag);
        Annotation a = new Annotation(anno.getName(), cp);
        a.addMemberValue("value", new javassist.bytecode.annotation.StringMemberValue("file/path", cp));
        attr.setAnnotation(a);
        m.getMethodInfo().addAttribute(attr);
        cc.writeFile();
        anno.toClass(test4.DefineClassCapability.class);
        Class<?> rc = ((java.lang.annotation.Annotation)m.getAnnotations()[0]).annotationType();
        assertEquals(anno.getName(), rc.getName());
    }
}
