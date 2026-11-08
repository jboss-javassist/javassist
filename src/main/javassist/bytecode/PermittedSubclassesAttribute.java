/**
 * 
 */
package javassist.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class representing PermittedSubclasses_attribute
 */
public class PermittedSubclassesAttribute extends AttributeInfo {

	/**
	 * The name of the <code>PermittedSubclasses</code> attribute.
	 */
	public static final String tag = "PermittedSubclasses";

	protected PermittedSubclassesAttribute(ConstPool cp, int n, DataInputStream in) throws IOException {
		super(cp, n, in);
	}

	private PermittedSubclassesAttribute(ConstPool cp, byte[] info) {
		super(cp, tag, info);
	}

	/**
	 * Constructs an empty PermittedSubclassesAttribute attribute.
	 *
	 * @see #addClass(String)
	 */
	public PermittedSubclassesAttribute(ConstPool cp) {
		this(cp, new byte[2]);
	}

	/**
	 * Gets a read-only List of the class names. Does not reflect the actual map.
	 */
	public List<String> getPermittedSubclasses() {
		int n = ByteArray.readU16bit(info, 0);
		List<String> str = new ArrayList<String>(n);
		for (int i = 0; i < n; i++) {
			str.add(constPool.getClassInfo(ByteArray.readU16bit(info, 2 + i * 2)));
		}
		return str;
	}

	public void addClass(String clazz) {
		if (getPermittedSubclasses().contains(Descriptor.toJavaName(clazz))) {
			return;
		}

		int n = ByteArray.readU16bit(info, 0);
		byte[] newInfo = new byte[info.length + 2];
		System.arraycopy(info, 0, newInfo, 0, info.length);
		ByteArray.write16bit(n + 1, newInfo, 0);
		ByteArray.write16bit(constPool.addClassInfo(clazz), newInfo, info.length);
		set(newInfo);
	}

	/**
	 * Copies this attribute and returns a new copy.
	 */
	@Override
	public AttributeInfo copy(ConstPool newCp, Map<String, String> classnames) {
		int n = ByteArray.readU16bit(info, 0);
		byte[] newInfo = new byte[info.length];
		ByteArray.write16bit(n, newInfo, 0);
		for (int i = 0; i < n; i++) {
			int pos = 2 + i * 2;
			int index = constPool.copy(ByteArray.readU16bit(info, pos), newCp, classnames);
			ByteArray.write16bit(index, newInfo, pos);
		}
		return new PermittedSubclassesAttribute(newCp, newInfo);
	}

}
