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

package javassist.compiler;

import java.util.ArrayList;
import javassist.compiler.ast.*;
import javassist.bytecode.*;

/* The code generator is implemeted by three files:
 * CodeGen.java, MemberCodeGen.java, and JvstCodeGen.
 * I just wanted to split a big file into three smaller ones.
 */

public abstract class CodeGen extends Visitor implements Opcode, TokenId {
    static final String javaLangObject = "java.lang.Object";
    static final String jvmJavaLangObject = "java/lang/Object";

    static final String javaLangString = "java.lang.String";
    static final String jvmJavaLangString = "java/lang/String";

    protected Bytecode bytecode;
    private int tempVar;

    /**
     * true if the last visited node is a return statement.
     */
    protected boolean hasReturned;

    /**
     * Must be true if compilation is for a static method.
     */
    public boolean inStaticMethod;

    protected ArrayList breakList, continueList;

    /* The following fields are used by atXXX() methods
     * for returning the type of the compiled expression.
     */
    protected int exprType;     // VOID, NULL, CLASS, BOOLEAN, INT, ...
    protected int arrayDim;
    protected String className; // JVM-internal representation

    public CodeGen(Bytecode b) {
        bytecode = b;
        tempVar = -1;
        hasReturned = false;
        inStaticMethod = false;
        breakList = null;
        continueList = null;
    }

    protected static void fatal() throws CompileError {
        throw new CompileError("fatal");
    }

    public static boolean is2word(int type, int dim) {
        return dim == 0 && (type == DOUBLE || type == LONG); 
    }

    public int getMaxLocals() { return bytecode.getMaxLocals(); }

    public void setMaxLocals(int n) {
        bytecode.setMaxLocals(n);
    }

    protected void incMaxLocals(int size) {
        bytecode.incMaxLocals(size);
    }

    /**
     * Returns a local variable that single or double words can be
     * stored in.
     */
    protected int getTempVar() {
        if (tempVar < 0) {
            tempVar = getMaxLocals();
            incMaxLocals(2);
        }

        return tempVar;
    }

    protected int getLocalVar(Declarator d) {
        int v = d.getLocalVar();
        if (v < 0) {
            v = getMaxLocals(); // delayed variable allocation.
            d.setLocalVar(v);
            incMaxLocals(1);
        }

        return v;
    }

    /**
     * Returns the JVM-internal representation of this class name.
     */
    protected abstract String getThisName();

    /**
     * Returns the JVM-internal representation of this super class name.
     */
    protected abstract String getSuperName() throws CompileError;

    /* Converts a class name into a JVM-internal representation.
     *
     * It may also expand a simple class name to java.lang.*.
     * For example, this converts Object into java/lang/Object.
     */
    protected abstract String resolveClassName(ASTList name)
        throws CompileError;

    /* Expands a simple class name to java.lang.*.
     * For example, this converts Object into java/lang/Object.
     */
    protected abstract String resolveClassName(String jvmClassName)
        throws CompileError;

    /**
     * @param name      the JVM-internal representation.
     *                  name is not exapnded to java.lang.*.
     */
    protected static String toJvmArrayName(String name, int dim) {
        if (name == null)
            return null;

        if (dim == 0)
            return name;
        else {
            StringBuffer sbuf = new StringBuffer();
            int d = dim;
            while (d-- > 0)
                sbuf.append('[');

            sbuf.append('L');
            sbuf.append(name);
            sbuf.append(';');

            return sbuf.toString();
        }
    }

    protected static String toJvmTypeName(int type, int dim) {
        char c = 'I';
        switch(type) {
        case BOOLEAN :
            c = 'Z';
            break;
        case BYTE :
            c = 'B';
            break;
        case CHAR :
            c = 'C';
            break;
        case SHORT :
            c = 'S';
            break;
        case INT :
            c = 'I';
            break;
        case LONG :
            c = 'J';
            break;
        case FLOAT :
            c = 'F';
            break;
        case DOUBLE :
            c = 'D';
            break;
        case VOID :
            c = 'V';
            break;
        }

        StringBuffer sbuf = new StringBuffer();
        while (dim-- > 0)
                sbuf.append('[');

        sbuf.append(c);
        return sbuf.toString();
    }

    protected static int jvmTypeNameToExprType(char type) {
        switch(type) {
        case 'Z' :
            return BOOLEAN;
        case 'B' :
            return BYTE;
        case 'C' :
            return CHAR;
        case 'S' :
            return SHORT;
        case 'I' :
            return INT;
        case 'J' :
            return LONG;
        case 'F' :
            return FLOAT;
        case 'D' :
            return DOUBLE;
        case 'V' :
            return VOID;
        default :
            return CLASS;
        }
    }

    public void atASTList(ASTList n) throws CompileError { fatal(); }
    
    public void atPair(Pair n) throws CompileError { fatal(); }

    public void atSymbol(Symbol n) throws CompileError { fatal(); }

    public void atFieldDecl(FieldDecl field) throws CompileError {
        field.getInit().accept(this);
    }

    public void atMethodDecl(MethodDecl method) throws CompileError {
        ASTList mods = method.getModifiers();
        setMaxLocals(1);
        while (mods != null) {
            Keyword k = (Keyword)mods.head();
            mods = mods.tail();
            if (k.get() == STATIC) {
                setMaxLocals(0);
                inStaticMethod = true;
            }
        }
            
        ASTList params = method.getParams();
        while (params != null) {
            atDeclarator((Declarator)params.head());
            params = params.tail();
        }

        Stmnt s = method.getBody();
        atMethodBody(s, method.isConstructor(),
                     method.getReturn().getType() == VOID);
    }

