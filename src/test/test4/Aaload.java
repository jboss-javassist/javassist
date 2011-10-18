package test4;
import java.awt.MenuItem;

public class Aaload {
    static void narf() {
        String[] list = null;
        for (int i = 0; i < list.length; i++) {
            String name = list[i];
            if (name.endsWith(".txt")) {
                MenuItem item = new MenuItem(name);
                item.addActionListener(null);
            }
        }
    }
}
