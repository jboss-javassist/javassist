/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2005 Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package sample.preproc;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Vector;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.ClassPool;

/**
 * This is a preprocessor for Java source programs using annotated
 * import declarations.
 *
 * <ul><pre>
 * import <i>class-name</i> by <i>assistant-name</i> [(<i>arg1, arg2, ...</i>)]
 * </pre></ul>
 *
 * <p>To process this annotation, run this class as follows:
 *
 * <ul><pre>
 * java sample.preproc.Compiler sample.j
 * </pre></ul>
 *
 * <p>This command produces <code>sample.java</code>, which only includes
 * regular import declarations.  Also, the Javassist program
 * specified by <i>assistant-name</i> is executed so that it produces
 * class files under the <code>./tmpjvst</code> directory.  The class
 * specified by <i>assistant-name</i> must implement
 * <code>sample.preproc.Assistant</code>.
 *
 * @see sample.preproc.Assistant
 */

public class Compiler {
    protected BufferedReader input;
    protected BufferedWriter output;
    protected ClassPool classPool;

    /**
     * Constructs a <code>Compiler</code> with a source file.
     *
     * @param inputname         the name of the source file.
     */
    public Compiler(String inputname) throws CannotCompileException {
        try {
            input = new BufferedReader(new FileReader(inputname));
        }
        catch (IOException e) {
            throw new CannotCompileException("cannot open: " + inputname);
        }

        String outputname = getOutputFilename(inputname);
        if (outputname.equals(inputname))
            throw new CannotCompileException("invalid source name: "
                                             + inputname);

        try {
            output = new BufferedWriter(new FileWriter(outputname));
        }
        catch (IOException e) {
            throw new CannotCompileException("cannot open: " + outputname);
        }

        classPool = ClassPool.getDefault();
    }

    /**
     * Starts preprocessing.
     */
    public void process() throws IOException, CannotCompileException {
        int c;
        CommentSkipper reader = new CommentSkipper(input, output);
        while ((c = reader.read()) != -1) {
            output.write(c);
            if (c == 'p') {
                if (skipPackage(reader))
                    break;
            }
            else if (c == 'i')
                readImport(reader);
            else if (c != ' ' && c != '\t' && c != '\n' && c != '\r')
                break;
        }

        while ((c = input.read()) != -1)
            output.write(c);

        input.close();
        output.close();
    }

    private boolean skipPackage(CommentSkipper reader) throws IOException {
        int c;
        c = reader.read();
        output.write(c);
        if (c != 'a')
            return true;

        while ((c = reader.read()) != -1) {
            output.write(c);
            if (c == ';')
                break;
        }

        return false;
    }

    private void readImport(CommentSkipper reader)
                                throws IOException, CannotCompileException
    {
        int word[] = new int[5];
        int c;
        for (int i = 0; i < 5; ++i) {
            word[i] = reader.read();
            output.write(word[i]);
        }

        if (word[0] != 'm' || word[1] != 'p' || word[2] != 'o'
            || word[3] != 'r' || word[4] != 't')
            return;     // syntax error?

        c = skipSpaces(reader, ' ');
        StringBuffer classbuf = new StringBuffer();
        while (c != ' ' && c != '\t' && c != '\n' && c != '\r'
               && c != ';' && c != -1) {
            classbuf.append((char)c);
            c = reader.read();
        }

        String importclass = classbuf.toString();
        c = skipSpaces(reader, c);
        if (c == ';') {
            output.write(importclass);
            output.write(';');
            return;
        }
        if (c != 'b')
            syntaxError(importclass);

        reader.read();  // skip 'y'

        StringBuffer assistant = new StringBuffer();
        Vector args = new Vector();
        c = readAssistant(reader, importclass, assistant, args);
        c = skipSpaces(reader, c);
        if (c != ';')
            syntaxError(importclass);

        runAssistant(importclass, assistant.toString(), args);
    }

    void syntaxError(String importclass) throws CannotCompileException {
        throw new CannotCompileException("Syntax error.  Cannot import "
                                         + importclass);
    }

