package javassist.bytecode;

import java.io.*;
import java.lang.reflect.Method;
import junit.framework.*;
import javassist.*;
import javassist.bytecode.annotation.*;
import javassist.bytecode.SignatureAttribute.*;

public class BytecodeTest extends TestCase {
    public static final String PATH = JvstTest.PATH;
    private ClassPool loader, dloader;
    private Loader cloader;

    public BytecodeTest(String name) {
         super(name);
    }

    protected void print(String msg) {
        System.out.println(msg);
    }

    protected void setUp() throws Exception {
        loader = ClassPool.getDefault();
        dloader = new ClassPool(null);
        dloader.appendSystemPath();
        dloader.insertClassPath(".");
        cloader = new Loader(dloader);
    }

    protected Object make(String name) throws Exception {
        return cloader.loadClass(name).getConstructor().newInstance();
    }

    protected int invoke(Object target, String method) throws Exception {
        Method m = target.getClass().getMethod(method, new Class[0]);
        Object res = m.invoke(target, new Object[0]);
        return ((Integer)res).intValue();
    }

    public void testByteVector() throws Exception {
        final int N = 257;
        Bytecode code = new Bytecode(null);
        for (int i = 0; i < N; i++) {
            code.add(i);
            assertEquals(i + 1, code.length());
            assertEquals((int)(byte)i, code.read(i));
            code.write(i, i + 1);
            assertEquals((int)(byte)(i + 1), code.read(i));
        }

        byte[] b = code.copy();
        assertEquals(N, b.length);
        for (int i = 0; i < N; i++)
            assertEquals((int)(byte)(i + 1), b[i]);


        code = new Bytecode(null);
        code.add(1);
        code.addGap(100);
        code.add(2);
        assertEquals(2, code.read(101));
    }

    public void testLongVector() throws Exception {
        LongVector vec = new LongVector();
        assertEquals(LongVector.ASIZE * LongVector.VSIZE, vec.capacity());
        int size = LongVector.ASIZE * LongVector.VSIZE * 3;
        for (int i = 0; i < size; i++) {
            vec.addElement(new IntegerInfo(i, i));
            assertEquals(i, ((IntegerInfo)vec.elementAt(i)).value);
            assertEquals(i + 1, vec.size());
        }

        size = LongVector.ASIZE * LongVector.VSIZE * 3;
        vec = new LongVector(size - 5);
        assertEquals(size, vec.capacity());
        for (int i = 0; i < size; i++) {
            vec.addElement(new IntegerInfo(i, i));
            assertEquals(i, ((IntegerInfo)vec.elementAt(i)).value);
            assertEquals(i + 1, vec.size());
        }
    }

    public void testClone() throws Exception {
        ConstPool cp = new ConstPool("test.CloneTest");
        Bytecode bc = new Bytecode(cp);
        bc.add(7);
        bc.add(11);
        Bytecode bc2 = (Bytecode)bc.clone();
        bc2.add(13);
        bc2.write(0, 17);
        assertEquals(7, bc.read(0));
        assertEquals(2, bc.length());
        assertEquals(3, bc2.length());
        assertEquals(cp, bc2.getConstPool());
        assertTrue(bc.getExceptionTable() != bc2.getExceptionTable());
    }

