package javassist.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * <code>NestMembers_attribute</code>.
 */
public class NestMembersAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"NestMembers"</code>.
     */
    public static final String tag = "NestMembers";

    NestMembersAttribute(ConstPool cp, int n, DataInputStream in) throws IOException {
        super(cp, n, in);
    }

    private NestMembersAttribute(ConstPool cp, byte[] info) {
        super(cp, tag, info);
    }

    /**
     * Makes a copy.  Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        pairs of replaced and substituted
     *                          class names.
     */
    @Override
    public AttributeInfo copy(ConstPool newCp, Map<String, String> classnames) {
        byte[] src = get();
        byte[] dest = new byte[src.length];
        ConstPool cp = getConstPool();

        int n = ByteArray.readU16bit(src, 0);
        ByteArray.write16bit(n, dest, 0);

        for (int i = 0, j = 2; i < n; ++i, j += 2) {
            int index = ByteArray.readU16bit(src, j);
            int newIndex = cp.copy(index, newCp, classnames);
            ByteArray.write16bit(newIndex, dest, j);
        }

        return new NestMembersAttribute(newCp, dest);
    }
}
