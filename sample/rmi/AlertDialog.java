package sample.rmi;

import java.awt.*;
import java.awt.event.*;

public class AlertDialog extends Frame implements ActionListener {
    private Label label;

    public AlertDialog() {
	super("Alert");
	setSize(200, 100);
	setLocation(100, 100);
	label = new Label();
	Button b = new Button("OK");
	b.addActionListener(this);
	Panel p = new Panel();
	p.add(b);
	add("North", label);
	add("South", p);
    }

    public void show(String message) {
	label.setText(message);
	setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
	setVisible(false);
    }
}
