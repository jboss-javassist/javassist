package sample.duplicate;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;

public class Viewer extends Applet
    implements MouseListener, ActionListener, WindowListener
{
    private static final Color[] colorList = {
	Color.orange, Color.pink, Color.green, Color.blue };

    private Ball ball;
    private int colorNo;

    public void init() {
	colorNo = 0;
	Button b = new Button("change");
	b.addActionListener(this);
	add(b);

	addMouseListener(this);
    }

    public void start() {
	ball = new Ball(50, 50);
	ball.changeColor(colorList[0]);
    }

    public void paint(Graphics g) {
	ball.paint(g);
    }

    public void mouseClicked(MouseEvent ev) {
	ball.move(ev.getX(), ev.getY());
	repaint();
    }

    public void mouseEntered(MouseEvent ev) {}

    public void mouseExited(MouseEvent ev) {}

    public void mousePressed(MouseEvent ev) {}

    public void mouseReleased(MouseEvent ev) {}

    public void actionPerformed(ActionEvent e) {
	ball.changeColor(colorList[++colorNo % colorList.length]);
	repaint();
    }

    public void windowOpened(WindowEvent e) {}

    public void windowClosing(WindowEvent e) {
	System.exit(0);
    }

    public void windowClosed(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowActivated(WindowEvent e) {}

    public void windowDeactivated(WindowEvent e) {}

    public static void main(String[] args) {
	Frame f = new Frame("Viewer");
	Viewer view = new Viewer();
	f.addWindowListener(view);
	f.add(view);
	f.setSize(300, 300);
	view.init();
	view.start();
	f.setVisible(true);
    }
}
