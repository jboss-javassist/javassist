package javassist.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javassist.bytecode.annotation.TypeAnnotationsWriter;

/**
 * A class representing
 * {@code RuntimeVisibleTypeAnnotations} attribute and
 * {@code RuntimeInvisibleTypeAnnotations} attribute.
 *
 * @since 3.19
 */
public class TypeAnnotationsAttribute extends AttributeInfo {
    /**
     * The name of the {@code RuntimeVisibleTypeAnnotations} attribute.
     */
    public static final String visibleTag = "RuntimeVisibleTypeAnnotations";

    /**
     * The name of the {@code RuntimeInvisibleTypeAnnotations} attribute.
     */
    public static final String invisibleTag = "RuntimeInvisibleTypeAnnotations";

    /**
     * Constructs a <code>Runtime(In)VisibleTypeAnnotations_attribute</code>.
     *
     * @param cp            constant pool
     * @param attrname      attribute name (<code>visibleTag</code> or
     *                      <code>invisibleTag</code>).
     * @param info          the contents of this attribute.  It does not
     *                      include <code>attribute_name_index</code> or
     *                      <code>attribute_length</code>.
     */
    public TypeAnnotationsAttribute(ConstPool cp, String attrname, byte[] info) {
        super(cp, attrname, info);
    }

    /**
     * @param n     the attribute name.
     */
    TypeAnnotationsAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    /**
     * Returns <code>num_annotations</code>.
     */
    public int numAnnotations() {
        return ByteArray.readU16bit(info, 0);
    }

