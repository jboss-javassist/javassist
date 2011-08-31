package javassist.compiler;

import java.io.*;
import javassist.*;
import javassist.bytecode.*;

public class CodeTest implements TokenId {
    public static void main(String[] args) throws Exception {
	ClassPool loader = ClassPool.getDefault();

	CtClass c = loader.get(args[0]);

	String line
	    = new BufferedReader(new InputStreamReader(System.in)).readLine();
	Bytecode b = new Bytecode(c.getClassFile().getConstPool(), 0, 0);

	Javac jc = new Javac(b, c);
	CtMember obj = jc.compile(line);
	if (obj instanceof CtMethod)
	    c.addMethod((CtMethod)obj);
	else
	    c.addConstructor((CtConstructor)obj);

	c.writeFile();
    }
}
