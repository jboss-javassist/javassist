package test.javassist.bytecode.analysis;

import javassist.ClassPool;
import javassist.bytecode.analysis.ControlFlow;
import javassist.bytecode.analysis.ControlFlow.Block;
import javassist.bytecode.analysis.ControlFlow.Node;

public class DomTreePrinter {
    public static void main(String[] args) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        ControlFlow cf = new ControlFlow(pool.get(args[0]).getDeclaredMethod(args[1]));
        Block[] blocks = cf.basicBlocks();
        for (int i = 0; i < blocks.length; i++)
            System.out.println(i + ": " + blocks[i]);

        Node[] dom = cf.dominatorTree();
        for (int i = 0; i < dom.length; i++)
            System.out.println(i + ": " + dom[i]);

        Node[] pdom = cf.postDominatorTree();
        for (int i = 0; i < pdom.length; i++)
            System.out.println(i + ": " + pdom[i]);
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

    public int dummy2(int n, int[] array) {
        int i = 0;
        while (i < n) {
            if (array[i] > 0)
                break;
            if (array[i++] > -1)
                continue;
            array[0]++;
            array[1]++;
        }
        return array[0];
    }

    public int dummy3(int n, int[] array) {
        int i = 0;
        do {
            if (array[i] > 0)
                break;
            if (array[i++] > -1)
                continue;
            array[0]++;
            array[1]++;
        } while (i < n);
        return array[0];
    }

    public int dummy4(int n, int[] array) {
        int i = 0;
        do {
            if (array[i] > 0)
                if (array[i++] > -1)
                    continue;
                else
                    return 0;
            array[0]++;
            array[1]++;
        } while (i < n);
        return array[0];
    }

}
