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

package javassist.compiler;

import java.util.ArrayList;
import java.util.Arrays;
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
    TypeChecker typeChecker;

    /**
     * true if the last visited node is a return statement.
     */
    protected boolean hasReturned;

    /**
     * Must be true if compilation is for a static method.
     */
    public boolean inStaticMethod;

    protected ArrayList breakList, continueList;

    /**
     * doit() in ReturnHook is called from atReturn().
     */
    protected static abstract class ReturnHook {
        ReturnHook next;

        /**
         * Returns true if the generated code ends with return,
         * throw, or goto. 
         */
        protected abstract boolean doit(Bytecode b, int opcode);

        protected ReturnHook(CodeGen gen) {
            next = gen.returnHooks;
            gen.returnHooks = this;
        }

        protected void remove(CodeGen gen) {
            gen.returnHooks = next;
        }
    }

    protected ReturnHook returnHooks;

    /* The following fields are used by atXXX() methods
     * for returning the type of the compiled expression.
     */
    protected int exprType;     // VOID, NULL, CLASS, BOOLEAN, INT, ...
    protected int arrayDim;
    protected String className; // JVM-internal representation

    public CodeGen(Bytecode b) {
        bytecode = b;
        tempVar = -1;
        typeChecker = null;
        hasReturned = false;
        inStaticMethod = false;
        breakList = null;
        continueList = null;
        returnHooks = null;
    }

    public void setTypeChecker(TypeChecker checker) {
        typeChecker = checker;
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

    public void compileExpr(ASTree expr) throws CompileError {
        doTypeCheck(expr);
        expr.accept(this);
    }

    public boolean compileBooleanExpr(boolean branchIf, ASTree expr)
        throws CompileError
    {
        doTypeCheck(expr);
        return booleanExpr(branchIf, expr);
    }

    public void doTypeCheck(ASTree expr) throws CompileError {
        if (typeChecker != null)
            expr.accept(typeChecker);
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
            doTypeCheck(expr);
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
        else if (op == SWITCH)
            atSwitchStmnt(st);
        else if (op == SYNCHRONIZED)
            atSyncStmnt(st);
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
        compileBooleanExpr(false, expr);
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

        boolean alwaysBranch = compileBooleanExpr(true, expr);
        bytecode.addIndex(pc2 - bytecode.currentPc() + 1);

        patchGoto(breakList, bytecode.currentPc());
        patchGoto(continueList, pc3);
        continueList = prevContList;
        breakList = prevBreakList;
        hasReturned = alwaysBranch;
    }

    protected void patchGoto(ArrayList list, int targetPc) {
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
            compileBooleanExpr(false, expr);
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

    private void atSwitchStmnt(Stmnt st) throws CompileError {
        compileExpr(st.head());

        ArrayList prevBreakList = breakList;
        breakList = new ArrayList();
        int opcodePc = bytecode.currentPc();
        bytecode.addOpcode(LOOKUPSWITCH);
        int npads = 3 - (opcodePc & 3);
        while (npads-- > 0)
            bytecode.add(0);

        Stmnt body = (Stmnt)st.tail();
        int npairs = 0;
        for (ASTList list = body; list != null; list = list.tail())
            if (((Stmnt)list.head()).getOperator() == CASE)
                ++npairs;

        // opcodePc2 is the position at which the default jump offset is.
        int opcodePc2 = bytecode.currentPc();
        bytecode.addGap(4);
        bytecode.add32bit(npairs);
        bytecode.addGap(npairs * 8);

        long[] pairs = new long[npairs];
        int ipairs = 0;
        int defaultPc = -1;
        for (ASTList list = body; list != null; list = list.tail()) {
            Stmnt label = (Stmnt)list.head();
            int op = label.getOperator();
            if (op == DEFAULT)
                defaultPc = bytecode.currentPc();
            else if (op != CASE)
                fatal();
            else {
                pairs[ipairs++]
                    = ((long)computeLabel(label.head()) << 32) + 
                      ((long)(bytecode.currentPc() - opcodePc) & 0xffffffff);
            }

            hasReturned = false;
            ((Stmnt)label.tail()).accept(this);
        }

        Arrays.sort(pairs);
        int pc = opcodePc2 + 8;
        for (int i = 0; i < npairs; ++i) {
            bytecode.write32bit(pc, (int)(pairs[i] >>> 32));
            bytecode.write32bit(pc + 4, (int)pairs[i]);
            pc += 8;
        } 

        if (defaultPc < 0 || breakList.size() > 0)
            hasReturned = false;

        int endPc = bytecode.currentPc();
        if (defaultPc < 0)
            defaultPc = endPc;

        bytecode.write32bit(opcodePc2, defaultPc - opcodePc);

        patchGoto(breakList, endPc);
        breakList = prevBreakList;
    }

    private int computeLabel(ASTree expr) throws CompileError {
        doTypeCheck(expr);
        expr = TypeChecker.stripPlusExpr(expr);
        if (expr instanceof IntConst)
            return (int)((IntConst)expr).get();
        else
            throw new CompileError("bad case label");
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
            compileExpr(result);
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

        for (ReturnHook har = returnHooks; har != null; har = har.next)
            if (har.doit(bytecode, op)) {
                hasReturned = true;
                return;
            }

        bytecode.addOpcode(op);
        hasReturned = true;
    }

    private void atThrowStmnt(Stmnt st) throws CompileError {
        ASTree e = st.getLeft();
        compileExpr(e);
        if (exprType != CLASS || arrayDim > 0)
            throw new CompileError("bad throw statement");

        bytecode.addOpcode(ATHROW);
        hasReturned = true;
    }

    /* overridden in MemberCodeGen
     */
    protected void atTryStmnt(Stmnt st) throws CompileError {
        hasReturned = false;
    }

    private void atSyncStmnt(Stmnt st) throws CompileError {
        int nbreaks = getListSize(breakList);
        int ncontinues = getListSize(continueList);

        compileExpr(st.head());
        if (exprType != CLASS && arrayDim == 0)
            throw new CompileError("bad type expr for synchronized block");

        Bytecode bc = bytecode;
        final int var = bc.getMaxLocals();
        bc.incMaxLocals(1);
        bc.addOpcode(DUP);
        bc.addAstore(var);
        bc.addOpcode(MONITORENTER);

        ReturnHook rh = new ReturnHook(this) {
            protected boolean doit(Bytecode b, int opcode) {
                b.addAload(var);
                b.addOpcode(MONITOREXIT);
                return false;
            }
        };

        int pc = bc.currentPc();
        Stmnt body = (Stmnt)st.tail();
        if (body != null)
            body.accept(this);

        int pc2 = bc.currentPc();
        int pc3 = 0;
        if (!hasReturned) {
            rh.doit(bc, 0);     // the 2nd arg is ignored.
            bc.addOpcode(Opcode.GOTO);
            pc3 = bc.currentPc();
            bc.addIndex(0);
        }

        if (pc < pc2) {         // if the body is not empty
            int pc4 = bc.currentPc();
            rh.doit(bc, 0);         // the 2nd arg is ignored.
            bc.addOpcode(ATHROW);
            bc.addExceptionHandler(pc, pc2, pc4, 0);
        }

        if (!hasReturned)
            bc.write16bit(pc3, bc.currentPc() - pc3 + 1);

        rh.remove(this);

        if (getListSize(breakList) != nbreaks
            || getListSize(continueList) != ncontinues)
            throw new CompileError(
                "sorry, cannot break/continue in synchronized block");
    }

    private static int getListSize(ArrayList list) {
        return list == null ? 0 : list.size();
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
        if (init != null) {
            doTypeCheck(init);
            atVariableAssign(null, '=', null, d, init, false);
        }
    }

    public abstract void atNewExpr(NewExpr n) throws CompileError;

    public abstract void atArrayInit(ArrayInit init) throws CompileError;

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

        // expr is null if the caller is atDeclarator().
        if (expr == null && right instanceof ArrayInit)
            atArrayVariableAssign((ArrayInit)right, varType, varArray, varClass);
        else
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

    protected abstract void atArrayVariableAssign(ArrayInit init,
            int varType, int varArray, String varClass) throws CompileError;

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
        if (op == PLUS_E && dim == 0 && type == CLASS)
            atStringPlusEq(expr, type, dim, cname, right);
        else {
            right.accept(this);
            if (invalidDim(exprType, arrayDim, className, type, dim, cname,
                           false) || (op != '=' && dim > 0))
                badAssign(expr);

            if (op != '=') {
                int token = assignOps[op - MOD_E];
                int k = lookupBinOp(token);
                if (k < 0)
                    fatal();

                atArithBinExpr(expr, token, k, type);
            }
        }

        if (op != '=' || (dim == 0 && !isRefType(type)))
            atNumCastExpr(exprType, type);

        // type check should be done here.
    }

    private void atStringPlusEq(Expr expr, int type, int dim, String cname,
                                ASTree right)
        throws CompileError
    {
        if (!jvmJavaLangString.equals(cname))
            badAssign(expr);

        convToString(type, dim);    // the value might be null.
        right.accept(this);
        convToString(exprType, arrayDim);
        bytecode.addInvokevirtual(javaLangString, "concat",
                                "(Ljava/lang/String;)Ljava/lang/String;");
        exprType = CLASS;
        arrayDim = 0;
        className = jvmJavaLangString;
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
        int dim1 = arrayDim;
        bytecode.addOpcode(Opcode.GOTO);
        int pc2 = bytecode.currentPc();
        bytecode.addIndex(0);
        bytecode.write16bit(pc, bytecode.currentPc() - pc + 1);
        expr.elseExpr().accept(this);
        if (dim1 != arrayDim)
            throw new CompileError("type mismatch in ?:");

        bytecode.write16bit(pc2, bytecode.currentPc() - pc2 + 1);
    }

    static final int[] binOp = {
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

    static int lookupBinOp(int token) {
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
            ASTree right = expr.oprand2();
            if (right == null)
                return;     // see TypeChecker.atBinExpr().

            int type1 = exprType;
            int dim1 = arrayDim;
            String cname1 = className;
            right.accept(this);
            if (dim1 != arrayDim)
                throw new CompileError("incompatible array types");

            if (token == '+' && dim1 == 0
                && (type1 == CLASS || exprType == CLASS))
                atStringConcatExpr(expr, type1, dim1, cname1);
            else
                atArithBinExpr(expr, token, k, type1);
        }
        else {
            /* equation: &&, ||, ==, !=, <=, >=, <, >
            */
            booleanExpr(true, expr);
            bytecode.addIndex(7);
            bytecode.addIconst(0);  // false
            bytecode.addOpcode(Opcode.GOTO);
            bytecode.addIndex(4);
            bytecode.addIconst(1);  // true
        }
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
                if (p == P_INT && exprType != BOOLEAN)
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

        // even if type1 is String, the left operand might be null.
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
            // here, arrayDim might represent the array dim. of the left oprand
            // if the right oprand is NULL.
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
            if (exprType != BOOLEAN || arrayDim != 0)
                throw new CompileError("boolean expr is required");

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

    static int getCompOperator(ASTree expr) throws CompileError {
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
            if (type1 != NULL && exprType != NULL)
                throw new CompileError("incompatible array types");
            else if (exprType == NULL)
                arrayDim = dim1;

        if (type1 == NULL)
            return exprType;
        else
            return type1;
    }

    private static final int ifOp[] = { EQ, IF_ICMPEQ, IF_ICMPNE,
                                        NEQ, IF_ICMPNE, IF_ICMPEQ,
                                        LE, IF_ICMPLE, IF_ICMPGT,
                                        GE, IF_ICMPGE, IF_ICMPLT,
                                        '<', IF_ICMPLT, IF_ICMPGE,
                                        '>', IF_ICMPGT, IF_ICMPLE };

    private static final int ifOp2[] = { EQ, IFEQ, IFNE,
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

    // used in TypeChecker.
    static boolean isP_INT(int type) {
        return typePrecedence(type) == P_INT;
    }

    // used in TypeChecker.
    static boolean rightIsStrong(int type1, int type2) {
        int type1_p = typePrecedence(type1);
        int type2_p = typePrecedence(type2);
        return type1_p >= 0 && type2_p >= 0 && type1_p > type2_p;
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
        // array access, member access,
        // (unary) +, (unary) -, ++, --, !, ~

        int token = expr.getOperator();
        ASTree oprand = expr.oprand1();
        if (token == '.') {
            String member = ((Symbol)expr.oprand2()).get();
            if (member.equals("class"))                
                atClassObject(expr);  // .class
            else
                atFieldRead(expr);
        }
        else if (token == MEMBER) {     // field read
            /* MEMBER ('#') is an extension by Javassist.
             * The compiler internally uses # for compiling .class
             * expressions such as "int.class".
             */
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
        else if (token == CALL)         // method call
            fatal();
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

    public abstract void atCallExpr(CallExpr expr) throws CompileError;

    protected abstract void atFieldRead(ASTree expr) throws CompileError;

    public void atClassObject(Expr expr) throws CompileError {
        ASTree op1 = expr.oprand1();
        if (!(op1 instanceof Symbol))
            throw new CompileError("fatal error: badly parsed .class expr");

        String cname = ((Symbol)op1).get();
        if (cname.startsWith("[")) {
            int i = cname.indexOf("[L");
            if (i >= 0) {
                String name = cname.substring(i + 2, cname.length() - 1);
                String name2 = resolveClassName(name);
                if (!name.equals(name2)) {
                    /* For example, to obtain String[].class,
                     * "[Ljava.lang.String;" (not "[Ljava/lang/String"!)
                     * must be passed to Class.forName().
                     */
                    name2 = MemberResolver.jvmToJavaName(name2);
                    StringBuffer sbuf = new StringBuffer();
                    while (i-- >= 0)
                        sbuf.append('[');

                    sbuf.append('L').append(name2).append(';');
                    cname = sbuf.toString();
                }
            }
        }
        else {
            cname = resolveClassName(MemberResolver.javaToJvmName(cname));
            cname = MemberResolver.jvmToJavaName(cname);
        }

        atClassObject2(cname);
        exprType = CLASS;
        arrayDim = 0;
        className = "java/lang/Class";
    }

    /* MemberCodeGen overrides this method.
     */
    protected void atClassObject2(String cname) throws CompileError {
        int start = bytecode.currentPc();
        bytecode.addLdc(cname);
        bytecode.addInvokestatic("java.lang.Class", "forName",
                                 "(Ljava/lang/String;)Ljava/lang/Class;");
        int end = bytecode.currentPc();
        bytecode.addOpcode(Opcode.GOTO);
        int pc = bytecode.currentPc();
        bytecode.addIndex(0);   // correct later

        bytecode.addExceptionHandler(start, end, bytecode.currentPc(),
                                     "java.lang.ClassNotFoundException");

        /* -- the following code is for inlining a call to DotClass.fail().

        int var = getMaxLocals();
        incMaxLocals(1);
        bytecode.growStack(1);
        bytecode.addAstore(var);

        bytecode.addNew("java.lang.NoClassDefFoundError");
        bytecode.addOpcode(DUP);
        bytecode.addAload(var);
        bytecode.addInvokevirtual("java.lang.ClassNotFoundException",
                                  "getMessage", "()Ljava/lang/String;");
        bytecode.addInvokespecial("java.lang.NoClassDefFoundError", "<init>",
                                  "(Ljava/lang/String;)V");
        */

        bytecode.growStack(1);
        bytecode.addInvokestatic("javassist.runtime.DotClass", "fail",
                                 "(Ljava/lang/ClassNotFoundException;)"
                                 + "Ljava/lang/NoClassDefFoundError;");
        bytecode.addOpcode(ATHROW);
        bytecode.write16bit(pc, bytecode.currentPc() - pc + 1);
    }

    public void atArrayRead(ASTree array, ASTree index)
        throws CompileError
    {
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
        case SHORT :
            return SASTORE;
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

                int delta = token == PLUSPLUS ? 1 : -1;
                if (var > 0xff) {
                    bytecode.addOpcode(WIDE);
                    bytecode.addOpcode(IINC);
                    bytecode.addIndex(var);
                    bytecode.addIndex(delta);
                }
                else {
                    bytecode.addOpcode(IINC);
                    bytecode.add(var);
                    bytecode.add(delta);
                }

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
        className = jvmJavaLangString;
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
