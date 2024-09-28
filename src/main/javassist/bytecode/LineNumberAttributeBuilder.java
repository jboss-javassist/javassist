package javassist.bytecode;

import javassist.compiler.ast.ASTree;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LineNumberAttributeBuilder {
    private final HashMap<Integer, Integer> map = new HashMap<>();

    public void put(int pc, ASTree tree) {
        if (tree != null)
            put(pc, tree.getLineNumber());
    }

    private void put(int pc, int lineNum) {
        Integer oldLineNum = map.get(pc);
        if (oldLineNum == null || lineNum > oldLineNum) {
            map.put(pc, lineNum);
        }
    }

    public LineNumberAttribute build(ConstPool cp) {
        int size = map.size();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(size * 4 + 2);
             DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeShort(size);
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                dos.writeShort(entry.getKey());
                dos.writeShort(entry.getValue());
            }
            return new LineNumberAttribute(cp, bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