    int readAssistant(CommentSkipper reader, String importclass,
                      StringBuffer assistant, Vector args)
        throws IOException, CannotCompileException
    {
        int c = readArgument(reader, assistant);
        c = skipSpaces(reader, c);
        if (c == '(') {
            do {
                StringBuffer arg = new StringBuffer();
                c = readArgument(reader, arg);
                args.addElement(arg.toString());
                c = skipSpaces(reader, c);
            } while (c == ',');

            if (c != ')')
                syntaxError(importclass);

            return reader.read();
        }

        return c;
    }

    int readArgument(CommentSkipper reader, StringBuffer buf)
        throws IOException
    {
        int c = skipSpaces(reader, ' ');
        while ('A' <= c && c <= 'Z' || 'a' <= c && c <= 'z'
               || '0' <= c && c <= '9' || c == '.' || c == '_') {
            buf.append((char)c);
            c = reader.read();
        }

        return c;
    }

    int skipSpaces(CommentSkipper reader, int c) throws IOException {
        while (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
            if (c == '\n' || c == '\r')
                output.write(c);

            c = reader.read();
        }

        return c;
    }

    /**
     * Is invoked if this compiler encoutenrs:
     *
     * <ul><pre>
     * import <i>class name</i> by <i>assistant</i> (<i>args1</i>, <i>args2</i>, ...);
     * </pre></ul>
     *
     * @param   classname       class name
     * @param   assistantname   assistant
     * @param   argv            args1, args2, ...
     */
    private void runAssistant(String importname, String assistantname,
                              Vector argv)
        throws IOException, CannotCompileException
    {
        Class assistant;
        Assistant a;
        int s = argv.size();
        String[] args = new String[s];
        for (int i = 0; i < s; ++i)
            args[i] = (String)argv.elementAt(i);

        try {
            assistant = Class.forName(assistantname);
        }
        catch (ClassNotFoundException e) {
            throw new CannotCompileException("Cannot find " + assistantname);
        }

        try {
            a = (Assistant)assistant.newInstance();
        }
        catch (Exception e) {
            throw new CannotCompileException(e);
        }

        CtClass[] imports = a.assist(classPool, importname, args);
        s = imports.length;
        if (s < 1)
            output.write(" java.lang.Object;");
        else {
            output.write(' ');
            output.write(imports[0].getName());
            output.write(';');
            for (int i = 1; i < s; ++i) {
                output.write(" import ");
                output.write(imports[1].getName());
                output.write(';');
            }
        }
    }

    private String getOutputFilename(String input) {
        int i = input.lastIndexOf('.');
        if (i < 0)
            i = input.length();

        return input.substring(0, i) + ".java";
    }

    public static void main(String[] args) {
        if (args.length > 0)
            try {
                Compiler c = new Compiler(args[0]);
                c.process();
            }
            catch (IOException e) {
                System.err.println(e);
            }
            catch (CannotCompileException e) {
                System.err.println(e);
            }
        else {
            System.err.println("Javassist version " + CtClass.version);
            System.err.println("No source file is specified.");
        }
    }
}

class CommentSkipper {
    private BufferedReader input;
    private BufferedWriter output;

    public CommentSkipper(BufferedReader reader, BufferedWriter writer) {
        input = reader;
        output = writer;
    }

    public int read() throws IOException {
        int c;
        while ((c = input.read()) != -1)
            if (c != '/')
                return c;
            else {
                c = input.read();
                if (c == '/')
                    skipCxxComments();
                else if (c == '*')
                    skipCComments();
                else 
                    output.write('/');
            }

        return c;
    }

    private void skipCxxComments() throws IOException {
        int c;
        output.write("//");
        while ((c = input.read()) != -1) {
            output.write(c);
            if (c == '\n' || c == '\r')
                break;
        }
    }

    private void skipCComments() throws IOException {
        int c;
        boolean star = false;
        output.write("/*");
        while ((c = input.read()) != -1) {
            output.write(c);
            if (c == '*')
                star = true;
            else if(star && c == '/')
                break;
            else
                star = false;
        }
    }
}
