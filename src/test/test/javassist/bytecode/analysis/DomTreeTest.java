package test.javassist.bytecode.analysis;

import javassist.ClassPool;
import javassist.bytecode.analysis.ControlFlow;
import javassist.bytecode.analysis.ControlFlow.Block;
import javassist.bytecode.analysis.ControlFlow.Node;
import junit.framework.TestCase;

public class DomTreeTest extends TestCase {
    private ClassPool pool = ClassPool.getDefault();

    public void testDomtree() throws Exception {
        ControlFlow cf = new ControlFlow(pool.get(DomTreeTest.class.getName()).getDeclaredMethod("test1"));
        Block[] blocks = cf.basicBlocks();
        // for (int i = 0; i < blocks.length; i++)
        //    System.out.println(i + ": " + blocks[i]);
        testBlock(blocks[0], new int[] {}, new int[] { 11, 6 } );
        testBlock(blocks[1], new int[] { 0 }, new int[] { 17, 11 } );
        testBlock(blocks[2], new int[] { 0, 6 }, new int[] { 19, 17 });
        testBlock(blocks[3], new int[] { 6, 11 }, new int[] { 19 });
        testBlock(blocks[4], new int[] { 11, 17 }, new int[] {});

        Node[] dom = cf.dominatorTree();
        assertNull(dom[0].parent());
        assertEquals(0, dom[1].parent().block().position());
        assertEquals(0, dom[2].parent().block().position());
        assertEquals(0, dom[3].parent().block().position());
        assertEquals(0, dom[4].parent().block().position());

        Node[] pdom = cf.postDominatorTree();
        assertEquals(19, pdom[0].parent().block().position());
        assertEquals(19, pdom[1].parent().block().position());
        assertEquals(19, pdom[2].parent().block().position());
        assertEquals(19, pdom[3].parent().block().position());
        assertNull(pdom[4].parent());
    }

    private void testBlock(Block b, int[] incoming, int[] outgoing) {
        assertEquals(incoming.length, b.incomings());
        int i = 0;
        for (int index: incoming)
            assertEquals(index, b.incoming(i++).position());
        i = 0;
        assertEquals(outgoing.length, b.exits());
        for (int index: outgoing)
            assertEquals(index, b.exit(i++).position());
    }

    private void testNode(Node n, int[] incoming, int[] outgoing) {
        int i = 0;
        for (int index: incoming)
            assertEquals(index, n.parent().block().index());
    }

    public void test1(){
        int k=0;
        if (k != 0 && k!=2 || k < 7) {
            k = 3 ;
        }
    }

    public void testDomtree2() throws Exception {
        ControlFlow cf = new ControlFlow(pool.get(DomTreeTest.class.getName()).getDeclaredMethod("test2"));
        Block[] blocks = cf.basicBlocks();
        // for (int i = 0; i < blocks.length; i++)
        //    System.out.println(i + ": " + blocks[i]);
        testBlock(blocks[0], new int[] { 7 }, new int[] { 14, 7 } );
        testBlock(blocks[1], new int[] { 0 }, new int[] { 0, 12 } );
        testBlock(blocks[2], new int[] { 7 }, new int[] {});
        testBlock(blocks[3], new int[] { 0 }, new int[] {});

        Node[] dom = cf.dominatorTree();
        assertNull(dom[0].parent());
        assertEquals(0, dom[1].parent().block().position());
        assertEquals(7, dom[2].parent().block().position());
        assertEquals(0, dom[3].parent().block().position());

        Node[] pdom = cf.postDominatorTree();
        assertNull(pdom[0].parent());
        assertNull(pdom[1].parent());
        assertNull(pdom[2].parent());
        assertNull(pdom[3].parent());
    }

    public int test2(int i){
        while (i-- > 0)
            if (i == 3)
                return 1;

        return i + 3;
    }

    public void testDomtree3() throws Exception {
        ControlFlow cf = new ControlFlow(pool.get(DomTreeTest.class.getName()).getDeclaredMethod("test3"));
        Block[] blocks = cf.basicBlocks();
        for (int i = 0; i < blocks.length; i++)
            System.out.println(blocks[i]);
    }

    public int test3(int i, int j) {
        while (i > 0) {
            try {
                j++;
            }
            catch (Throwable t) {
                j = 0;
            }
            i--;
        }

        return j;
    }
}
