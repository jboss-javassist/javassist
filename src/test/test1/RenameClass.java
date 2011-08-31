package test1;

import java.util.Hashtable;

class RenameClass2 {
    String name;
}

public class RenameClass {
    private Hashtable table;
    public RenameClass() {
	table = new Hashtable();
    }

    public int test() {
        say();
        return 0;
    }

    public void say() {
	RenameClass2[] pair = new RenameClass2[2];
	pair[0] = new RenameClass2();
	table.put("Muga", pair);
	RenameClass2[] p = (RenameClass2[]) table.get("Muga");
    }
}
