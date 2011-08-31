package javassist.bytecode;

import java.util.zip.*;
import java.util.Enumeration;
import java.util.List;
import java.io.*;

public class CodeAnalyzerTest {
    public static void main(String[] args) throws Exception {
        ZipFile zfile = new ZipFile(args[0]);
        Enumeration e = zfile.entries();
        while (e.hasMoreElements()) {
            ZipEntry zip = (ZipEntry)e.nextElement();
            if (zip.getName().endsWith(".class"))
                test(zfile.getInputStream(zip));
        }
    }

    static void test(InputStream is) throws Exception {
        is = new BufferedInputStream(is);
        ClassFile cf = new ClassFile(new DataInputStream(is));
        is.close();
        List list = cf.getMethods();
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)list.get(i);
            CodeAttribute ca = minfo.getCodeAttribute();
            if (ca != null) {
                try {
                    int max = ca.getMaxStack();
                    int newMax = ca.computeMaxStack();
                    if (max != newMax)
                        System.out.println(max + " -> " + newMax +
                                           " for " + minfo.getName() + " (" +
                                           minfo.getDescriptor() + ") in " +
                                           cf.getName());
                }
                catch (BadBytecode e) {
                    System.out.println(e.getMessage() +
                                       " for " + minfo.getName() + " (" +
                                       minfo.getDescriptor() + ") in " +
                                       cf.getName());
                }
            }
        }
    }
}
