package javassist.bytecode;

import java.io.*;
import java.lang.reflect.Method;
import junit.framework.*;
import javassist.*;
import javassist.bytecode.annotation.*;

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
        return cloader.loadClass(name).newInstance();
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
            vec.addElement(new IntegerInfo(i));
            assertEquals(i, ((IntegerInfo)vec.elementAt(i)).value);
            assertEquals(i + 1, vec.size());
        }

        size = LongVector.ASIZE * LongVector.VSIZE * 3;
        vec = new LongVector(size - 5);
        assertEquals(size, vec.capacity());
        for (int i = 0; i < size; i++) {
            vec.addElement(new IntegerInfo(i));
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
