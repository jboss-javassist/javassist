package sample.rmi;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import javassist.tools.rmi.ObjectImporter;
import javassist.tools.rmi.ObjectNotFoundException;
import javassist.tools.web.Viewer;

public class CountApplet extends Applet implements ActionListener {
    private Font font;
    private ObjectImporter importer;
    private Counter counter;
    private AlertDialog dialog;
    private String message;

    private String paramButton;
    private String paramName;

    public void init() {
	paramButton = getParameter("button");
	paramName = getParameter("name");
	importer = new ObjectImporter(this);
	commonInit();
    }

    /* call this method instead of init() if this program is not run
     * as an applet.
     */
    public void applicationInit() {
	paramButton = "OK";
	paramName = "counter";
	Viewer cl = (Viewer)getClass().getClassLoader();
	importer = new ObjectImporter(cl.getServer(), cl.getPort());
	commonInit();
    }

    private void commonInit() {
	font = new Font("SansSerif", Font.ITALIC, 40);
	Button b = new Button(paramButton);
	b.addActionListener(this);
	add(b);
	dialog = new AlertDialog();
	message = "???";
    }

    public void destroy() {
	dialog.dispose();
    }

    public void start() {
	try {
	    counter = (Counter)importer.lookupObject(paramName);
	    message = Integer.toString(counter.get());
	}
	catch (ObjectNotFoundException e) {
	    dialog.show(e.toString());
	}
    }

    public void actionPerformed(ActionEvent e) {
	counter.increase();
	message = Integer.toString(counter.get());
	repaint();
    }

    public void paint(Graphics g) {
	g.setFont(font);
	g.drawRect(50, 50, 100, 100);
	g.setColor(Color.blue);
	g.drawString(message, 60, 120);
    }

    public static void main(String[] args) {
	Frame f = new Frame("CountApplet");
	CountApplet ca = new CountApplet();
	f.add(ca);
	f.setSize(300, 300);
	ca.applicationInit();
	ca.start();
	f.setVisible(true);
    }
}
