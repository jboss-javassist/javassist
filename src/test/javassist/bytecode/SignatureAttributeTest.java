package javassist.bytecode;

import junit.framework.TestCase;
import java.util.HashMap;

public class SignatureAttributeTest extends TestCase {
	public void test1() {
		final String signature = "TX;TY;La/b/C$D$E$J$K;"; //a sequence of three ReferenceTypeSignature
		final HashMap<String, String> map = new HashMap<>();
		map.put("a/b/C$D$E$J$K", "o/p/Q$R$S$T$U");
		map.put("e/F$G$H$I", "v/W$X$Y$Z");
		final String signatureRenamed = SignatureAttribute.renameClass(signature, map);
		assertEquals("TX;TY;Lo/p/Q$R$S$T$U;", signatureRenamed);
	}

	public void test2() {
		final String signature = "La/b/C<TA;TB;>.D<Ljava/lang/Integer;>;"; //a ClassTypeSignature
		final HashMap<String, String> map = new HashMap<>();
		map.put("a/b/C$D", "o/p/Q$R");
		map.put("java/lang/Integer", "java/lang/Long");
		final String signatureRenamed = SignatureAttribute.renameClass(signature, map);
		assertEquals("Lo/p/Q<TA;TB;>.R<Ljava/lang/Long;>;", signatureRenamed);
	}
	
	public void test3() {
		final String signature = "BJLB<TX;Lc/D$E;>.F<TY;>;TZ;"; //a sequence of four JavaTypeSignature
		final HashMap<String, String> map = new HashMap<>();
		map.put("B$F", "P$T");
		map.put("c/D$E", "q/R$S");
		final String signatureRenamed = SignatureAttribute.renameClass(signature, map);
		assertEquals("BJLP<TX;Lq/R$S;>.T<TY;>;TZ;", signatureRenamed);
	}
	
	public void test4() {
		final String signature = "La/b/C<TX;>;[[Ld/E<+TY;-Ljava/lang/Object;*>;Z"; //a sequence of three JavaTypeSignature
		final HashMap<String, String> map = new HashMap<>();
		map.put("java/lang/Object", "java/util/Map");
		map.put("d/E", "F");
		final String signatureRenamed = SignatureAttribute.renameClass(signature, map);
		assertEquals("La/b/C<TX;>;[[LF<+TY;-Ljava/util/Map;*>;Z", signatureRenamed);
	}
	
	public void test5() {
		final String signature = "La/b/C$D$E<TX;Le/F$G<TY;TZ;>.H$I<TU;TV;>;>.J$K;"; //a ClassTypeSignature
		final HashMap<String, String> map = new HashMap<>();
		map.put("a/b/C$D$E$J$K", "o/p/Q$R$S$T$U");
		map.put("e/F$G$H$I", "v/W$X$Y$Z");
		final String signatureRenamed = SignatureAttribute.renameClass(signature, map);
		assertEquals("Lo/p/Q$R$S<TX;Lv/W$X<TY;TZ;>.Y$Z<TU;TV;>;>.T$U;", signatureRenamed);
	}
	
	public void test6() {
		final String signature = "<X:La/B$C<TY;>.D<TZ;>;:Le/F$G;>Lh/I$J;"; //a ClassSignature
		final HashMap<String, String> map = new HashMap<>();
		map.put("a/B$C$D", "o/P$Q$R");
		map.put("e/F$G", "s/T$U");
		map.put("h/I$J", "v/W$X");
		final String signatureRenamed = SignatureAttribute.renameClass(signature, map);
		assertEquals("<X:Lo/P$Q<TY;>.R<TZ;>;:Ls/T$U;>Lv/W$X;", signatureRenamed);
	}
	
	public void test7() {
		final String signature = "<A:La/B$C;:Ld/E<TX;>.F<TY;>;:TZ;B:Ljava/lang/Thread;>(LX;TA;LA;)V^Ljava/lang/Exception;"; //a MethodSignature
		final HashMap<String, String> map = new HashMap<>();
		map.put("A", "P");
		map.put("a/B$C", "s/T$U");
		map.put("d/E$F", "v/W$X");
		map.put("X", "V");
		map.put("java/lang/Exception", "java/lang/RuntimeException");
		final String signatureRenamed = SignatureAttribute.renameClass(signature, map);
		assertEquals("<A:Ls/T$U;:Lv/W<TX;>.X<TY;>;:TZ;B:Ljava/lang/Thread;>(LV;TA;LP;)V^Ljava/lang/RuntimeException;", signatureRenamed);
	}
	
	public static void main(String[] s) {
		new SignatureAttributeTest().test1();
		new SignatureAttributeTest().test2();
		new SignatureAttributeTest().test3();
		new SignatureAttributeTest().test4();
		new SignatureAttributeTest().test5();
		new SignatureAttributeTest().test6();
		new SignatureAttributeTest().test7();
	}
}
