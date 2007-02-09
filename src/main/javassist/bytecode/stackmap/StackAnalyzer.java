package javassist.bytecode.stackmap;

import javassist.bytecode.ConstPool;

public class StackAnalyzer extends StackAnalyzerCore {

    public StackAnalyzer(ConstPool cp, int maxStack, int maxLocals) {
        super(cp, maxStack, maxLocals);
    }
}
