import java.io.*;
import javassist.util.HotSwapper;

public class Test {
    public static void main(String[] args) throws Exception {
        HotSwapper hs = new HotSwapper(8000);
        new HelloWorld().print();

        File newfile = new File("logging/HelloWorld.class");
        byte[] bytes = new byte[(int)newfile.length()];
        new FileInputStream(newfile).read(bytes);
        System.out.println("** reload a logging version");

        hs.reload("HelloWorld", bytes);
        new HelloWorld().print();

        newfile = new File("HelloWorld.class");
        bytes = new byte[(int)newfile.length()];
        new FileInputStream(newfile).read(bytes);
        System.out.println("** reload the original version");

        hs.reload("HelloWorld", bytes);
        new HelloWorld().print();
    }
}
