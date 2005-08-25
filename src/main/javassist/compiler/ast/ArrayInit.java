package javassist.compiler.ast;

import javassist.compiler.CompileError;
/**
 * Array initializer such as <code>{ 1, 2, 3 }</code>.
 */
public class ArrayInit extends ASTList {
    public ArrayInit(ASTree firstElement) {
        super(firstElement);
    }

    public void accept(Visitor v) throws CompileError { v.atArrayInit(this); }

    public String getTag() { return "array"; }
}
