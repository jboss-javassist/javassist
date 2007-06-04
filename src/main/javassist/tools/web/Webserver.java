/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba. All Rights Reserved.
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

package javassist.tools.web;

import java.net.*;
import java.io.*;
import java.util.Date;
import javassist.*;

/**
 * A web server for running sample programs.
 *
 * <p>This enables a Java program to instrument class files loaded by
 * web browsers for applets.  Since the (standard) security manager
 * does not allow an applet to create and use a class loader,
 * instrumenting class files must be done by this web server.
 *
 * <p><b>Note:</b> although this class is included in the Javassist API,
 * it is provided as a sample implementation of the web server using
 * Javassist.  Especially, there might be security flaws in this server.
 * Please use this with YOUR OWN RISK.
 */
public class Webserver {
    private ServerSocket socket;
    private ClassPool classPool;
    protected Translator translator;

    private final static byte[] endofline = { 0x0d, 0x0a };

    private final static int typeHtml = 1;
    private final static int typeClass = 2;
    private final static int typeGif = 3;
    private final static int typeJpeg = 4;
    private final static int typeText = 5;

    /**
     * If this field is not null, the class files taken from
     * <code>ClassPool</code> are written out under the directory
     * specified by this field.  The directory name must not end
     * with a directory separator.
     */
    public String debugDir = null;

    /**
     * The top directory of html (and .gif, .class, ...) files.
     * It must end with the directory separator such as "/".
     * (For portability, "/" should be used as the directory separator.
     * Javassist automatically translates "/" into a platform-dependent
     * character.)
     * If this field is null, the top directory is the current one where
     * the JVM is running.
     *
     * <p>If the given URL indicates a class file and the class file
     * is not found under the directory specified by this variable,
     * then <code>Class.getResourceAsStream()</code> is called
     * for searching the Java class paths.
     */
    public String htmlfileBase = null;

    /**
     * Starts a web server.
     * The port number is specified by the first argument.
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            Webserver web = new Webserver(args[0]);
            web.run();
        }
        else
            System.err.println(
                        "Usage: java javassist.tools.web.Webserver <port number>");
    }

    /**
     * Constructs a web server.
     *
     * @param port      port number
     */
    public Webserver(String port) throws IOException {
        this(Integer.parseInt(port));
    }

    /**
     * Constructs a web server.
     *
     * @param port      port number
     */
    public Webserver(int port) throws IOException {
        socket = new ServerSocket(port);
        classPool = null;
        translator = null;
    }

    /**
     * Requests the web server to use the specified
     * <code>ClassPool</code> object for obtaining a class file.
     */
    public void setClassPool(ClassPool loader) {
        classPool = loader;
    }

    /**
     * Adds a translator, which is called whenever a client requests
     * a class file.
     *
     * @param cp        the <code>ClassPool</code> object for obtaining
     *                  a class file.
     * @param t         a translator.
     */
    public void addTranslator(ClassPool cp, Translator t)
        throws NotFoundException, CannotCompileException
    {
        classPool = cp;
        translator = t;
        t.start(classPool);
    }

    /**
     * Closes the socket.
     */
    public void end() throws IOException {
        socket.close();
    }

    /**
     * Prints a log message.
     */
    public void logging(String msg) {
        System.out.println(msg);
    }

    /**
     * Prints a log message.
     */
    public void logging(String msg1, String msg2) {
        System.out.print(msg1);
        System.out.print(" ");
        System.out.println(msg2);
    }

    /**
     * Prints a log message.
     */
    public void logging(String msg1, String msg2, String msg3) {
        System.out.print(msg1);
        System.out.print(" ");
        System.out.print(msg2);
        System.out.print(" ");
        System.out.println(msg3);
    }

    /**
     * Prints a log message with indentation.
     */
    public void logging2(String msg) {
        System.out.print("    ");
        System.out.println(msg);
    }

    /**
     * Begins the HTTP service.
     */
    public void run() {
        System.err.println("ready to service...");
        for (;;)
            try {
                ServiceThread th = new ServiceThread(this, socket.accept());
                th.start();
            }
            catch (IOException e) {
                logging(e.toString());
            }
    }

    final void process(Socket clnt) throws IOException {
        InputStream in = new BufferedInputStream(clnt.getInputStream());
        String cmd = readLine(in);
        logging(clnt.getInetAddress().getHostName(),
                new Date().toString(), cmd);
        while (skipLine(in) > 0){
        }

        OutputStream out = new BufferedOutputStream(clnt.getOutputStream());
        try {
            doReply(in, out, cmd);
        }
        catch (BadHttpRequest e) {
            replyError(out, e);
        }

        out.flush();
        in.close();
        out.close();
        clnt.close();
    }

    private String readLine(InputStream in) throws IOException {
        StringBuffer buf = new StringBuffer();
        int c;
        while ((c = in.read()) >= 0 && c != 0x0d)
            buf.append((char)c);

        in.read();      /* skip 0x0a (LF) */
        return buf.toString();
    }

