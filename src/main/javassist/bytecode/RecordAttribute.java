package javassist.bytecode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the Record attribute in a class file, which stores information
 * about the record components of a record class.
 */
public class RecordAttribute extends AttributeInfo {
	public static final String tag = "Record";

	private List<RecordComponentInfo> components = new ArrayList<>();

    /**
     * Constructs a RecordAttribute by reading from a DataInputStream.
     *
     * @param cp        the constant pool
     * @param nameIndex the index into the constant pool of the attribute name
     * @param in        the input stream to read from
     * @throws IOException if an I/O error occurs
     */
	public RecordAttribute(ConstPool cp, int nameIndex, DataInputStream in) throws IOException {
	    super(cp, nameIndex, in);
	    this.constPool = cp;
	    int pos = 0;
	    int componentsCount = ByteArray.readU16bit(info, pos);
	    pos += 2;
	    for (int i = 0; i < componentsCount; i++) {
	        int name = ByteArray.readU16bit(info, pos);
	        pos += 2;
	        int descriptorIndex = ByteArray.readU16bit(info, pos);
	        pos += 2;
	        int attributesCount = ByteArray.readU16bit(info, pos);
	        pos += 2;
	        List<AttributeInfo> attributes = new ArrayList<>(attributesCount);
	        for (int j = 0; j < attributesCount; j++) {
	            int attrNameIndex = ByteArray.readU16bit(info, pos);
	            pos += 2;
	            int attrLength = ByteArray.read32bit(info, pos);
	            pos += 4;
	            byte[] attrInfo = new byte[attrLength];
	            System.arraycopy(info, pos, attrInfo, 0, attrLength);
	            pos += attrLength;

	            // Create a byte array containing the attribute_name_index, attribute_length, and attrInfo
	            ByteArrayOutputStream baos = new ByteArrayOutputStream();
	            DataOutputStream dos = new DataOutputStream(baos);
	            dos.writeShort(attrNameIndex);    // Write attribute_name_index (u2)
	            dos.writeInt(attrLength);         // Write attribute_length (u4)
	            dos.write(attrInfo);              // Write attribute_info (u1[attrLength])
	            dos.flush();

	            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(baos.toByteArray());
	            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

	            AttributeInfo attr = AttributeInfo.read(constPool, dataInputStream);
	            attributes.add(attr);
	        }

	        RecordComponentInfo component = new RecordComponentInfo(constPool, name, descriptorIndex, attributes);
	        components.add(component);
	    }
	}

	/**
	 * Constructs a RecordAttribute with specified components.
	 *
	 * @param cp         the constant pool
	 * @param attrName   the attribute name
	 * @param components the list of record components
	 */
	public RecordAttribute(ConstPool cp, List<RecordComponentInfo> components) {
		super(cp, tag);
		this.constPool = cp;
		this.components = components;
	}

	/**
	 * Returns the list of record components.
	 *
	 * @return the list of RecordComponentInfo
	 */
	public List<RecordComponentInfo> getComponents() {
		return components;
	}

	/**
	 * Represents a single record component within the Record attribute.
	 */
	public static class RecordComponentInfo {
		private ConstPool constPool;
		int nameIndex;
		int descriptorIndex;
		private List<AttributeInfo> attributes;

		/**
		 * Constructs a RecordComponentInfo.
		 *
		 * @param cp              the constant pool
		 * @param nameIndex       the index into the constant pool of the component's
		 *                        name
		 * @param descriptorIndex the index into the constant pool of the component's
		 *                        descriptor
		 * @param attributes      the list of attributes associated with the component
		 */
		public RecordComponentInfo(ConstPool cp, int nameIndex, int descriptorIndex, List<AttributeInfo> attributes) {
			this.constPool = cp;
			this.nameIndex = nameIndex;
			this.descriptorIndex = descriptorIndex;
			this.attributes = attributes;
		}

		/**
		 * Returns the name Index of the component.
		 *
		 * @return the component name
		 */
		public int getNameIndex() {
			return nameIndex;
		}
		
		
		/**
		 * Returns the name of the component.
		 *
		 * @return the component name
		 */
		public String getName() {
			return constPool.getUtf8Info(nameIndex);
		}

		/**
		 * Returns the descriptor of the component.
		 *
		 * @return the component descriptor
		 */
		public String getDescriptor() {
			return constPool.getUtf8Info(descriptorIndex);
		}

		/**
		 * Returns the list of attributes associated with the component.
		 *
		 * @return the list of AttributeInfo
		 */
		public List<AttributeInfo> getAttributes() {
			return attributes;
		}

		/**
		 * Sets the descriptor index after renaming.
		 *
		 * @param index the new descriptor index
		 */
		public void setDescriptorIndex(int index) {
			this.descriptorIndex = index;
		}
		
		public void setNameIndex(int index) {
			this.nameIndex=index;
		}

	}

	@Override
	public AttributeInfo copy(ConstPool newCp, Map<String, String> classnames) {
		List<RecordComponentInfo> newComponents = new ArrayList<>(components.size());
		for (RecordComponentInfo component : components) {
			String name = component.getName();
			int newNameIndex = newCp.addUtf8Info(name);
			String descriptor = component.getDescriptor();
			String newDescriptor = Descriptor.rename(descriptor, classnames);
			int newDescriptorIndex = newCp.addUtf8Info(newDescriptor);
			List<AttributeInfo> newAttributes = new ArrayList<>();
			for (AttributeInfo attr : component.getAttributes()) {
				AttributeInfo newAttr = attr.copy(newCp, classnames);
				newAttributes.add(newAttr);
			}

			RecordComponentInfo newComponent = new RecordComponentInfo(newCp, newNameIndex, newDescriptorIndex,
					newAttributes);
			newComponents.add(newComponent);
		}
		RecordAttribute newAttribute = new RecordAttribute(newCp, newComponents);
		return newAttribute;
	}

	@Override
	void renameClass(String oldname, String newname) {
		for (RecordComponentInfo component : components) {
			// Update descriptor
			String descriptor = component.getDescriptor();
			String newDescriptor = Descriptor.rename(descriptor, oldname, newname);
			if (!descriptor.equals(newDescriptor)) {
				int newDescriptorIndex = constPool.addUtf8Info(newDescriptor);
				component.setDescriptorIndex(newDescriptorIndex);
			}
			// Rename class in attributes
			for (AttributeInfo attr : component.getAttributes()) {
				attr.renameClass(oldname, newname);
			}
		}
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

	@Override
	public void write(DataOutputStream out) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeShort(components.size());
		for (RecordComponentInfo component : components) {
			dos.writeShort(component.nameIndex);
			dos.writeShort(component.descriptorIndex);
			List<AttributeInfo> attributes = component.getAttributes();
			dos.writeShort(attributes.size());
			for (AttributeInfo attr : attributes) {
				attr.write(dos);
			}
		}
		dos.flush();
		byte[] newInfo = baos.toByteArray();
		out.writeShort(name);
		out.writeInt(newInfo.length);
		out.write(newInfo);
	}

}