    /**
     * @param isCons	true if super() must be called.
     *			false if the method is a class initializer.
     */
    public void atMethodBody(Stmnt s, boolean isCons, boolean isVoid)
        throws CompileError
    {
        if (s == null)
            return;

        if (isCons && needsSuperCall(s))
            insertDefaultSuperCall();

        hasReturned = false;
        s.accept(this);
        if (!hasReturned)
            if (isVoid) {
                bytecode.addOpcode(Opcode.RETURN);
                hasReturned = true;
            }
            else
                throw new CompileError("no return statement");
    }

    private boolean needsSuperCall(Stmnt body) throws CompileError {
        if (body.getOperator() == BLOCK)
            body = (Stmnt)body.head();

        if (body != null && body.getOperator() == EXPR) {
            ASTree expr = body.head();
            if (expr != null && expr instanceof Expr
                && ((Expr)expr).getOperator() == CALL) {
                ASTree target = ((Expr)expr).head();
                if (target instanceof Keyword) {
                    int token = ((Keyword)target).get();
                    return token != THIS && token != SUPER;
                }
            }
        }

        return true;
    }

    protected abstract void insertDefaultSuperCall() throws CompileError;

    public void atStmnt(Stmnt st) throws CompileError {
        if (st == null)
            return;     // empty

        int op = st.getOperator();
        if (op == EXPR) {
            ASTree expr = st.getLeft();
            if (expr instanceof AssignExpr)
                atAssignExpr((AssignExpr)expr, false);
            else if (isPlusPlusExpr(expr)) {
                Expr e = (Expr)expr;
                atPlusPlus(e.getOperator(), e.oprand1(), e, false);
            }
            else {
                expr.accept(this);
                if (is2word(exprType, arrayDim))
                    bytecode.addOpcode(POP2);
                else if (exprType != VOID)
                    bytecode.addOpcode(POP);
            }
        }
        else if (op == DECL || op == BLOCK) {
            ASTList list = st;
            while (list != null) {
                ASTree h = list.head();
                list = list.tail();
                if (h != null)
                    h.accept(this);
            }
        }
        else if (op == IF)
            atIfStmnt(st);
        else if (op == WHILE || op == DO)
            atWhileStmnt(st, op == WHILE);
        else if (op == FOR)
            atForStmnt(st);
        else if (op == BREAK || op == CONTINUE)
            atBreakStmnt(st, op == BREAK);
        else if (op == TokenId.RETURN)
            atReturnStmnt(st);
        else if (op == THROW)
            atThrowStmnt(st);
        else if (op == TRY)
            atTryStmnt(st);
        else {
            // LABEL, SWITCH label stament might be null?.
            hasReturned = false;
            throw new CompileError(
                "sorry, not supported statement: TokenId " + op);
        }
    }

    private void atIfStmnt(Stmnt st) throws CompileError {
        ASTree expr = st.head();
        Stmnt thenp = (Stmnt)st.tail().head();
        Stmnt elsep = (Stmnt)st.tail().tail().head();
        booleanExpr(false, expr);
        int pc = bytecode.currentPc();
        int pc2 = 0;
        bytecode.addIndex(0);   // correct later

        hasReturned = false;
        if (thenp != null)
            thenp.accept(this);

        boolean thenHasReturned = hasReturned;
        hasReturned = false;

        if (elsep != null && !thenHasReturned) {
            bytecode.addOpcode(Opcode.GOTO);
            pc2 = bytecode.currentPc();
            bytecode.addIndex(0);
        }

        bytecode.write16bit(pc, bytecode.currentPc() - pc + 1);

        if (elsep != null) {
            elsep.accept(this);
            if (!thenHasReturned)
                bytecode.write16bit(pc2, bytecode.currentPc() - pc2 + 1);

            hasReturned = thenHasReturned && hasReturned;
        }
    }

    private void atWhileStmnt(Stmnt st, boolean notDo) throws CompileError {
        ArrayList prevBreakList = breakList;
        ArrayList prevContList = continueList;
        breakList = new ArrayList();
        continueList = new ArrayList();

        ASTree expr = st.head();
        Stmnt body = (Stmnt)st.tail();

        int pc = 0;
        if (notDo) {
            bytecode.addOpcode(Opcode.GOTO);
            pc = bytecode.currentPc();
            bytecode.addIndex(0);
        }

        int pc2 = bytecode.currentPc();
        if (body != null)
            body.accept(this);

        int pc3 = bytecode.currentPc();
        if (notDo)
            bytecode.write16bit(pc, pc3 - pc + 1);

        boolean alwaysBranch = booleanExpr(true, expr);
        bytecode.addIndex(pc2 - bytecode.currentPc() + 1);

        patchGoto(breakList, bytecode.currentPc());
        patchGoto(continueList, pc3);
        continueList = prevContList;
        breakList = prevBreakList;
        hasReturned = alwaysBranch;
    }

