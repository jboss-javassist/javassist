package javassist.compiler;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.Bytecode;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.LineNumberAttributeBuilder;
import javassist.compiler.ast.*;

public class JvstCodeGenWitlLineNumber extends JvstCodeGen {
    private final LineNumberAttributeBuilder lineNumberAttributeBuilder = new LineNumberAttributeBuilder();
    public JvstCodeGenWitlLineNumber(Bytecode b, CtClass cc, ClassPool cp) {
        super(b, cc, cp);
    }

    public LineNumberAttribute toLineNumberAttribute() {
        return lineNumberAttributeBuilder.build(bytecode.getConstPool());
    }

    @Override
    public void atASTList(ASTList n) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), n);
        super.atASTList(n);
    }

    @Override
    public void atPair(Pair n) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), n);
        super.atPair(n);
    }

    @Override
    public void atSymbol(Symbol n) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), n);
        super.atSymbol(n);
    }

    @Override
    public void atFieldDecl(FieldDecl field) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), field);
        super.atFieldDecl(field);
    }

    @Override
    public void atMethodDecl(MethodDecl method) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), method);
        super.atMethodDecl(method);
    }

    @Override
    public void atMethodBody(Stmnt s, boolean isCons, boolean isVoid) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), s);
        super.atMethodBody(s, isCons, isVoid);
    }

    @Override
    public void atStmnt(Stmnt st) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), st);
        super.atStmnt(st);
    }

    @Override
    public void atDeclarator(Declarator d) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), d);
        super.atDeclarator(d);
    }

    @Override
    public void atAssignExpr(AssignExpr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atAssignExpr(expr);
    }

    @Override
    protected void atAssignExpr(AssignExpr expr, boolean doDup) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atAssignExpr(expr, doDup);
    }

    @Override
    protected void atAssignCore(Expr expr, int op, ASTree right, int type, int dim, String cname) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atAssignCore(expr, op, right, type, dim, cname);
    }

    @Override
    public void atCondExpr(CondExpr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atCondExpr(expr);
    }

    @Override
    public void atBinExpr(BinExpr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atBinExpr(expr);
    }

    @Override
    public void atInstanceOfExpr(InstanceOfExpr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atInstanceOfExpr(expr);
    }

    @Override
    public void atExpr(Expr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atExpr(expr);
    }

    @Override
    public void atClassObject(Expr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atClassObject(expr);
    }

    @Override
    public void atArrayRead(ASTree array, ASTree index) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), array);
        super.atArrayRead(array, index);
    }

    @Override
    protected void arrayAccess(ASTree array, ASTree index) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), array);
        super.arrayAccess(array, index);
    }

    @Override
    public void atArrayPlusPlus(int token, boolean isPost, Expr expr, boolean doDup) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atArrayPlusPlus(token, isPost, expr, doDup);
    }

    @Override
    protected void atPlusPlusCore(int dup_code, boolean doDup, int token, boolean isPost, Expr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atPlusPlusCore(dup_code, doDup, token, isPost, expr);
    }

    @Override
    public void atVariable(Variable v) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), v);
        super.atVariable(v);
    }

    @Override
    public void atKeyword(Keyword k) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), k);
        super.atKeyword(k);
    }

    @Override
    public void atStringL(javassist.compiler.ast.StringL s) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), s);
        super.atStringL(s);
    }

    @Override
    public void atIntConst(IntConst i) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), i);
        super.atIntConst(i);
    }

    @Override
    public void atDoubleConst(DoubleConst d) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), d);
        super.atDoubleConst(d);
    }

    @Override
    public void atMember(Member mem) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), mem);
        super.atMember(mem);
    }

    @Override
    protected void atFieldAssign(Expr expr, int op, ASTree left, ASTree right, boolean doDup) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atFieldAssign(expr, op, left, right, doDup);
    }

    @Override
    public void atCastExpr(CastExpr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atCastExpr(expr);
    }

    @Override
    protected void atCastToRtype(CastExpr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atCastToRtype(expr);
    }

    @Override
    protected void atCastToWrapper(CastExpr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atCastToWrapper(expr);
    }

    @Override
    public void atCallExpr(CallExpr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atCallExpr(expr);
    }

    @Override
    protected void atCflow(ASTList cname, int lineNumber) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), cname);
        super.atCflow(cname, lineNumber);
    }

    @Override
    protected void atTryStmnt(Stmnt st) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), st);
        super.atTryStmnt(st);
    }

    @Override
    public void atNewExpr(NewExpr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atNewExpr(expr);
    }

    @Override
    public void atNewArrayExpr(NewExpr expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atNewArrayExpr(expr);
    }

    @Override
    protected void atArrayVariableAssign(ArrayInit init, int varType, int varArray, String varClass) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), init);
        super.atArrayVariableAssign(init, varType, varArray, varClass);
    }

    @Override
    public void atArrayInit(ArrayInit init) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), init);
        super.atArrayInit(init);
    }

    @Override
    protected void atMultiNewArray(int type, ASTList classname, ASTList size) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), classname);
        super.atMultiNewArray(type, classname, size);
    }

    @Override
    public void atMethodCallCore(CtClass targetClass, String mname, ASTList args, boolean isStatic, boolean isSpecial, int aload0pos, MemberResolver.Method found) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), args);
        super.atMethodCallCore(targetClass, mname, args, isStatic, isSpecial, aload0pos, found);
    }

    @Override
    protected void atFieldRead(ASTree expr) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atFieldRead(expr);
    }

    @Override
    protected void atFieldPlusPlus(int token, boolean isPost, ASTree oprand, Expr expr, boolean doDup) throws CompileError {
        lineNumberAttributeBuilder.put(bytecode.currentPc(), expr);
        super.atFieldPlusPlus(token, isPost, oprand, expr, doDup);
    }
}
