package sample.duplicate;

import java.awt.Graphics;
import java.awt.Color;

public class Ball {
    private int x, y;
    private Color color;
    private int radius = 30;
    private boolean isBackup = false;

    public Ball(int x, int y) {
	move(x, y);
	changeColor(Color.orange);
    }

    // This constructor is for a backup object.
    public Ball(Ball b) {
	isBackup = true;
    }

    // Adjust the position so that the backup object is visible.
    private void adjust() {
	if (isBackup) {
	    this.x += 50;
	    this.y += 50;
	}
    }

    public void paint(Graphics g) {
	g.setColor(color);
	g.fillOval(x, y, radius, radius);
    }

    public void move(int x, int y) {
	this.x = x;
	this.y = y;
	adjust();
    }

    public void changeColor(Color color) {
	this.color = color;
    }
}
