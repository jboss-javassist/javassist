import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.*;

@interface Entity {}

@interface Table {}

public class Test {
    public static void main(String[] args) throws Exception {
        ClassPool classPool = ClassPool.getDefault();
        ClassFile cf = classPool.makeClass("TestSub").getClassFile();
        ConstPool constPool = cf.getConstPool();
        Annotation[] annotations = new Annotation[2];
        AnnotationsAttribute attrib =
                new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation annotation = new Annotation(constPool, classPool.get("Entity"));
        annotations[0] = annotation;
        // Add @Table(name="",schema="") to class
        annotation = new Annotation(constPool, classPool.get("Table"));
        annotation.addMemberValue("name", new StringMemberValue("name", constPool));
        annotation.addMemberValue("schema", new StringMemberValue("schema", constPool));
        ArrayMemberValue blankMemberValueArray = new ArrayMemberValue(new AnnotationMemberValue(constPool), constPool);
        blankMemberValueArray.setValue(new MemberValue[0]);
        annotation.addMemberValue("uniqueConstraints", blankMemberValueArray);
        annotation.addMemberValue("indexes", blankMemberValueArray);
        annotations[1] = annotation;
        attrib.setAnnotations(annotations);
        cf.addAttribute(attrib);
    }
}
