package javassist.proxyfactory;

import java.io.Serializable;

/**
 * <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class MyCls implements Serializable {

    private int i,j;

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public int getJ() {
        return j;
    }

    public void setJ(int j) {
        this.j = j;
    }
}
