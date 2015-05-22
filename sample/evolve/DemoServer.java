package sample.evolve;

import javassist.tools.web.*;
import java.io.*;

/**
 * A web server for demonstrating class evolution.  It must be
 * run with a DemoLoader.
 *
 * If a html file /java.html is requested, this web server calls
 * WebPage.show() for constructing the contents of that html file
 * So if a DemoLoader changes the definition of WebPage, then
 * the image of /java.html is also changed.
 * Note that WebPage is not an applet.  It is rather
 * similar to a CGI script or a servlet.  The web server never
 * sends the class file of WebPage to web browsers.
 *
 * Furthermore, if a html file /update.html is requested, this web
 * server overwrites WebPage.class (class file) and calls update()
 * in VersionManager so that WebPage.class is loaded into the JVM
 * again.  The new contents of WebPage.class are copied from
 * either sample/evolve/WebPage.class
 * or sample/evolve/sample/evolve/WebPage.class.
 */
public class DemoServer extends Webserver {

    public static void main(String[] args) throws IOException
    {
	if (args.length == 1) {
	    DemoServer web = new DemoServer(Integer.parseInt(args[0]));
	    web.run();
	}
	else
	    System.err.println(
		"Usage: java sample.evolve.DemoServer <port number>");
    }

    public DemoServer(int port) throws IOException {
	super(port);
	htmlfileBase = "sample/evolve/";
    }

    private static final String ver0 = "sample/evolve/WebPage.class.0";
    private static final String ver1 = "sample/evolve/WebPage.class.1";
    private String currentVersion = ver0;

    public void doReply(InputStream in, OutputStream out, String cmd)
	throws IOException, BadHttpRequest
    {
	if (cmd.startsWith("GET /java.html ")) {
	    runJava(out);
	    return;
	}
	else if (cmd.startsWith("GET /update.html ")) {
	    try {
		if (currentVersion == ver0)
		    currentVersion = ver1;
		else
		    currentVersion = ver0;

		updateClassfile(currentVersion);
		VersionManager.update("sample.evolve.WebPage");
	    }
	    catch (CannotUpdateException e) {
		logging(e.toString());
	    }
	    catch (FileNotFoundException e) {
		logging(e.toString());
	    }
	}

	super.doReply(in, out, cmd);
    }

    private void runJava(OutputStream outs) throws IOException {
	OutputStreamWriter out = new OutputStreamWriter(outs);
	out.write("HTTP/1.0 200 OK\r\n\r\n");
	WebPage page = new WebPage();
	page.show(out);
	out.close();
    }

    /* updateClassfile() copies the specified file to WebPage.class.
     */
    private void updateClassfile(String filename)
	throws IOException, FileNotFoundException
    {
	byte[] buf = new byte[1024];

	FileInputStream fin
	    = new FileInputStream(filename);
	FileOutputStream fout
	    = new FileOutputStream("sample/evolve/WebPage.class");
	for (;;) {
	    int len = fin.read(buf);
	    if (len >= 0)
		fout.write(buf, 0, len);
	    else
		break;
	}
    }
}