    private void patchGoto(ArrayList list, int targetPc) {
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            int pc = ((Integer)list.get(i)).intValue();
            bytecode.write16bit(pc, targetPc - pc + 1);
        }
    }

    private void atForStmnt(Stmnt st) throws CompileError {
        ArrayList prevBreakList = breakList;
        ArrayList prevContList = continueList;
        breakList = new ArrayList();
        continueList = new ArrayList();

        Stmnt init = (Stmnt)st.head();
        ASTList p = st.tail();
        ASTree expr = p.head();
        p = p.tail();
        Stmnt update = (Stmnt)p.head();
        Stmnt body = (Stmnt)p.tail();

        if (init != null)
            init.accept(this);

        int pc = bytecode.currentPc();
        int pc2 = 0;
        if (expr != null) {
            booleanExpr(false, expr);
            pc2 = bytecode.currentPc();
            bytecode.addIndex(0);
        }

        if (body != null)
            body.accept(this);

        int pc3 = bytecode.currentPc();
        if (update != null)
            update.accept(this);

        bytecode.addOpcode(Opcode.GOTO);
        bytecode.addIndex(pc - bytecode.currentPc() + 1);

        int pc4 = bytecode.currentPc();
        if (expr != null)
            bytecode.write16bit(pc2, pc4 - pc2 + 1);

        patchGoto(breakList, pc4);
        patchGoto(continueList, pc3);
        continueList = prevContList;
        breakList = prevBreakList;
        hasReturned = false;
    }

    private void atBreakStmnt(Stmnt st, boolean notCont)
        throws CompileError
    {
        if (st.head() != null)
            throw new CompileError(
                        "sorry, not support labeled break or continue");

        bytecode.addOpcode(Opcode.GOTO);
        Integer pc = new Integer(bytecode.currentPc());
        bytecode.addIndex(0);
        if (notCont)
            breakList.add(pc);
        else
            continueList.add(pc);
    }

    protected void atReturnStmnt(Stmnt st) throws CompileError {
        atReturnStmnt2(st.getLeft());
    }

    protected final void atReturnStmnt2(ASTree result) throws CompileError {
        int op;
        if (result == null)
            op = Opcode.RETURN;
        else {
            result.accept(this);
            if (arrayDim > 0)
                op = ARETURN;
            else {
                int type = exprType;
                if (type == DOUBLE)
                    op = DRETURN;
                else if (type == FLOAT)
                    op = FRETURN;
                else if (type == LONG)
                    op = LRETURN;
                else if (isRefType(type))
                    op = ARETURN;
                else
                    op = IRETURN;
            }
        }

        bytecode.addOpcode(op);
        hasReturned = true;
    }

    private void atThrowStmnt(Stmnt st) throws CompileError {
        ASTree e = st.getLeft();
        e.accept(this);
        if (exprType != CLASS || arrayDim > 0)
            throw new CompileError("bad throw statement");

        bytecode.addOpcode(ATHROW);
        hasReturned = true;
    }

    protected void atTryStmnt(Stmnt st) throws CompileError {
        hasReturned = false;
    }

    private static boolean isPlusPlusExpr(ASTree expr) {
        if (expr instanceof Expr) {
            int op = ((Expr)expr).getOperator();
            return op == PLUSPLUS || op == MINUSMINUS;
        }

        return false;
    }

    public void atDeclarator(Declarator d) throws CompileError {
        d.setLocalVar(getMaxLocals());
        d.setClassName(resolveClassName(d.getClassName()));

        int size;
        if (is2word(d.getType(), d.getArrayDim()))
            size = 2;
        else
            size = 1;

        incMaxLocals(size);

        /*  NOTE: Array initializers has not been supported.
         */
        ASTree init = d.getInitializer();
        if (init != null)
            atVariableAssign(null, '=', null, d, init, false);
    }

    public abstract void atNewExpr(NewExpr n) throws CompileError;

    public void atAssignExpr(AssignExpr expr) throws CompileError {
        atAssignExpr(expr, true);
    }

    protected void atAssignExpr(AssignExpr expr, boolean doDup)
        throws CompileError
    {
        // =, %=, &=, *=, /=, +=, -=, ^=, |=, <<=, >>=, >>>=
        int op = expr.getOperator();
        ASTree left = expr.oprand1();
        ASTree right = expr.oprand2();
        if (left instanceof Variable)
            atVariableAssign(expr, op, (Variable)left,
                             ((Variable)left).getDeclarator(),
                             right, doDup);
        else {
            if (left instanceof Expr) {
                Expr e = (Expr)left;
                if (e.getOperator() == ARRAY) {
                    atArrayAssign(expr, op, (Expr)left, right, doDup);
                    return;
                }
            }

            atFieldAssign(expr, op, left, right, doDup);
        }
    }

    protected static void badAssign(Expr expr) throws CompileError {
        String msg;
        if (expr == null)
            msg = "incompatible type for assignment";
        else
            msg = "incompatible type for " + expr.getName();

        throw new CompileError(msg);
    }

    /* op is either =, %=, &=, *=, /=, +=, -=, ^=, |=, <<=, >>=, or >>>=.
     *
     * expr and var can be null.
     */
    private void atVariableAssign(Expr expr, int op, Variable var,
                                  Declarator d, ASTree right,
                                  boolean doDup) throws CompileError
    {
        int varType = d.getType();
        int varArray = d.getArrayDim();
        String varClass = d.getClassName();
        int varNo = getLocalVar(d);

        if (op != '=')
            atVariable(var);

        atAssignCore(expr, op, right, varType, varArray, varClass);

        if (doDup)
            if (is2word(varType, varArray))
                bytecode.addOpcode(DUP2);
            else
                bytecode.addOpcode(DUP);

        if (varArray > 0)
            bytecode.addAstore(varNo);
        else if (varType == DOUBLE)
            bytecode.addDstore(varNo);
        else if (varType == FLOAT)
            bytecode.addFstore(varNo);
        else if (varType == LONG)
            bytecode.addLstore(varNo);
        else if (isRefType(varType))
            bytecode.addAstore(varNo);
        else
            bytecode.addIstore(varNo);

        exprType = varType;
        arrayDim = varArray;
        className = varClass;
    }

    private void atArrayAssign(Expr expr, int op, Expr array,
                        ASTree right, boolean doDup) throws CompileError
    {
        arrayAccess(array.oprand1(), array.oprand2());

        if (op != '=') {
            bytecode.addOpcode(DUP2);
            bytecode.addOpcode(getArrayReadOp(exprType, arrayDim));
        }

        int aType = exprType;
        int aDim = arrayDim;
        String cname = className;

        atAssignCore(expr, op, right, aType, aDim, cname);

        if (doDup)
            if (is2word(aType, aDim))
                bytecode.addOpcode(DUP2_X2);
            else
                bytecode.addOpcode(DUP_X2);

        bytecode.addOpcode(getArrayWriteOp(aType, aDim));
        exprType = aType;
        arrayDim = aDim;
        className = cname;
    }

    protected abstract void atFieldAssign(Expr expr, int op, ASTree left,
                        ASTree right, boolean doDup) throws CompileError;

    protected void atAssignCore(Expr expr, int op, ASTree right,
                                int type, int dim, String cname)
        throws CompileError
    {
        right.accept(this);
        if (invalidDim(exprType, arrayDim, className, type, dim, cname, false)
            || (op != '=' && dim > 0))
            badAssign(expr);

        if (op == PLUS_E && dim == 0 && type == CLASS)
            atStringConcatExpr(expr, type, dim, cname);
        else if (op != '=') {
            int token = assignOps[op - MOD_E];
            int k = lookupBinOp(token);
            if (k < 0)
                fatal();

            atArithBinExpr(expr, token, k, type);
        }

        if (op != '=' || (dim == 0 && !isRefType(type)))
            atNumCastExpr(exprType, type);

        // type check should be done here.
    }

    private boolean invalidDim(int srcType, int srcDim, String srcClass,
                               int destType, int destDim, String destClass,
                               boolean isCast)
    {
        if (srcDim != destDim)
            if (srcType == NULL)
                return false;
            else if (destDim == 0 && destType == CLASS
                     && jvmJavaLangObject.equals(destClass))
                return false;
            else if (isCast && srcDim == 0 && srcType == CLASS
                     && jvmJavaLangObject.equals(srcClass))
                return false;
            else
                return true;

        return false;
    }

    public void atCondExpr(CondExpr expr) throws CompileError {
        booleanExpr(false, expr.condExpr());
        int pc = bytecode.currentPc();
        bytecode.addIndex(0);   // correct later
        expr.thenExpr().accept(this);
        bytecode.addOpcode(Opcode.GOTO);
        int pc2 = bytecode.currentPc();
        bytecode.addIndex(0);
        bytecode.write16bit(pc, bytecode.currentPc() - pc + 1);
        expr.elseExpr().accept(this);
        bytecode.write16bit(pc2, bytecode.currentPc() - pc2 + 1);
    }

    private final int[] binOp = {
        '+', DADD, FADD, LADD, IADD,
        '-', DSUB, FSUB, LSUB, ISUB,
        '*', DMUL, FMUL, LMUL, IMUL,
        '/', DDIV, FDIV, LDIV, IDIV,
        '%', DREM, FREM, LREM, IREM,
        '|', NOP,  NOP,  LOR,  IOR,
        '^', NOP,  NOP,  LXOR, IXOR,
        '&', NOP,  NOP,  LAND, IAND,
        LSHIFT, NOP, NOP, LSHL, ISHL,
        RSHIFT, NOP, NOP, LSHR, ISHR,
        ARSHIFT, NOP, NOP, LUSHR, IUSHR };

    private int lookupBinOp(int token) {
        int[] code = binOp;
        int s = code.length;
        for (int k = 0; k < s; k = k + 5)
            if (code[k] == token)
                return k;

        return -1;
    }

    public void atBinExpr(BinExpr expr) throws CompileError {
        int token = expr.getOperator();

        /* arithmetic operators: +, -, *, /, %, |, ^, &, <<, >>, >>>
         */
        int k = lookupBinOp(token);
        if (k >= 0) {
            expr.oprand1().accept(this);
            int type1 = exprType;
            int dim1 = arrayDim;
            String cname1 = className;
            expr.oprand2().accept(this);
            if (dim1 != arrayDim)
                throw new CompileError("incompatible array types");

            if (token == '+' && dim1 == 0
                && (type1 == CLASS || exprType == CLASS))
                atStringConcatExpr(expr, type1, dim1, cname1);
            else
                atArithBinExpr(expr, token, k, type1);

            return;
        }

        /* equation: &&, ||, ==, !=, <=, >=, <, >
         */
        booleanExpr(true, expr);
        bytecode.addIndex(7);
        bytecode.addIconst(0);  // false
        bytecode.addOpcode(Opcode.GOTO);
        bytecode.addIndex(4);
        bytecode.addIconst(1);  // true
    }

    /* arrayDim values of the two oprands must be equal.
     * If an oprand type is not a numeric type, this method
     * throws an exception.
     */
    private void atArithBinExpr(Expr expr, int token,
                                int index, int type1) throws CompileError
    {
        if (arrayDim != 0)
            badTypes(expr);

        int type2 = exprType;
        if (token == LSHIFT || token == RSHIFT || token == ARSHIFT)
            if (type2 == INT || type2 == SHORT
                || type2 == CHAR || type2 == BYTE)
                exprType = type1;
            else
                badTypes(expr);
        else
            convertOprandTypes(type1, type2, expr);

        int p = typePrecedence(exprType);
        if (p >= 0) {
            int op = binOp[index + p + 1];
            if (op != NOP) {
                if (p == P_INT)
                    exprType = INT;     // type1 may be BYTE, ...

                bytecode.addOpcode(op);
                return;
            }
        }

        badTypes(expr);
    }

    private void atStringConcatExpr(Expr expr, int type1, int dim1,
                                    String cname1) throws CompileError
    {
        int type2 = exprType;
        int dim2 = arrayDim;
        boolean type2Is2 = is2word(type2, dim2);
        boolean type2IsString
            = (type2 == CLASS && jvmJavaLangString.equals(className));

        if (type2Is2)
            convToString(type2, dim2);

        if (is2word(type1, dim1)) {
            bytecode.addOpcode(DUP_X2);
            bytecode.addOpcode(POP);
        }
        else
            bytecode.addOpcode(SWAP);

        convToString(type1, dim1);
        bytecode.addOpcode(SWAP);

        if (!type2Is2 && !type2IsString)
            convToString(type2, dim2);

        bytecode.addInvokevirtual(javaLangString, "concat",
                                "(Ljava/lang/String;)Ljava/lang/String;");
        exprType = CLASS;
        arrayDim = 0;
        className = jvmJavaLangString;
    }

    private void convToString(int type, int dim) throws CompileError {
        final String method = "valueOf";

        if (isRefType(type) || dim > 0)
            bytecode.addInvokestatic(javaLangString, method,
                                "(Ljava/lang/Object;)Ljava/lang/String;");
        else if (type == DOUBLE)
            bytecode.addInvokestatic(javaLangString, method,
                                     "(D)Ljava/lang/String;");
        else if (type == FLOAT)
            bytecode.addInvokestatic(javaLangString, method,
                                     "(F)Ljava/lang/String;");
        else if (type == LONG)
            bytecode.addInvokestatic(javaLangString, method,
                                     "(J)Ljava/lang/String;");
        else if (type == BOOLEAN)
            bytecode.addInvokestatic(javaLangString, method,
                                     "(Z)Ljava/lang/String;");
        else if (type == CHAR)
            bytecode.addInvokestatic(javaLangString, method,
                                     "(C)Ljava/lang/String;");
        else if (type == VOID)
            throw new CompileError("void type expression");
        else /* INT, BYTE, SHORT */
            bytecode.addInvokestatic(javaLangString, method,
                                     "(I)Ljava/lang/String;");
    }

    /* Produces the opcode to branch if the condition is true.
     * The oprand is not produced.
     *
     * @return	true if the compiled code is GOTO (always branch).
     */
    private boolean booleanExpr(boolean branchIf, ASTree expr)
        throws CompileError
    {
        boolean isAndAnd;
        int op = getCompOperator(expr);
        if (op == EQ) {         // ==, !=, ...
            BinExpr bexpr = (BinExpr)expr;
            int type1 = compileOprands(bexpr);
            compareExpr(branchIf, bexpr.getOperator(), type1, bexpr);
        }
        else if (op == '!')
            booleanExpr(!branchIf, ((Expr)expr).oprand1());
        else if ((isAndAnd = (op == ANDAND)) || op == OROR) {
            BinExpr bexpr = (BinExpr)expr;
            booleanExpr(!isAndAnd, bexpr.oprand1());
            int pc = bytecode.currentPc();
            bytecode.addIndex(0);       // correct later

            booleanExpr(isAndAnd, bexpr.oprand2());
            bytecode.write16bit(pc, bytecode.currentPc() - pc + 3);
            if (branchIf != isAndAnd) {
                bytecode.addIndex(6);   // skip GOTO instruction
                bytecode.addOpcode(Opcode.GOTO);
            }
        }
        else if (isAlwaysBranch(expr, branchIf)) {
            bytecode.addOpcode(Opcode.GOTO);
            return true;	// always branch
        }
        else {                          // others
            expr.accept(this);
            bytecode.addOpcode(branchIf ? IFNE : IFEQ);
        }

        exprType = BOOLEAN;
        arrayDim = 0;
        return false;
    }


    private static boolean isAlwaysBranch(ASTree expr, boolean branchIf) {
        if (expr instanceof Keyword) {
            int t = ((Keyword)expr).get();
            return branchIf ? t == TRUE : t == FALSE;
        }

        return false;
    }

    private static int getCompOperator(ASTree expr) throws CompileError {
        if (expr instanceof Expr) {
            Expr bexpr = (Expr)expr;
            int token = bexpr.getOperator();
            if (token == '!')
                return '!';
            else if ((bexpr instanceof BinExpr)
                     && token != OROR && token != ANDAND
                     && token != '&' && token != '|')
                return EQ;      // ==, !=, ...
            else
                return token;
        }

        return ' ';     // others
    }

    private int compileOprands(BinExpr expr) throws CompileError {
        expr.oprand1().accept(this);
        int type1 = exprType;
        int dim1 = arrayDim;
        expr.oprand2().accept(this);
        if (dim1 != arrayDim)
            throw new CompileError("incompatible array types");

        return type1;
    }

    private final int ifOp[] = { EQ, IF_ICMPEQ, IF_ICMPNE,
                                 NEQ, IF_ICMPNE, IF_ICMPEQ,
                                 LE, IF_ICMPLE, IF_ICMPGT,
                                 GE, IF_ICMPGE, IF_ICMPLT,
                                 '<', IF_ICMPLT, IF_ICMPGE,
                                 '>', IF_ICMPGT, IF_ICMPLE };

    private final int ifOp2[] = { EQ, IFEQ, IFNE,
                                  NEQ, IFNE, IFEQ,
                                  LE, IFLE, IFGT,
                                  GE, IFGE, IFLT,
                                  '<', IFLT, IFGE,
                                  '>', IFGT, IFLE };

    /* Produces the opcode to branch if the condition is true.
     * The oprands are not produced.
     *
     * Parameter expr - compare expression ==, !=, <=, >=, <, >
     */
    private void compareExpr(boolean branchIf,
                             int token, int type1, BinExpr expr)
        throws CompileError
    {
        if (arrayDim == 0)
            convertOprandTypes(type1, exprType, expr);

        int p = typePrecedence(exprType);
        if (p == P_OTHER || arrayDim > 0)
            if (token == EQ)
                bytecode.addOpcode(branchIf ? IF_ACMPEQ : IF_ACMPNE);
            else if (token == NEQ)
                bytecode.addOpcode(branchIf ? IF_ACMPNE : IF_ACMPEQ);
            else
                badTypes(expr);
        else
            if (p == P_INT) {
                int op[] = ifOp;
                for (int i = 0; i < op.length; i += 3)
                    if (op[i] == token) {
                        bytecode.addOpcode(op[i + (branchIf ? 1 : 2)]);
                        return;
                    }

                badTypes(expr);
            }
            else {
                if (p == P_DOUBLE)
                    if (token == '<' || token == LE)
                        bytecode.addOpcode(DCMPG);
                    else
                        bytecode.addOpcode(DCMPL);
                else if (p == P_FLOAT)
                    if (token == '<' || token == LE)
                        bytecode.addOpcode(FCMPG);
                    else
                        bytecode.addOpcode(FCMPL);
                else if (p == P_LONG)
                    bytecode.addOpcode(LCMP); // 1: >, 0: =, -1: <
                else
                    fatal();

                int[] op = ifOp2;
                for (int i = 0; i < op.length; i += 3)
                    if (op[i] == token) {
                        bytecode.addOpcode(op[i + (branchIf ? 1 : 2)]);
                        return;
                    }

                badTypes(expr);
            }
    }

    protected static void badTypes(Expr expr) throws CompileError {
        throw new CompileError("invalid types for " + expr.getName());
    }

    private static final int P_DOUBLE = 0;
    private static final int P_FLOAT = 1;
    private static final int P_LONG = 2;
    private static final int P_INT = 3;
    private static final int P_OTHER = -1;

    protected static boolean isRefType(int type) {
        return type == CLASS || type == NULL;
    }

    private static int typePrecedence(int type) {
        if (type == DOUBLE)
            return P_DOUBLE;
        else if (type == FLOAT)
            return P_FLOAT;
        else if (type == LONG)
            return P_LONG;
        else if (isRefType(type))
            return P_OTHER;
        else if (type == VOID)
            return P_OTHER;     // this is wrong, but ...
        else
            return P_INT;       // BOOLEAN, BYTE, CHAR, SHORT, INT
    }

    private static final int[] castOp = {
            /*            D    F    L    I */
            /* double */ NOP, D2F, D2L, D2I,
            /* float  */ F2D, NOP, F2L, F2I,
            /* long   */ L2D, L2F, NOP, L2I,
            /* other  */ I2D, I2F, I2L, NOP };

    /* do implicit type conversion.
     * arrayDim values of the two oprands must be zero.
     */
    private void convertOprandTypes(int type1, int type2, Expr expr)
        throws CompileError
    {
        boolean rightStrong;
        int type1_p = typePrecedence(type1);
        int type2_p = typePrecedence(type2);

        if (type2_p < 0 && type1_p < 0) // not primitive types
            return;

        if (type2_p < 0 || type1_p < 0) // either is not a primitive type
            badTypes(expr);

        int op, result_type;
        if (type1_p <= type2_p) {
            rightStrong = false;
            exprType = type1;
            op = castOp[type2_p * 4 + type1_p];
            result_type = type1_p;
        }
        else {
            rightStrong = true;
            op = castOp[type1_p * 4 + type2_p];
            result_type = type2_p;
        }

        if (rightStrong) {
            if (result_type == P_DOUBLE || result_type == P_LONG) {
                if (type1_p == P_DOUBLE || type1_p == P_LONG)
                    bytecode.addOpcode(DUP2_X2);
                else
                    bytecode.addOpcode(DUP2_X1);

                bytecode.addOpcode(POP2);
                bytecode.addOpcode(op);
                bytecode.addOpcode(DUP2_X2);
                bytecode.addOpcode(POP2);
            }
            else if (result_type == P_FLOAT) {
                if (type1_p == P_LONG) {
                    bytecode.addOpcode(DUP_X2);
                    bytecode.addOpcode(POP);
                }
                else
                    bytecode.addOpcode(SWAP);

                bytecode.addOpcode(op);
                bytecode.addOpcode(SWAP);
            }
            else
                fatal();
        }
        else if (op != NOP)
            bytecode.addOpcode(op);
    }

    public void atCastExpr(CastExpr expr) throws CompileError {
        String cname = resolveClassName(expr.getClassName());
        String toClass = checkCastExpr(expr, cname);
        int srcType = exprType;
        exprType = expr.getType();
        arrayDim = expr.getArrayDim();
        className = cname;
        if (toClass == null)
            atNumCastExpr(srcType, exprType);   // built-in type
        else
            bytecode.addCheckcast(toClass);
    }

    public void atInstanceOfExpr(InstanceOfExpr expr) throws CompileError {
        String cname = resolveClassName(expr.getClassName());
        String toClass = checkCastExpr(expr, cname);
        bytecode.addInstanceof(toClass);
        exprType = BOOLEAN;
        arrayDim = 0;
    }

    private String checkCastExpr(CastExpr expr, String name)
        throws CompileError
    {
        final String msg = "invalid cast";
        ASTree oprand = expr.getOprand();
        int dim = expr.getArrayDim();
        int type = expr.getType();
        oprand.accept(this);
        int srcType = exprType;
        if (invalidDim(srcType, arrayDim, className, type, dim, name, true)
            || srcType == VOID || type == VOID)
            throw new CompileError(msg);

        if (type == CLASS) {
            if (!isRefType(srcType))
                throw new CompileError(msg);

            return toJvmArrayName(name, dim);
        }
        else
            if (dim > 0)
                return toJvmTypeName(type, dim);
            else
                return null;    // built-in type
    }

    void atNumCastExpr(int srcType, int destType)
        throws CompileError
    {
        if (srcType == destType)
            return;
        
        int op, op2;
        int stype = typePrecedence(srcType);
        int dtype = typePrecedence(destType);
        if (0 <= stype && stype < 3)
            op = castOp[stype * 4 + dtype];
        else
            op = NOP;

        if (destType == DOUBLE)
            op2 = I2D;
        else if (destType == FLOAT)
            op2 = I2F;
        else if (destType == LONG)
            op2 = I2L;
        else if (destType == SHORT)
            op2 = I2S;
        else if (destType == CHAR)
            op2 = I2C;
        else if (destType == BYTE)
            op2 = I2B;
        else
            op2 = NOP;

        if (op != NOP)
            bytecode.addOpcode(op);

        if (op == NOP || op == L2I || op == F2I || op == D2I)
            if (op2 != NOP)
                bytecode.addOpcode(op2);
    }

    public void atExpr(Expr expr) throws CompileError {
        // method call, array access, member access,
        // (unary) +, (unary) -, ++, --, !, ~

        int token = expr.getOperator();
        ASTree oprand = expr.oprand1();
        if (token == CALL)              // method call
            atMethodCall(expr);
        else if (token == '.')
            if (((Symbol)expr.oprand2()).get().equals("length"))
                atArrayLength(expr);
            else
                atFieldRead(expr);
        else if (token == MEMBER) {     // field read
            if (!atClassObject(expr))   // .class
                atFieldRead(expr);
        }
        else if (token == ARRAY)
            atArrayRead(oprand, expr.oprand2());
        else if (token == PLUSPLUS || token == MINUSMINUS)
            atPlusPlus(token, oprand, expr, true);
        else if (token == '!') {
            booleanExpr(false, expr);
            bytecode.addIndex(7);
            bytecode.addIconst(1);
            bytecode.addOpcode(Opcode.GOTO);
            bytecode.addIndex(4);
            bytecode.addIconst(0);
        }
        else {
            expr.oprand1().accept(this);
            int type = typePrecedence(exprType);
            if (arrayDim > 0)
                badType(expr);

            if (token == '-') {
                if (type == P_DOUBLE)
                    bytecode.addOpcode(DNEG);
                else if (type == P_FLOAT)
                    bytecode.addOpcode(FNEG);
                else if (type == P_LONG)
                    bytecode.addOpcode(LNEG);
                else if (type == P_INT) {
                    bytecode.addOpcode(INEG);
                    exprType = INT;     // type may be BYTE, ...
                }
                else
                    badType(expr);
            }
            else if (token == '~') {
                if (type == P_INT) {
                    bytecode.addIconst(-1);
                    bytecode.addOpcode(IXOR);
                    exprType = INT;     // type may be BYTE. ...
                }
                else if (type == P_LONG) {
                    bytecode.addLconst(-1);
                    bytecode.addOpcode(LXOR);
                }
                else
                    badType(expr);

            }
            else if (token == '+') {
                if (type == P_OTHER)
                    badType(expr);

                // do nothing. ignore.
            }
            else
                fatal();
        }
    }

    protected static void badType(Expr expr) throws CompileError {
        throw new CompileError("invalid type for " + expr.getName());
    }

    protected abstract void atMethodCall(Expr expr) throws CompileError;

    protected abstract void atFieldRead(ASTree expr) throws CompileError;

    public boolean atClassObject(Expr expr) throws CompileError {
        if (!((Symbol)expr.oprand2()).get().equals("class"))
            return false;

        if (resolveClassName((ASTList)expr.oprand1()) == null)
            return false;

        throw new CompileError(".class is not supported");
    }

    public void atArrayLength(Expr expr) throws CompileError {
        expr.oprand1().accept(this);
        if (arrayDim == 0)
            throw new CompileError(".length applied to a non array");

        bytecode.addOpcode(ARRAYLENGTH);
        exprType = INT;
        arrayDim = 0;
    }

    public void atArrayRead(ASTree array, ASTree index)
        throws CompileError
    {
        int op;
        arrayAccess(array, index);
        bytecode.addOpcode(getArrayReadOp(exprType, arrayDim));
    }

    protected void arrayAccess(ASTree array, ASTree index)
        throws CompileError
    {
        array.accept(this);
        int type = exprType;
        int dim = arrayDim;
        if (dim == 0)
            throw new CompileError("bad array access");

        String cname = className;

        index.accept(this);
        if (typePrecedence(exprType) != P_INT || arrayDim > 0)
            throw new CompileError("bad array index");

        exprType = type;
        arrayDim = dim - 1;
        className = cname;
    }

    protected static int getArrayReadOp(int type, int dim) {
        int op;
        if (dim > 0)
            return AALOAD;

        switch (type) {
        case DOUBLE :
            return DALOAD;
        case FLOAT :
            return FALOAD;
        case LONG :
            return LALOAD;
        case INT :
            return IALOAD;
        case SHORT :
            return SALOAD;
        case CHAR :
            return CALOAD;
        case BYTE :
        case BOOLEAN :
            return BALOAD;
        default :
            return AALOAD;
        }
    }

    protected static int getArrayWriteOp(int type, int dim) {
        int op;
        if (dim > 0)
            return AASTORE;

        switch (type) {
        case DOUBLE :
            return DASTORE;
        case FLOAT :
            return FASTORE;
        case LONG :
            return LASTORE;
        case INT :
            return IASTORE;
        case CHAR :
            return CASTORE;
        case BYTE :
        case BOOLEAN :
            return BASTORE;
        default :
            return AASTORE;
        }
    }

    private void atPlusPlus(int token, ASTree oprand, Expr expr,
                            boolean doDup) throws CompileError
    {
        boolean isPost = oprand == null;        // ++i or i++?
        if (isPost)
            oprand = expr.oprand2();

        if (oprand instanceof Variable) {
            Declarator d = ((Variable)oprand).getDeclarator();
            int t = exprType = d.getType();
            arrayDim = d.getArrayDim();
            int var = getLocalVar(d);
            if (arrayDim > 0)
                badType(expr);

            if (t == DOUBLE) {
                bytecode.addDload(var);
                if (doDup && isPost)
                    bytecode.addOpcode(DUP2);

                bytecode.addDconst(1.0);
                bytecode.addOpcode(token == PLUSPLUS ? DADD : DSUB);
                if (doDup && !isPost)
                    bytecode.addOpcode(DUP2);

                bytecode.addDstore(var);
            }
            else if (t == LONG) {
                bytecode.addLload(var);
                if (doDup && isPost)
                    bytecode.addOpcode(DUP2);

                bytecode.addLconst((long)1);
                bytecode.addOpcode(token == PLUSPLUS ? LADD : LSUB);
                if (doDup && !isPost)
                    bytecode.addOpcode(DUP2);

                bytecode.addLstore(var);
            }
            else if (t == FLOAT) {
                bytecode.addFload(var);
                if (doDup && isPost)
                    bytecode.addOpcode(DUP);

                bytecode.addFconst(1.0f);
                bytecode.addOpcode(token == PLUSPLUS ? FADD : FSUB);
                if (doDup && !isPost)
                    bytecode.addOpcode(DUP);

                bytecode.addFstore(var);
            }
            else if (t == BYTE || t == CHAR || t == SHORT || t == INT) {
                if (doDup && isPost)
                    bytecode.addIload(var);

                bytecode.addOpcode(IINC);
                bytecode.add(var);
                bytecode.add(token == PLUSPLUS ? 1 : -1);

                if (doDup && !isPost)
                    bytecode.addIload(var);
            }
            else
                badType(expr);
        }
        else {
            if (oprand instanceof Expr) {
                Expr e = (Expr)oprand;
                if (e.getOperator() == ARRAY) {
                    atArrayPlusPlus(token, isPost, e, doDup);
                    return;
                }
            }

            atFieldPlusPlus(token, isPost, oprand, expr, doDup);
        }
    }

    public void atArrayPlusPlus(int token, boolean isPost,
                        Expr expr, boolean doDup) throws CompileError
    {
        arrayAccess(expr.oprand1(), expr.oprand2());
        int t = exprType;
        int dim = arrayDim;
        if (dim > 0)
            badType(expr);

        bytecode.addOpcode(DUP2);
        bytecode.addOpcode(getArrayReadOp(t, arrayDim));
        int dup_code = is2word(t, dim) ? DUP2_X2 : DUP_X2;
        atPlusPlusCore(dup_code, doDup, token, isPost, expr);
        bytecode.addOpcode(getArrayWriteOp(t, dim));
    }

    protected void atPlusPlusCore(int dup_code, boolean doDup,
                                  int token, boolean isPost,
                                  Expr expr) throws CompileError
    {
        int t = exprType;

        if (doDup && isPost)
            bytecode.addOpcode(dup_code);

        if (t == INT || t == BYTE || t == CHAR || t == SHORT) {
            bytecode.addIconst(1);
            bytecode.addOpcode(token == PLUSPLUS ? IADD : ISUB);
            exprType = INT;
        }
        else if (t == LONG) {
            bytecode.addLconst((long)1);
            bytecode.addOpcode(token == PLUSPLUS ? LADD : LSUB);
        }
        else if (t == FLOAT) {
            bytecode.addFconst(1.0f);
            bytecode.addOpcode(token == PLUSPLUS ? FADD : FSUB);
        }
        else if (t == DOUBLE) {
            bytecode.addDconst(1.0);
            bytecode.addOpcode(token == PLUSPLUS ? DADD : DSUB);
        }
        else
            badType(expr);

        if (doDup && !isPost)
            bytecode.addOpcode(dup_code);
    }

    protected abstract void atFieldPlusPlus(int token, boolean isPost,
                ASTree oprand, Expr expr, boolean doDup) throws CompileError;

    public abstract void atMember(Member n) throws CompileError;

    public void atVariable(Variable v) throws CompileError {
        Declarator d = v.getDeclarator();
        exprType = d.getType();
        arrayDim = d.getArrayDim();
        className = d.getClassName();
        int var = getLocalVar(d);

        if (arrayDim > 0)
            bytecode.addAload(var);
        else
            switch (exprType) {
            case CLASS :
                bytecode.addAload(var);
                break;
            case LONG :
                bytecode.addLload(var);
                break;
            case FLOAT :
                bytecode.addFload(var);
                break;
            case DOUBLE :
                bytecode.addDload(var);
                break;
            default :   // BOOLEAN, BYTE, CHAR, SHORT, INT
                bytecode.addIload(var);
                break;
            }
    }

    public void atKeyword(Keyword k) throws CompileError {
        arrayDim = 0;
        int token = k.get();
        switch (token) {
        case TRUE :
            bytecode.addIconst(1);
            exprType = BOOLEAN;
            break;
        case FALSE :
            bytecode.addIconst(0);
            exprType = BOOLEAN;
            break;
        case NULL :
            bytecode.addOpcode(ACONST_NULL);
            exprType = NULL;
            break;
        case THIS :
        case SUPER :
            if (inStaticMethod)
                throw new CompileError("not-available: "
                                       + (token == THIS ? "this" : "super"));

            bytecode.addAload(0);
            exprType = CLASS;
            if (token == THIS)
                className = getThisName();
            else
                className = getSuperName();             
            break;
        default :
            fatal();
        }
    }

    public void atStringL(StringL s) throws CompileError {
        exprType = CLASS;
        arrayDim = 0;
        className = "java/lang/String";
        bytecode.addLdc(s.get());
    }

    public void atIntConst(IntConst i) throws CompileError {
        arrayDim = 0;
        long value = i.get();
        int type = i.getType();
        if (type == IntConstant || type == CharConstant) {
            exprType = (type == IntConstant ? INT : CHAR);
            bytecode.addIconst((int)value);
        }
        else {
            exprType = LONG;
            bytecode.addLconst(value);
        }
    }

    public void atDoubleConst(DoubleConst d) throws CompileError {
        arrayDim = 0;
        if (d.getType() == DoubleConstant) {
            exprType = DOUBLE;
            bytecode.addDconst(d.get());
        }
        else {
            exprType = FLOAT;
            bytecode.addFconst((float)d.get());
        }
    }
}