    private int skipLine(InputStream in) throws IOException {
        int c;
        int len = 0;
        while ((c = in.read()) >= 0 && c != 0x0d)
            ++len;

        in.read();      /* skip 0x0a (LF) */
        return len;
    }

    /**
     * Proceses a HTTP request from a client.
     *
     * @param out       the output stream to a client
     * @param cmd       the command received from a client
     */
    public void doReply(InputStream in, OutputStream out, String cmd)
        throws IOException, BadHttpRequest
    {
        int len;
        int fileType;
        String filename, urlName;

        if (cmd.startsWith("GET /"))
            filename = urlName = cmd.substring(5, cmd.indexOf(' ', 5));
        else
            throw new BadHttpRequest();

        if (filename.endsWith(".class"))
            fileType = typeClass;
        else if (filename.endsWith(".html") || filename.endsWith(".htm"))
            fileType = typeHtml;
        else if (filename.endsWith(".gif"))
            fileType = typeGif;
        else if (filename.endsWith(".jpg"))
            fileType = typeJpeg;
        else
            fileType = typeText;        // or textUnknown

        len = filename.length();
        if (fileType == typeClass
            && letUsersSendClassfile(out, filename, len))
            return;

        checkFilename(filename, len);
        if (htmlfileBase != null)
            filename = htmlfileBase + filename;

        if (File.separatorChar != '/')
            filename = filename.replace('/', File.separatorChar);

        File file = new File(filename);
        if (file.canRead()) {
            sendHeader(out, file.length(), fileType);
            FileInputStream fin = new FileInputStream(file);
            byte[] filebuffer = new byte[4096];
            for (;;) {
                len = fin.read(filebuffer);
                if (len <= 0)
                    break;
                else
                    out.write(filebuffer, 0, len);
            }

            fin.close();
            return;
        }

        // If the file is not found under the html-file directory,
        // then Class.getResourceAsStream() is tried.

        if (fileType == typeClass) {
            InputStream fin
                = getClass().getResourceAsStream("/" + urlName);
            if (fin != null) {
                ByteArrayOutputStream barray = new ByteArrayOutputStream();
                byte[] filebuffer = new byte[4096];
                for (;;) {
                    len = fin.read(filebuffer);
                    if (len <= 0)
                        break;
                    else
                        barray.write(filebuffer, 0, len);
                }

                byte[] classfile = barray.toByteArray();
                sendHeader(out, classfile.length, typeClass);
                out.write(classfile);
                fin.close();
                return;
            }
        }

        throw new BadHttpRequest();
    }

    private void checkFilename(String filename, int len)
        throws BadHttpRequest
    {
        for (int i = 0; i < len; ++i) {
            char c = filename.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && c != '.' && c != '/')
                throw new BadHttpRequest();
        }

        if (filename.indexOf("..") >= 0)
            throw new BadHttpRequest();
    }

    private boolean letUsersSendClassfile(OutputStream out,
                                          String filename, int length)
        throws IOException, BadHttpRequest
    {
        if (classPool == null)
            return false;

        byte[] classfile;
        String classname
            = filename.substring(0, length - 6).replace('/', '.');
        try {
            if (translator != null)
                translator.onLoad(classPool, classname);

            CtClass c = classPool.get(classname);
            classfile = c.toBytecode();
            if (debugDir != null)
                c.writeFile(debugDir);
        }
        catch (Exception e) {
            throw new BadHttpRequest(e);
        }

        sendHeader(out, classfile.length, typeClass);
        out.write(classfile);
        return true;
    }

    private void sendHeader(OutputStream out, long dataLength, int filetype)
        throws IOException
    {
        out.write("HTTP/1.0 200 OK".getBytes());
        out.write(endofline);
        out.write("Content-Length: ".getBytes());
        out.write(Long.toString(dataLength).getBytes());
        out.write(endofline);
        if (filetype == typeClass)
            out.write("Content-Type: application/octet-stream".getBytes());
        else if (filetype == typeHtml)
            out.write("Content-Type: text/html".getBytes());
        else if (filetype == typeGif)
            out.write("Content-Type: image/gif".getBytes());
        else if (filetype == typeJpeg)
            out.write("Content-Type: image/jpg".getBytes());
        else if (filetype == typeText)
            out.write("Content-Type: text/plain".getBytes());

        out.write(endofline);
        out.write(endofline);
    }

    private void replyError(OutputStream out, BadHttpRequest e)
        throws IOException
    {
        logging2("bad request: " + e.toString());
        out.write("HTTP/1.0 400 Bad Request".getBytes());
        out.write(endofline);
        out.write(endofline);
        out.write("<H1>Bad Request</H1>".getBytes());
    }
}

class ServiceThread extends Thread {
    Webserver web;
    Socket sock;

    public ServiceThread(Webserver w, Socket s) {
        web = w;
        sock = s;
    }

    public void run() {
        try {
            web.process(sock);
        }
        catch (IOException e) {
        }
    }
}
