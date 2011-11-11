package test.javassist.bytecode.analysis;

import javassist.ClassPool;
import javassist.bytecode.analysis.ControlFlow;
import javassist.bytecode.analysis.ControlFlow.Block;

public class DomTreePrinter {
    public static void main(String[] args) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        ControlFlow cf = new ControlFlow(pool.get(args[0]).getDeclaredMethod(args[1]));
        Block[] blocks = cf.basicBlocks();
        for (int i = 0; i < blocks.length; i++)
            System.out.println(i + ": " + blocks[i]);
    }

    public int dummy(int n, int[] array) {
        for (int i = 0; i < n; i++) {
            if (array[i] > 0)
                break;
            if (array[i] > -1)
                continue;
            array[0]++;
            array[1]++;
        }
        return array[0];
    }
}