    public void test2byteLocalVar() throws Exception {
        CtClass cc = loader.makeClass("test.LocalVar2");
        CtMethod m = CtNewMethod.abstractMethod(CtClass.intType, "test",
                                                null, null, cc);
        Bytecode code = new Bytecode(cc.getClassFile().getConstPool(), 2, 300);
        code.addIconst(1);
        code.addIstore(255);
        code.addIload(255);
        code.addIstore(256);
        code.addIload(256);

        code.addLconst(1);
        code.addLstore(255);
        code.addLload(255);
        code.addLstore(256);
        code.addLload(256);

        code.addFconst(1.0f);
        code.addFstore(255);
        code.addFload(255);
        code.addFstore(256);
        code.addFload(256);

        code.addDconst(1.0);
        code.addDstore(255);
        code.addDload(255);
        code.addDstore(256);
        code.addDload(256);

        code.addOpcode(Opcode.ACONST_NULL);
        code.addAstore(255);
        code.addAload(255);
        code.addAstore(256);
        code.addAload(256);

        code.addIconst(1);
        code.addOpcode(Opcode.IRETURN);

        m.getMethodInfo().setCodeAttribute(code.toCodeAttribute());
        m.setModifiers(Modifier.PUBLIC);
        cc.addMethod(m);
        cc.writeFile();

        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "test"));
    }

    public void testBytecode() throws Exception {
        final int N = 64;
        Bytecode b = new Bytecode(null, 0, 0);
        try {
            b.write(3, 3);
            throw new Exception("out of range");
        }
        catch (ArrayIndexOutOfBoundsException e) {}

        try {
            b.read(3);
            throw new Exception("out of range");
        }
        catch (ArrayIndexOutOfBoundsException e) {}

        for (int i = 0; i < N * 3; ++i) {
            b.add(i % 100);
            assertEquals(i % 100, b.read(i));
        }

        for (int i = 0; i < N * 3; ++i)
            assertEquals(i % 100, b.read(i));

        for (int i = 0; i < N * 3; ++i) {
            b.write(i, i % 100);
            assertEquals(i % 100, b.read(i));
        }

        for (int i = 0; i < N * 3; ++i)
            assertEquals(i % 100, b.read(i));
    }

    public void testBytecode2() throws Exception {
        final int N = 64;
        Bytecode b = new Bytecode(null, 0, 0);

        for (int i = 0; i < N * 3 / 16; ++i) {
            b.addGap(16);
            assertEquals(16 * (i + 1), b.length());
        }

        b = new Bytecode(null, 0, 0);

        for (int i = 0; i < N * 3 / 10; ++i) {
            b.addGap(10);
            assertEquals(10 * (i + 1), b.length());
        }
    }

    public void testDescriptor() throws Exception {
        assertEquals("(II)", Descriptor.getParamDescriptor("(II)V"));
        assertEquals("()", Descriptor.getParamDescriptor("()I"));

        assertEquals(1, Descriptor.dataSize("I"));
        assertEquals(2, Descriptor.dataSize("D"));
        assertEquals(2, Descriptor.dataSize("J"));
        assertEquals(1, Descriptor.dataSize("[J"));
        assertEquals(1, Descriptor.dataSize("[[D"));
        assertEquals(1, Descriptor.dataSize("LD;"));

        assertEquals(-1, Descriptor.dataSize("(I)V"));
        assertEquals(0, Descriptor.dataSize("(D)J"));
        assertEquals(0, Descriptor.dataSize("()V"));
        assertEquals(1, Descriptor.dataSize("()I"));
        assertEquals(-1, Descriptor.dataSize("([DLA;)I"));
        assertEquals(-3, Descriptor.dataSize("(BIJ)LA;"));
        assertEquals(-3, Descriptor.dataSize("(BIJ)[D"));

        boolean ok = false;
        try {
            Descriptor.dataSize("(Ljava/lang/String)I");
        }
        catch (IndexOutOfBoundsException e) {
            print("testDescriptor(): dataSize() " + e);
            ok = true;
        }
        assertTrue(ok);

        ok = false;
        try {
            Descriptor.numOfParameters("([DLjava/lang/String)I");
        }
        catch (IndexOutOfBoundsException e) {
            print("testDescriptor(): numOfParameters() " + e);
            ok = true;
        }
        assertTrue(ok);
    }

    public void testDescriptor2() throws Exception {
        assertEquals("int", Descriptor.toClassName("I"));
        assertEquals("double[]", Descriptor.toClassName("[D"));
        assertEquals("boolean[][]", Descriptor.toClassName("[[Z"));
        assertEquals("java.lang.String",
                Descriptor.toClassName("Ljava/lang/String;"));
        assertEquals("java.lang.String[]",
                Descriptor.toClassName("[Ljava/lang/String;"));
        try {
            assertEquals("Foo", Descriptor.toClassName("LFoo;;"));
            fail("LFoo;;");
        }
        catch (RuntimeException e) {}
        try {
            assertEquals("int", Descriptor.toClassName("II"));
            fail("II");
        }
        catch (RuntimeException e) {}
    }

    public void testLineNumberAttribute() throws Exception {
        CtClass cc = loader.get("test1.LineNumber");
        CtMethod m = cc.getDeclaredMethod("sort");
        MethodInfo minfo = m.getMethodInfo();
        CodeAttribute ca = minfo.getCodeAttribute();
        LineNumberAttribute ainfo
            = (LineNumberAttribute)ca.getAttribute(LineNumberAttribute.tag);

        int n = ainfo.tableLength();
        for (int i = 0; i < n; ++i)
            print("Line " + ainfo.lineNumber(i) + " at " + ainfo.startPc(i));

        print("find Line 10: " + ainfo.toStartPc(10));
        print("find PC 30: " + ainfo.toLineNumber(30));

        LineNumberAttribute.Pc pc = ainfo.toNearPc(6);
        print("line 6: " + pc.index);
        assertEquals(8, pc.line);

        pc = ainfo.toNearPc(7);
        print("line 7: " + pc.index);
        assertEquals(8, pc.line);

        pc = ainfo.toNearPc(8);
        print("line 8: " + pc.index);
        assertEquals(8, pc.line);

        pc = ainfo.toNearPc(9);
        print("line 9: " + pc.index);
        assertEquals(9, pc.line);

        pc = ainfo.toNearPc(15);
        print("line 15: " + pc.index);
        assertEquals(17, pc.line);

        pc = ainfo.toNearPc(19);
        print("line 19: " + pc.index);
        assertEquals(20, pc.line);

        pc = ainfo.toNearPc(21);
        print("line 20: " + pc.index);
        assertEquals(20, pc.line);

        pc = ainfo.toNearPc(22);
        print("line 21: " + pc.index);
        assertEquals(20, pc.line);
    }

    public void testRenameClass() throws Exception {
        CtClass cc = loader.get("test1.RenameClass");
        cc.replaceClassName("test1.RenameClass2", "java.lang.String");
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(0, invoke(obj, "test"));
    }

    public void testDeprecatedAttribute() throws Exception {
        CtClass cc = loader.get("java.lang.Thread");
        CtMethod m = cc.getDeclaredMethod("suspend");
        MethodInfo minfo = m.getMethodInfo();
        DeprecatedAttribute ainfo
            = (DeprecatedAttribute)minfo.getAttribute(DeprecatedAttribute.tag);
        assertTrue(ainfo != null);
        m = cc.getDeclaredMethod("toString");
        minfo = m.getMethodInfo();
        ainfo
            = (DeprecatedAttribute)minfo.getAttribute(DeprecatedAttribute.tag);
        assertTrue(ainfo == null);
    }

    public void testLocalVarAttribute() throws Exception {
        CtClass cc = loader.get("test1.LocalVars");
        CtMethod m = cc.getDeclaredMethod("foo");
        MethodInfo minfo = m.getMethodInfo();
        CodeAttribute ca = minfo.getCodeAttribute();
        LocalVariableAttribute ainfo
            = (LocalVariableAttribute)ca.getAttribute(
                                            LocalVariableAttribute.tag);
        assertTrue(ainfo != null);

        CtClass cc2 = loader.makeClass("test1.LocalVars2");
        CtMethod m2 = new CtMethod(m, cc2, null);
        CodeAttribute ca2 = m2.getMethodInfo().getCodeAttribute();
        ConstPool cp2 = ca2.getConstPool();
        LocalVariableAttribute ainfo2
            = (LocalVariableAttribute)ainfo.copy(cp2, null);
        ca2.getAttributes().add(ainfo2);
        cc2.addMethod(m2);
        cc2.writeFile();
        print("**** local variable table ***");
        for (int i = 0; i < ainfo2.tableLength(); i++) {
            String msg = ainfo2.startPc(i) + " " + ainfo2.codeLength(i)
                + " " + ainfo2.variableName(i) + " "
                + ainfo2.descriptor(i)
                + " " + ainfo2.index(i);
            print(msg);
            if (ainfo2.variableName(i).equals("j"))
                assertEquals("I", ainfo2.descriptor(i));
        }
        print("**** end ***");
    }

    public void testAnnotations() throws Exception {
        String fname = PATH + "annotation/Test.class";
        BufferedInputStream fin
            = new BufferedInputStream(new FileInputStream(fname));
        ClassFile cf = new ClassFile(new DataInputStream(fin));
        AnnotationsAttribute attr = (AnnotationsAttribute)
            cf.getAttribute(AnnotationsAttribute.invisibleTag);
        String sig = attr.toString();
        System.out.println(sig);

        ClassFile cf2 = new ClassFile(false, "test1.AnnoTest",
                                      "java.lang.Object");
        cf2.addAttribute(attr.copy(cf2.getConstPool(), null));
        AnnotationsAttribute attr2 = (AnnotationsAttribute)
            cf2.getAttribute(AnnotationsAttribute.invisibleTag);

        DataOutputStream out
            = new DataOutputStream(new FileOutputStream("test1/AnnoTest.class"));
        cf2.write(out);

        assertEquals(sig, attr2.toString());
    }

    public void testAnnotations2() throws Exception {
        ClassFile cf = new ClassFile(false, "test1.AnnoTest2",
                                     "java.lang.Object");
        AnnotationsAttribute anno
            = new AnnotationsAttribute(cf.getConstPool(),
                                       AnnotationsAttribute.invisibleTag);
        ConstPool cp = cf.getConstPool();
        Annotation a = new Annotation("Anno", cp);
        StringMemberValue smv = new StringMemberValue("foo", cp);
        a.addMemberValue("name", smv);
        anno.setAnnotation(a);
        cf.addAttribute(anno);

        String fname = "test1/AnnoTest2.class";
        DataOutputStream out
            = new DataOutputStream(new FileOutputStream(fname));
        cf.write(out);

        BufferedInputStream fin
            = new BufferedInputStream(new FileInputStream(fname));
        cf = new ClassFile(new DataInputStream(fin));
        AnnotationsAttribute attr = (AnnotationsAttribute)
            cf.getAttribute(AnnotationsAttribute.invisibleTag);

        String sig = attr.toString();
        System.out.println(sig);
        assertEquals("@Anno(name=\"foo\")", sig);
    }

    public void testAddClassInfo() throws Exception {
        CtClass cc = loader.get("test1.AddClassInfo");
        ClassFile cf = cc.getClassFile();
        ConstPool cp = cf.getConstPool();
        int i = cp.addClassInfo("test1.AddClassInfo");
        assertEquals(i, cp.getThisClassInfo());

        cc.addMethod(CtNewMethod.make("public int bar() { return foo(); }", cc));
        cc.writeFile();
        Object obj = make(cc.getName());
        assertEquals(1, invoke(obj, "bar"));
    }

    public void testRename() throws Exception {
        ConstPool cp = new ConstPool("test1.Foo");
        int i = cp.addClassInfo("test1.Bar");
        assertEquals(i, cp.addClassInfo("test1.Bar"));
        cp.renameClass("test1/Bar", "test1/Bar2");
        assertEquals("test1.Bar2", cp.getClassInfo(i));
        assertEquals(i, cp.addClassInfo("test1.Bar2"));
        int j = cp.addClassInfo("test1.Bar");
        assertTrue(i != j);
        assertEquals(j, cp.addClassInfo("test1.Bar"));
    }

    public void testSignature() throws Exception {
        parseMsig("(TT;)TT;", "<> (T) T");
        parseMsig("<S:Ljava/lang/Object;>(TS;[TS;)TT;",
                  "<S extends java.lang.Object> (S, S[]) T");
        parseMsig("()TT;^TT;", "<> () T throws T");
        String sig = "<T:Ljava/lang/Exception;>LPoi$Foo<Ljava/lang/String;>;LBar;LBar2;";
        String rep = "<T extends java.lang.Exception> extends Poi.Foo<java.lang.String> implements Bar, Bar2";
        SignatureAttribute.ClassSignature cs = SignatureAttribute.toClassSignature(sig);
        assertEquals(rep, cs.toString());
        CtClass c = loader.get("test3.SigAttribute");
        CtField f = c.getDeclaredField("value");
        SignatureAttribute a = (SignatureAttribute)f.getFieldInfo2().getAttribute(SignatureAttribute.tag);
        assertNotNull(a);
        f.getFieldInfo().prune(new ConstPool("test3.SigAttribute"));
        a = (SignatureAttribute)f.getFieldInfo2().getAttribute(SignatureAttribute.tag);
        assertNotNull(a);
    }

    private void parseMsig(String sig, String rep) throws Exception {
        SignatureAttribute.MethodSignature ms = SignatureAttribute.toMethodSignature(sig);
        assertEquals(rep, ms.toString());
    }

    public void testSignatureChange() throws Exception {
        changeMsig("<S:Ljava/lang/Object;>(TS;[TS;)Ljava/lang/Object", "java/lang/Object",
                   "<S:Ljava/lang/Objec;>(TS;[TS;)Ljava/lang/Object", "java/lang/Objec"); 
        changeMsig("<S:Ljava/lang/Object;>(TS;[TS;)TT;", "java/lang/Object",
                   "<S:Ljava/lang/Objec;>(TS;[TS;)TT;", "java/lang/Objec"); 
        changeMsig("<S:Ljava/lang/Object;>(TS;[TS;)Ljava/lang/Object2;", "java/lang/Object",
                   "<S:Ljava/lang/Objec;>(TS;[TS;)Ljava/lang/Object2;", "java/lang/Objec"); 
        changeMsig("<S:Ljava/lang/Object;>(TS;[TS;)Ljava/lang/Objec;", "java/lang/Object",
                   "<S:Ljava/lang/Object2;>(TS;[TS;)Ljava/lang/Objec;", "java/lang/Object2"); 
        changeMsig2("<S:Ljava/lang/Object;>(TS;[TS;)TT;", "java/lang/Object",
                    "<S:Ljava/lang/Objec;>(TS;[TS;)TT;", "java/lang/Objec"); 
        changeMsig2("<S:Ljava/lang/Object;>(TS;[TS;)Ljava/lang/Object2;", "java/lang/Object",
                    "<S:Ljava/lang/Objec;>(TS;[TS;)Ljava/lang/Object2;", "java/lang/Objec"); 
        changeMsig2("<S:Ljava/lang/Object;>(TS;[TS;)Ljava/lang/Objec;", "java/lang/Object",
                    "<S:Ljava/lang/Object2;>(TS;[TS;)Ljava/lang/Objec;", "java/lang/Object2"); 
        String sig = "<T:Ljava/lang/Exception;>LPoi$Foo<Ljava/lang/String;>;LBar;LBar2;";
        //String res = "<T:Ljava/lang/Exception;>LPoi$Foo<Ljava/lang/String2;>;LBar;LBar2;";
        changeMsig(sig, "java/lang/String", sig, "java/lang/String2");
        changeMsig2(sig, "java/lang/String", sig, "java/lang/String2");
        changeMsig("Ltest<TE;>.List;", "ist", "Ltest<TE;>.List;", "IST");
    }

    private void changeMsig(String old, String oldname, String result, String newname) {
        String r = SignatureAttribute.renameClass(old, oldname, newname);
        assertEquals(result, r);
    }

    private void changeMsig2(String old, String oldname, String result, String newname) {
        ClassMap map = new ClassMap();
        map.put(oldname, newname);
        String r = SignatureAttribute.renameClass(old, map);
        assertEquals(result, r);
    }

    public void testSignatureEncode() throws Exception {
        BaseType bt = new BaseType("int");
        TypeVariable tv = new TypeVariable("S");
        ArrayType at = new ArrayType(1, tv);
        ClassType ct1 = new ClassType("test.Foo");
        TypeArgument ta = new TypeArgument();
        TypeArgument ta2 = new TypeArgument(ct1);
        TypeArgument ta3 = TypeArgument.subclassOf(ct1);
        ClassType ct2 = new ClassType("test.Foo", new TypeArgument[] { ta, ta2, ta3 });
        ClassType ct3 = new ClassType("test.Bar");
        ClassType ct4 = new ClassType("test.Bar", new TypeArgument[] { ta });
        NestedClassType ct5 = new NestedClassType(ct4, "Baz", new TypeArgument[] { ta });
        TypeParameter tp1 = new TypeParameter("U");
        TypeParameter tp2 = new TypeParameter("V", ct1, new ObjectType[] { ct3 });
        ClassSignature cs = new ClassSignature(new TypeParameter[] { tp1 },
                                               ct1,
                                               new ClassType[] { ct2 });
        MethodSignature ms = new MethodSignature(new TypeParameter[] { tp1, tp2 },
                                                 new Type[] { bt, at, ct5 }, ct3,
                                                 new ObjectType[] { ct1, tv });

        assertEquals("<U:Ljava/lang/Object;>Ltest/Foo;Ltest/Foo<*Ltest/Foo;+Ltest/Foo;>;",
                     cs.encode());
        assertEquals("<U:Ljava/lang/Object;V:Ltest/Foo;:Ltest/Bar;>(I[TS;Ltest/Bar<*>$Baz<*>;)Ltest/Bar;^Ltest/Foo;^TS;",
                     ms.encode());
    }

    public void testModifiers() throws Exception {
        CtClass c = loader.get("test3.Mods");
        c.setModifiers(Modifier.PROTECTED);
        assertEquals(AccessFlag.PROTECTED | AccessFlag.SUPER, c.getClassFile2().getAccessFlags());
        CtClass c2 = loader.get("test3.Mods2");
        c2.setModifiers(Modifier.PUBLIC | c2.getModifiers());
        assertEquals(AccessFlag.PUBLIC | AccessFlag.INTERFACE | AccessFlag.ABSTRACT,
                     c2.getClassFile2().getAccessFlags());

        ClassFile cf = new ClassFile(false, "Test", null);
        assertEquals(AccessFlag.SUPER, cf.getAccessFlags());
        ClassFile cf2 = new ClassFile(true, "Test2", null);
        assertEquals(AccessFlag.INTERFACE | AccessFlag.ABSTRACT, cf2.getAccessFlags());
    }

    public void testByteStream() throws Exception {
        ByteStream bs = new ByteStream(16);
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(ba);
        for (int i = 0; i < 100; i++) {
            bs.write(i);
            dos.write(i);
            bs.writeShort(i + 1);
            dos.writeShort(i + 1);
            bs.writeInt(i + 2);
            dos.writeInt(i + 2);
            bs.writeLong(i + 3);
            dos.writeLong(i + 3);
        }

        bs.writeLong(Long.MAX_VALUE);
        dos.writeLong(Long.MAX_VALUE);
        bs.writeFloat(Float.MAX_VALUE);
        dos.writeFloat(Float.MAX_VALUE);
        bs.writeDouble(Double.MAX_VALUE);
        dos.writeDouble(Double.MAX_VALUE);
        compare(bs, ba);
    }

    public void testByteStreamUtf() throws Exception {
        ByteStream bs = new ByteStream(4);
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(ba);
        char c2 = '\u00b4';
        char c3 = '\u3007';
        bs.writeUTF("abc");
        dos.writeUTF("abc");
        String s = "" + c2 + c2;
        bs.writeUTF(s);
        dos.writeUTF(s);

        s = "" + c3 + c3;
        bs.writeUTF(s);
        dos.writeUTF(s);

        s = "abcdefgh" + c2 + "123" + c3 + "456";
        bs.writeUTF(s);
        dos.writeUTF(s);

        compare(bs, ba);
    }

    private void compare(ByteStream bs, ByteArrayOutputStream bos) {
        byte[] bs2 = bs.toByteArray();
        byte[] bos2 = bos.toByteArray();
        assertEquals(bs2.length, bos2.length);
        for (int i = 0; i < bs2.length; i++)
            assertEquals(bs2[i], bos2[i]);
    }

    public void testConstInfos() throws Exception {
        int n = 1;
        Utf8Info ui1 = new Utf8Info("test", n++);
        Utf8Info ui2 = new Utf8Info("te" + "st", n++);
        Utf8Info ui3 = new Utf8Info("test2", n++);
        assertTrue(ui1.hashCode() == ui2.hashCode());
        assertTrue(ui1.equals(ui1));
        assertTrue(ui1.equals(ui2));
        assertFalse(ui1.equals(ui3));
        assertFalse(ui1.equals(null));

        ClassInfo ci1 = new ClassInfo(ui1.index, n++);
        ClassInfo ci2 = new ClassInfo(ui1.index, n++);
        ClassInfo ci3 = new ClassInfo(ui2.index, n++);
        ClassInfo ci4 = new ClassInfo(ui3.index, n++);
        assertTrue(ci1.hashCode() == ci2.hashCode());
        assertTrue(ci1.equals(ci1));
        assertTrue(ci1.equals(ci2));
        assertFalse(ci1.equals(ci3));
        assertFalse(ci1.equals(ci4));
        assertFalse(ci1.equals(ui1));
        assertFalse(ci1.equals(null));

        NameAndTypeInfo ni1 = new NameAndTypeInfo(ui1.index, ui3.index, n++);
        NameAndTypeInfo ni2 = new NameAndTypeInfo(ui1.index, ui3.index, n++);
        NameAndTypeInfo ni3 = new NameAndTypeInfo(ui1.index, ui1.index, n++);
        NameAndTypeInfo ni4 = new NameAndTypeInfo(ui3.index, ui3.index, n++);
        assertTrue(ni1.hashCode() == ni2.hashCode());
        assertTrue(ni1.equals(ni1));
        assertTrue(ni1.equals(ni2));
        assertFalse(ni1.equals(ni3));
        assertFalse(ni1.equals(ni4));
        assertFalse(ni1.equals(ci1));
        assertFalse(ni1.equals(null));

        MethodrefInfo mi1 = new MethodrefInfo(ui1.index, ui3.index, n++);
        MethodrefInfo mi2 = new MethodrefInfo(ui1.index, ui3.index, n++);
        MethodrefInfo mi3 = new MethodrefInfo(ui1.index, ui1.index, n++);
        MethodrefInfo mi4 = new MethodrefInfo(ui2.index, ui3.index, n++);
        assertTrue(mi1.hashCode() == mi2.hashCode());
        assertTrue(mi1.equals(mi1));
        assertTrue(mi1.equals(mi2));
        assertFalse(mi1.equals(mi3));
        assertFalse(mi1.equals(mi4));
        assertFalse(mi1.equals(ci1));
        assertFalse(mi1.equals(null));

        FieldrefInfo field1 = new FieldrefInfo(ui1.index, ui3.index, n++);
        FieldrefInfo field2 = new FieldrefInfo(ui1.index, ui1.index, n++);
        FieldrefInfo field3 = new FieldrefInfo(ui1.index, ui1.index, n++);
        InterfaceMethodrefInfo intf1 = new InterfaceMethodrefInfo(ui1.index, ui3.index, n++);
        InterfaceMethodrefInfo intf2 = new InterfaceMethodrefInfo(ui1.index, ui3.index, n++);
        assertFalse(mi1.equals(field1));
        assertFalse(field1.equals(mi1));
        assertTrue(field2.equals(field3));
        assertFalse(mi1.equals(field2));
        assertFalse(mi1.equals(intf1));
        assertFalse(intf1.equals(mi1));
        assertTrue(intf1.equals(intf2));

        StringInfo si1 = new StringInfo(ui1.index, n++);
        StringInfo si2 = new StringInfo(ui1.index, n++);
        StringInfo si3 = new StringInfo(ui2.index, n++);
        assertTrue(si1.hashCode() == si2.hashCode());
        assertTrue(si1.equals(si1));
        assertTrue(si1.equals(si2));
        assertFalse(si1.equals(si3));
        assertFalse(si1.equals(ci1));
        assertFalse(si1.equals(null));

        IntegerInfo ii1 = new IntegerInfo(12345, n++);
        IntegerInfo ii2 = new IntegerInfo(12345, n++);
        IntegerInfo ii3 = new IntegerInfo(-12345, n++);
        assertTrue(ii1.hashCode() == ii2.hashCode());
        assertTrue(ii1.equals(ii1));
        assertTrue(ii1.equals(ii2));
        assertFalse(ii1.equals(ii3));
        assertFalse(ii1.equals(ci1));
        assertFalse(ii1.equals(null));

        FloatInfo fi1 = new FloatInfo(12345.0F, n++);
        FloatInfo fi2 = new FloatInfo(12345.0F, n++);
        FloatInfo fi3 = new FloatInfo(-12345.0F, n++);
        assertTrue(fi1.hashCode() == fi2.hashCode());
        assertTrue(fi1.equals(fi1));
        assertTrue(fi1.equals(fi2));
        assertFalse(fi1.equals(fi3));
        assertFalse(fi1.equals(ci1));
        assertFalse(fi1.equals(null));
       
        LongInfo li1 = new LongInfo(12345L, n++);
        LongInfo li2 = new LongInfo(12345L, n++);
        LongInfo li3 = new LongInfo(-12345L, n++);
        assertTrue(li1.hashCode() == li2.hashCode());
        assertTrue(li1.equals(li1));
        assertTrue(li1.equals(li2));
        assertFalse(li1.equals(li3));
        assertFalse(li1.equals(ci1));
        assertFalse(li1.equals(null));

        DoubleInfo di1 = new DoubleInfo(12345.0, n++);
        DoubleInfo di2 = new DoubleInfo(12345.0, n++);
        DoubleInfo di3 = new DoubleInfo(-12345.0, n++);
        assertTrue(di1.hashCode() == di2.hashCode());
        assertTrue(di1.equals(di1));
        assertTrue(di1.equals(di2));
        assertFalse(di1.equals(di3));
        assertFalse(di1.equals(ci1));
        assertFalse(di1.equals(null));
    }

    public void testConstInfoAdd() {
        ConstPool cp = new ConstPool("test.Tester");
        assertEquals("test.Tester", cp.getClassName());
        int n0 = cp.addClassInfo("test.Foo");
        assertEquals(n0, cp.addClassInfo("test.Foo"));
        int n1 = cp.addUtf8Info("test.Bar");
        assertEquals(n1, cp.addUtf8Info("test.Bar"));
        int n2 = cp.addUtf8Info("()V");
        assertEquals(n2, cp.addUtf8Info("()V"));
        assertTrue(n1 != n2);
        int n3 = cp.addNameAndTypeInfo(n1, n2);
        assertEquals(n3, cp.addNameAndTypeInfo(n1, n2));
        assertEquals(n3, cp.addNameAndTypeInfo("test.Bar", "()V"));
        int n4 = cp.addNameAndTypeInfo("test.Baz", "()V");
        assertTrue(n3 != n4);
        assertTrue(n3 != cp.addNameAndTypeInfo(cp.addUtf8Info("test.Baz"), n2));
        int n5 = cp.addFieldrefInfo(n0, n3);
        assertEquals(n5, cp.addFieldrefInfo(n0, n3));
        assertTrue(n5 != cp.addFieldrefInfo(n0, n4));
        assertTrue(cp.addMethodrefInfo(n0, n3) == cp.addMethodrefInfo(n0, n3));
        assertTrue(cp.addMethodrefInfo(n0, "test", "()B") == cp.addMethodrefInfo(n0, "test", "()B"));
        assertTrue(cp.addMethodrefInfo(n0, "test", "()B") != cp.addMethodrefInfo(n0, "test", "()I"));
        assertTrue(n5 != cp.addInterfaceMethodrefInfo(n0, n3));
        assertTrue(cp.addInterfaceMethodrefInfo(n0, "test", "()B")
                   == cp.addInterfaceMethodrefInfo(n0, "test", "()B"));
        assertTrue(cp.addInterfaceMethodrefInfo(n0, "test", "()B")
                   != cp.addInterfaceMethodrefInfo(n0, "test", "()I"));
        int n6 = cp.addStringInfo("foobar");
        assertEquals(n6, cp.addStringInfo("foobar"));
        assertTrue(n6 != cp.addStringInfo("foobar2"));
        int n7 = cp.addIntegerInfo(123);
        assertEquals(n7, cp.addIntegerInfo(123));
        assertTrue(n7 != cp.addIntegerInfo(-123));
        int n8 = cp.addFloatInfo(123);
        assertEquals(n8, cp.addFloatInfo(123.0F));
        assertTrue(n8 != cp.addFloatInfo(-123.0F));
        int n9 = cp.addLongInfo(1234L);
        assertEquals(n9, cp.addLongInfo(1234L));
        assertTrue(n9 != cp.addLongInfo(-1234L));
        int n10 = cp.addDoubleInfo(1234.0);
        assertEquals(n10, cp.addDoubleInfo(1234.0));
        assertTrue(n10 != cp.addDoubleInfo(-1234.0));

        cp.prune();
        assertEquals(n1, cp.addUtf8Info("test.Bar"));
        assertEquals(n0, cp.addClassInfo("test.Foo"));
        assertEquals(n10, cp.addDoubleInfo(1234.0));
    }

    public void testRenameInConstPool() {
        ConstPool cp = new ConstPool("test.Tester");
        int n1 = cp.addClassInfo("test.Foo");
        int n2 = cp.addClassInfo("test.Bar");
        int n3 = cp.addClassInfo("test.Baz");
        int n4 = cp.addNameAndTypeInfo("foo", "(Ltest/Foo;)V");
        int n5 = cp.addNameAndTypeInfo("bar", "(Ltest/Bar;)V");
        int n6 = cp.addNameAndTypeInfo("baz", "(Ltest/Baz;)V");
        int n7 = cp.addClassInfo("[Ltest/Foo;");
        int n8 = cp.addClassInfo("[Ltest/Bar;");

        cp.renameClass("test/Foo", "test/Foo2");
        assertEquals("test.Foo2", cp.getClassInfo(n1));
        assertEquals("(Ltest/Foo2;)V", cp.getUtf8Info(cp.getNameAndTypeDescriptor(n4)));
        assertTrue(cp.addClassInfo("test.Foo2") == n1);
        assertTrue(cp.addClassInfo("test.Foo") != n1);
        assertTrue(cp.addNameAndTypeInfo("foo", "(Ltest/Foo2;)V") == n4);
        assertTrue(cp.addNameAndTypeInfo("foo", "(Ltest/Foo;)V") != n4);
        assertEquals("[Ltest.Foo2;", cp.getClassInfo(n7));

        ClassMap map = new ClassMap();
        map.put("test.Bar", "test.Bar2");
        map.put("test.Baz", "test.Baz2");
        cp.renameClass(map);
        assertEquals("test.Bar2", cp.getClassInfo(n2));
        assertEquals("(Ltest/Bar2;)V", cp.getUtf8Info(cp.getNameAndTypeDescriptor(n5)));
        assertTrue(cp.addClassInfo("test.Bar2") == n2);
        assertTrue(cp.addClassInfo("test.Bar") != n2);
        assertTrue(cp.addNameAndTypeInfo("bar", "(Ltest/Bar2;)V") == n5);
        assertTrue(cp.addNameAndTypeInfo("bar", "(Ltest/Bar;)V") != n5);
        assertEquals("[Ltest.Bar2;", cp.getClassInfo(n8));
    }

    public void testInvokeDynamic() throws Exception {
        CtClass cc = loader.get("test4.InvokeDyn");
        ClassFile cf = cc.getClassFile();
        ConstPool cp = cf.getConstPool();

        Bytecode code = new Bytecode(cp, 0, 1);
        code.addAload(0);
        code.addIconst(9);
        code.addLdc("nine");
        code.addInvokedynamic(0, "call", "(ILjava/lang/String;)I");
        code.addOpcode(Opcode.SWAP);
        code.addOpcode(Opcode.POP);
        code.addOpcode(Opcode.IRETURN);

        MethodInfo minfo = new MethodInfo(cp, "test", "()I");
        minfo.setCodeAttribute(code.toCodeAttribute());
        minfo.setAccessFlags(AccessFlag.PUBLIC);
        minfo.rebuildStackMapIf6(loader, cf);
        cf.addMethod(minfo);

        cf.addMethod(new MethodInfo(cp, "test2", minfo, null));
        int mtIndex = cp.addMethodTypeInfo(cp.addUtf8Info("(I)V"));

        String desc
            = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)" +
              "Ljava/lang/invoke/CallSite;";
        int mri = cp.addMethodrefInfo(cp.addClassInfo(cc.getName()), "boot", desc);
        int mhi = cp.addMethodHandleInfo(ConstPool.REF_invokeStatic, mri);
        int[] args = new int[0];
        BootstrapMethodsAttribute.BootstrapMethod[] bms
            = new BootstrapMethodsAttribute.BootstrapMethod[1];
        bms[0] = new BootstrapMethodsAttribute.BootstrapMethod(mhi, args);

        cf.addAttribute(new BootstrapMethodsAttribute(cp, bms));
        cc.writeFile();

        Object obj = make(cc.getName());
        assertEquals(9, invoke(obj, "test"));

        ClassPool cp2 = new ClassPool();
        cp2.appendClassPath(".");
        CtClass cc2 = cp2.get(cc.getName());
        assertEquals("test4.InvokeDyn", cc2.getClassFile().getName());
        ConstPool cPool2 = cc2.getClassFile().getConstPool();
        assertEquals("(I)V", cPool2.getUtf8Info(cPool2.getMethodTypeInfo(mtIndex)));
    }

    public static void main(String[] args) {
        // junit.textui.TestRunner.run(suite());
        junit.awtui.TestRunner.main(new String[] {
            "javassist.bytecode.BytecodeTest" });
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Bytecode Tests");
        suite.addTestSuite(BytecodeTest.class);
        return suite;
    }
}
