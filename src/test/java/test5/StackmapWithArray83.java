package test5;

import java.util.ArrayList;
import java.util.List;

public class StackmapWithArray83 {
	public int run() {
		bytecodeVerifyError();
		bytecodeVerifyError2();
		return 1;
	}

	public void bytecodeVerifyError() {
        List<Integer> test = new ArrayList<Integer>();
        String[] newLine = new String[10];
        for (Integer idx : test) {
            String address = newLine[1];
            int tabPos = -1;
            if (tabPos != -1) {
                address = address.substring(tabPos + 1);
            }
            newLine[4] = address;
        }
    }

	public void bytecodeVerifyError2() {
        List<Integer> test = new ArrayList<Integer>();
        int[] newLine = new int[10];
        for (Integer idx : test) {
            int address = newLine[1];
            int tabPos = -1;
            if (tabPos != -1) {
                address = address + tabPos;
            }
            newLine[4] = address;
        }
    }
}
