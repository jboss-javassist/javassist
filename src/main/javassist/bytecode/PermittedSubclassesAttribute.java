/**
 * 
 */
package javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
	private List<Integer> classes = new ArrayList<Integer>();

	protected PermittedSubclassesAttribute(ConstPool cp, int n, DataInputStream in) throws IOException {
		super(cp, n, in);
		classes = new ArrayList<>();
		int pos = 0;
		int number_of_classes = ByteArray.readU16bit(info, pos);
		pos += 2;
		for (int i = 0; i < number_of_classes; i++) {
			int class_index = ByteArray.readU16bit(info, pos);
			pos += 2;
			classes.add(class_index);
		}

	}

	/**
	 * Constructs an empty PermittedSubclassesAttribute attribute.
	 *
	 * @see #setPermittedSubclasses(List<String>)
	 */
	public PermittedSubclassesAttribute(ConstPool cp) {
		super(cp, tag, new byte[2]);
		ByteArray.write16bit(0, get(), 0);

	}

	/**
	 * Gets a read-only List of the class names. Does not reflect the actual map.
	 */
	public List<String> getPermittedSubclasses() {
		List<String> str = new ArrayList<String>();
		for (int clazz : classes) {
			str.add(constPool.getClassInfo(clazz));
		}
		return str;
	}

	public void addClass(String clazz) {
		if(!getPermittedSubclasses().contains(clazz)) {
		classes.add(constPool.addClassInfo(clazz.replace(".", "/")));
		}
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeShort(constPool.addUtf8Info(getName()));
		int size = 2 + classes.size() * 2;
		info = new byte[size];
		ByteArray.write16bit(classes.size(), info, 0);
		int pos = 2;
		for (int clazz : classes) {
			String className = constPool.getClassInfo(clazz);
			ByteArray.write16bit(constPool.addClassInfo(className), info, pos);
			pos += 2;
		}
		out.writeInt(info.length);
		out.write(info);
	}

	@Override
	void renameClass(String oldname, String newname) {
		List<Integer> str = new ArrayList<Integer>();
		for (int clazz : classes) {
			String className = constPool.getClassInfo(clazz);
			if (className.equals(oldname)) {
				str.add(constPool.addClassInfo(newname));
			} else {
				str.add(clazz);
			}
		}
		this.classes = str;
	}

	@Override
	void renameClass(Map<String, String> classnames) {
		for (Map.Entry<String, String> entry : classnames.entrySet()) {
			renameClass(entry.getKey(), entry.getValue());
		}
	}

	@Override
	void getRefClasses(Map<String, String> classnames) {
		renameClass(classnames);
	}

	/**
	 * Copies this attribute and returns a new copy.
	 */
	@Override
	public AttributeInfo copy(ConstPool newCp, Map<String, String> classnames) {
		PermittedSubclassesAttribute attr = new PermittedSubclassesAttribute(newCp);
		for(int clazz:classes) {
			String name = constPool.getClassInfo(clazz);
			attr.addClass(name);
		}
		return attr;
	}

}
