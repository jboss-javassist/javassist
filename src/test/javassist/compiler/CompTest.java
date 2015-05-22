package javassist.compiler;

import junit.framework.*;
import javassist.*;
import java.util.*;
import javassist.compiler.ast.*;

/*
public class Test{

  public static void main(String[] args) throws Exception {
    ClassPool pool = ClassPool.getDefault();
    CtClass cc = pool.get("Print");
    CtMethod cm = cc.getDeclaredMethod("print");
    cm.insertBefore("{((Advice)(new
    HelloAspect().getAdvice().get(0))).print();}");
    //cm.insertBefore("{new Advice(\"advice\").print();}");
    pool.write(cc.getName());
    new Print().print();
  }
}
*/

public class CompTest extends TestCase {
    ClassPool sloader;

    public CompTest(String name) {
         super(name);
    }

    protected void print(String msg) {
        System.out.println(msg);
    }

    protected void setUp() throws Exception {
        sloader = ClassPool.getDefault();
    }

    public void testCast() throws Exception {
        Javac jc = new Javac(sloader.get("javassist.compiler.Print"));
        jc.compileStmnt(
              "{((javassist.compiler.Advice)"
            + "  (new javassist.compiler.HelloAspect().getAdvice().get(0)))"
            + "  .print();}");
    }

    public void testStaticMember() throws Exception {
        String src = "javassist.compiler.Print#k = 3;";
        Parser p = new Parser(new Lex(src));
        SymbolTable stb = new SymbolTable();
        Stmnt s = p.parseStatement(stb);
        Expr expr = (Expr)s.getLeft().getLeft();
        assertEquals('#', expr.getOperator());
        assertEquals("javassist.compiler.Print",
                     ((Symbol)expr.oprand1()).get());
    }

    public void testStaticMember2() throws Exception {
        String src = "String#k = 3;";
        Parser p = new Parser(new Lex(src));
        SymbolTable stb = new SymbolTable();
        Stmnt s = p.parseStatement(stb);
        Expr expr = (Expr)s.getLeft().getLeft();
        assertEquals('#', expr.getOperator());
        assertEquals("String", ((Symbol)expr.oprand1()).get());
    }

    public void testDoubleConst() {
        Lex lex = new Lex("7d 0.3d 5e-2d .3d 3e2; .4D 2e-1D;");
        assertEquals(TokenId.DoubleConstant, lex.get());
        assertEquals(TokenId.DoubleConstant, lex.get());
        assertEquals(TokenId.DoubleConstant, lex.get());
        assertEquals(TokenId.DoubleConstant, lex.get());
        assertEquals(TokenId.DoubleConstant, lex.get());
        assertEquals(';', lex.get());
        assertEquals(TokenId.DoubleConstant, lex.get());
        assertEquals(TokenId.DoubleConstant, lex.get());
        assertEquals(';', lex.get());
    }

    public void testRecordLocalVar() throws Exception {
        Javac jv = new Javac(sloader.get("javassist.compiler.Print"));
        jv.gen.recordVariable("I", "i0", 0, jv.stable);
        isRightDecl((Declarator)jv.stable.get("i0"), TokenId.INT, 0, null); 
        jv.gen.recordVariable("[I", "i1", 1, jv.stable);
        isRightDecl((Declarator)jv.stable.get("i1"), TokenId.INT, 1, null); 
        jv.gen.recordVariable("[[D", "i2", 2, jv.stable);
        isRightDecl((Declarator)jv.stable.get("i2"), TokenId.DOUBLE, 2, null); 
        jv.gen.recordVariable("Ljava/lang/String;", "i3", 4, jv.stable);
        isRightDecl((Declarator)jv.stable.get("i3"), TokenId.CLASS, 0,
                    "java/lang/String");
        jv.gen.recordVariable("[LTest;", "i4", 5, jv.stable);
        isRightDecl((Declarator)jv.stable.get("i4"), TokenId.CLASS, 1,
                    "Test");
        jv.gen.recordVariable("[[LTest;", "i5", 6, jv.stable);
        isRightDecl((Declarator)jv.stable.get("i5"), TokenId.CLASS, 2,
                    "Test");
    }

    private void isRightDecl(Declarator d, int type, int dim, String cname) {
        assertEquals(type, d.getType());
        assertEquals(dim, d.getArrayDim());
        assertEquals(cname, d.getClassName());
    }

    public void testArgTypesToString() {
        String s;
        s = TypeChecker.argTypesToString(new int[0], new int[0], new String[0]);
        assertEquals("()", s);
        s = TypeChecker.argTypesToString(new int[] { TokenId.INT, TokenId.CHAR, TokenId.CLASS },
                                         new int[] { 0, 1, 0 },
                                         new String[] { null, null, "String" });
        assertEquals("(int,char[],String)", s);
    }

    public static void main(String[] args) {
        // junit.textui.TestRunner.run(suite());
        junit.awtui.TestRunner.main(new String[] {
            "javassist.compiler.CompTest" });
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Compiler Tests");
        suite.addTestSuite(CompTest.class);
        return suite;
    }
}

class Print{
    public void print(){ System.out.println("@@@"); }
    public static int k;
}

class HelloAspect{
  List list;
  
  HelloAspect() {
    list = new LinkedList(); 
    list.add(new Advice("advice"));
  }
  
  List getAdvice() {
    return list;
  }
}

class Advice{
  String str = "";
  Advice(String str) {
    this.str = str;
  }
  void print(){
      System.out.println(str);
  }
}
