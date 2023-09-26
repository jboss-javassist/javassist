package javassist.bytecode;

import javassist.compiler.ast.ASTree;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LineNumberAttributeBuilder {
    private final HashMap<Integer, Integer> map = new HashMap<>();

    public void put(int newPc, ASTree tree) {
        if (tree != null)
            put(newPc, tree.getLineNumber());
    }

    private void put(int newPc, int lineNum) {
        Integer pc = map.get(lineNum);
        if (pc == null || newPc < pc) {
            map.put(lineNum, newPc);
        }
    }

    public LineNumberAttribute build(ConstPool cp) {
        int size = map.size();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(size * 4 + 2);
             DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeShort(size);
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                dos.writeShort(entry.getValue());
                dos.writeShort(entry.getKey());
            }
            return new LineNumberAttribute(cp, bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
