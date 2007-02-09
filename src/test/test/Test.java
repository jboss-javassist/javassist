package test;

import javassist.bytecode.*;

public class Test {
    public static void main(String[] args) {
        String[] names = Mnemonic.OPCODE;
        for (int i = 0; i < names.length; i++)
            if (names[i] == null)
                System.out.println("        case " + i + " :");
            else
                System.out.println("        case Opcode." + names[i].toUpperCase() + " :");
    }
}
