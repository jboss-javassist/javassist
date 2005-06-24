package sample.evolve;

import java.io.*;
import java.util.*;

/**
 * Updatable class.  DemoServer instantiates this class and calls
 * show() on the created object.
 */

public class WebPage {
    public void show(OutputStreamWriter out) throws IOException {
	out.write("<H2>Current Time:</H2>");
	Calendar c = new GregorianCalendar();
	out.write("<CENTER><H3><FONT color=\"blue\">");
	out.write(c.getTime().toString());
	out.write("</FONT></H3></CENTER><HR>");
	out.write("<P><A HREF=\"demo.html\">Return to the home page.</A>");
    }
}
