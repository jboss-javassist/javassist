package javassist;

import java.util.Enumeration;
import java.util.zip.*;

public class ClassPoolBench {
    static ClassPool cp;
    static boolean mod = false, detach = false, readonly = false;

    public static void accessClass(String name) throws Exception {
        CtClass cc = cp.get(name);
        System.out.println(cc.getName());
        cc.getSuperclass();
        if (mod)
            cc.getClassFile();

        if (detach)
            cc.stopPruning(true);

        if (!readonly)
            cc.toBytecode();

        if (detach)
           cc.detach();
    }

    public static void accessAll(String filename) throws Exception {
        ZipFile zip = new ZipFile(filename);
        Enumeration files = zip.entries();
        while (files.hasMoreElements()) {
            ZipEntry z = (ZipEntry)files.nextElement();
            String name = z.getName();
            if (name.endsWith(".class")) {
                name = name.substring(0, name.length() - 6)
                           .replace('/', '.');
                accessClass(name);
            }
        }

        zip.close();
    }

    public static void main(String[] args) throws Exception {
        cp = ClassPool.getDefault();
        cp.appendClassPath(args[0]);
        if (args[1].equals("true"))
            mod = true;
        else if (args[1].equals("detach"))
            mod = detach = true;
        else if (args[1].equals("read"))
            readonly = true;

        System.err.println("mod: " + mod + " detach: " + detach
                           + " readonly: " + readonly);
        accessAll(args[0]);
    }
}