    /**
     * Copies this attribute and returns a new copy.
     */
    @Override
    public AttributeInfo copy(ConstPool newCp, Map<String,String> classnames) {
        Copier copier = new Copier(info, constPool, newCp, classnames);
        try {
            copier.annotationArray();
            return new TypeAnnotationsAttribute(newCp, getName(), copier.close());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param oldname       a JVM class name.
     * @param newname       a JVM class name.
     */
    @Override
    void renameClass(String oldname, String newname) {
        Map<String,String> map = new HashMap<String,String>();
        map.put(oldname, newname);
        renameClass(map);
    }

    @Override
    void renameClass(Map<String,String> classnames) {
        Renamer renamer = new Renamer(info, getConstPool(), classnames);
        try {
            renamer.annotationArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void getRefClasses(Map<String,String> classnames) { renameClass(classnames); }

    /**
     * To visit each elements of the type annotation attribute,
     * call {@code annotationArray()}.
     *
     * @see #annotationArray()
     */
    static class TAWalker extends AnnotationsAttribute.Walker {
        SubWalker subWalker;

        TAWalker(byte[] attrInfo) {
            super(attrInfo);
            subWalker = new SubWalker(attrInfo);
        }

        @Override
        int annotationArray(int pos, int num) throws Exception {
            for (int i = 0; i < num; i++) {
                int targetType = info[pos] & 0xff;
                pos = subWalker.targetInfo(pos + 1, targetType);
                pos = subWalker.typePath(pos);
                pos = annotation(pos);
            }

            return pos;
        }
    }

    static class SubWalker {
        byte[] info;

        SubWalker(byte[] attrInfo) {
            info = attrInfo;
        }

        final int targetInfo(int pos, int type) throws Exception {
            switch (type) {
            case 0x00:
            case 0x01: {
                int index = info[pos] & 0xff;
                typeParameterTarget(pos, type, index);
                return pos + 1; }
            case 0x10: {
                int index = ByteArray.readU16bit(info, pos);
                supertypeTarget(pos, index);
                return pos + 2; }
            case 0x11:
            case 0x12: {
                int param = info[pos] & 0xff;
                int bound = info[pos + 1] & 0xff;
                typeParameterBoundTarget(pos, type, param, bound);
                return pos + 2; }
            case 0x13:
            case 0x14:
            case 0x15:
                emptyTarget(pos, type);
                return pos;
            case 0x16: {
                int index = info[pos] & 0xff;
                formalParameterTarget(pos, index);
                return pos + 1; }
            case 0x17: {
                int index = ByteArray.readU16bit(info, pos);
                throwsTarget(pos, index);
                return pos + 2; }
            case 0x40:
            case 0x41: {
                int len = ByteArray.readU16bit(info, pos);
                return localvarTarget(pos + 2, type, len); }
            case 0x42: {
                int index = ByteArray.readU16bit(info, pos);
                catchTarget(pos, index);
                return pos + 2; }
            case 0x43:
            case 0x44:
            case 0x45:
            case 0x46: {
                int offset = ByteArray.readU16bit(info, pos);
                offsetTarget(pos, type, offset);
                return pos + 2; }
            case 0x47:
            case 0x48:
            case 0x49:
            case 0x4a:
            case 0x4b: {
                int offset = ByteArray.readU16bit(info, pos);
                int index = info[pos + 2] & 0xff;
                typeArgumentTarget(pos, type, offset, index);
                return pos + 3; }
            default:
                throw new RuntimeException("invalid target type: " + type);
            }
        }

        void typeParameterTarget(int pos, int targetType, int typeParameterIndex)
            throws Exception {}

        void supertypeTarget(int pos, int superTypeIndex) throws Exception {}

        void typeParameterBoundTarget(int pos, int targetType, int typeParameterIndex,
                                      int boundIndex) throws Exception {}

        void emptyTarget(int pos, int targetType) throws Exception {}

        void formalParameterTarget(int pos, int formalParameterIndex) throws Exception {}

        void throwsTarget(int pos, int throwsTypeIndex) throws Exception {}

        int localvarTarget(int pos, int targetType, int tableLength) throws Exception {
            for (int i = 0; i < tableLength; i++) {
                int start = ByteArray.readU16bit(info, pos);
                int length = ByteArray.readU16bit(info, pos + 2);
                int index = ByteArray.readU16bit(info, pos + 4);
                localvarTarget(pos, targetType, start, length, index);
                pos += 6;
            }

            return pos;
        }

        void localvarTarget(int pos, int targetType, int startPc, int length, int index)
            throws Exception {}

        void catchTarget(int pos, int exceptionTableIndex) throws Exception {}

        void offsetTarget(int pos, int targetType, int offset) throws Exception {}

        void typeArgumentTarget(int pos, int targetType, int offset, int typeArgumentIndex)
            throws Exception {}

        final int typePath(int pos) throws Exception {
            int len = info[pos++] & 0xff;
            return typePath(pos, len);
        }

        int typePath(int pos, int pathLength) throws Exception {
            for (int i = 0; i < pathLength; i++) {
                int kind = info[pos] & 0xff;
                int index = info[pos + 1] & 0xff;
                typePath(pos, kind, index);
                pos += 2;
            }

            return pos;
        }

        void typePath(int pos, int typePathKind, int typeArgumentIndex) throws Exception {}
    }

    static class Renamer extends AnnotationsAttribute.Renamer {
        SubWalker sub;

        Renamer(byte[] attrInfo, ConstPool cp, Map<String,String> map) {
            super(attrInfo, cp, map);
            sub = new SubWalker(attrInfo);
        }

        @Override
        int annotationArray(int pos, int num) throws Exception {
            for (int i = 0; i < num; i++) {
                int targetType = info[pos] & 0xff;
                pos = sub.targetInfo(pos + 1, targetType);
                pos = sub.typePath(pos);
                pos = annotation(pos);
            }

            return pos;
        }
    }

    static class Copier extends AnnotationsAttribute.Copier {
        SubCopier sub;

        Copier(byte[] attrInfo, ConstPool src, ConstPool dest, Map<String,String> map) {
            super(attrInfo, src, dest, map, false);
            TypeAnnotationsWriter w = new TypeAnnotationsWriter(output, dest);
            writer = w;
            sub = new SubCopier(attrInfo, src, dest, map, w);
        }

        @Override
        int annotationArray(int pos, int num) throws Exception {
            writer.numAnnotations(num);
            for (int i = 0; i < num; i++) {
                int targetType = info[pos] & 0xff;
                pos = sub.targetInfo(pos + 1, targetType);
                pos = sub.typePath(pos);
                pos = annotation(pos);
            }

            return pos;
        }
    }

    static class SubCopier extends SubWalker {
        ConstPool srcPool, destPool;
        Map<String,String> classnames;
        TypeAnnotationsWriter writer;

        SubCopier(byte[] attrInfo, ConstPool src, ConstPool dest,
                Map<String,String> map, TypeAnnotationsWriter w)
        {
            super(attrInfo);
            srcPool = src;
            destPool = dest;
            classnames = map;
            writer = w;
        }

        @Override
        void typeParameterTarget(int pos, int targetType, int typeParameterIndex)
            throws Exception
        {
            writer.typeParameterTarget(targetType, typeParameterIndex);
        }

        @Override
        void supertypeTarget(int pos, int superTypeIndex) throws Exception {
            writer.supertypeTarget(superTypeIndex);
        }

        @Override
        void typeParameterBoundTarget(int pos, int targetType, int typeParameterIndex,
                                      int boundIndex)
            throws Exception
        {
            writer.typeParameterBoundTarget(targetType, typeParameterIndex, boundIndex);
        }

        @Override
        void emptyTarget(int pos, int targetType) throws Exception {
            writer.emptyTarget(targetType);
        }

        @Override
        void formalParameterTarget(int pos, int formalParameterIndex) throws Exception {
            writer.formalParameterTarget(formalParameterIndex);
        }

        @Override
        void throwsTarget(int pos, int throwsTypeIndex) throws Exception {
            writer.throwsTarget(throwsTypeIndex);
        }

        @Override
        int localvarTarget(int pos, int targetType, int tableLength) throws Exception {
            writer.localVarTarget(targetType, tableLength);
            return super.localvarTarget(pos, targetType, tableLength);
        }

        @Override
        void localvarTarget(int pos, int targetType, int startPc, int length, int index)
            throws Exception
        {
            writer.localVarTargetTable(startPc, length, index);
        }

        @Override
        void catchTarget(int pos, int exceptionTableIndex) throws Exception {
            writer.catchTarget(exceptionTableIndex);
        }

        @Override
        void offsetTarget(int pos, int targetType, int offset) throws Exception {
            writer.offsetTarget(targetType, offset);
        }

        @Override
        void typeArgumentTarget(int pos, int targetType, int offset, int typeArgumentIndex)
            throws Exception
        {
            writer.typeArgumentTarget(targetType, offset, typeArgumentIndex);
        }

        @Override
        int typePath(int pos, int pathLength) throws Exception {
            writer.typePath(pathLength);
            return super.typePath(pos, pathLength);
        }

        @Override
        void typePath(int pos, int typePathKind, int typeArgumentIndex) throws Exception {
            writer.typePathPath(typePathKind, typeArgumentIndex);
        }
    }
}
