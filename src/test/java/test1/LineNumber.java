package test1;

import java.io.*;

public class LineNumber {
    public static void sort(int[] data) {
        int i, j;
        for (i = 0; i < data.length - 1; ++i) {
            int k = i;
            int p = data[k];
            for (j = i + 1; j < data.length; ++j)
                if (p > data[j]) {
                    k = j;
                    p = data[k];
                }

            data[k] = data[i];
            data[i] = p;
        }
    }

    public int f(int i) {
        i = i + 3;
        return i;
    }

    public static void main(String[] args) throws Exception {
        int i;
        int data[] = new int[Integer.parseInt(args[0])];

        BufferedReader r = new BufferedReader(new FileReader(args[1]));
        for (i = 0; i < data.length; ++i) {
            String value = r.readLine();
            data[i] = Integer.parseInt(value);
        }

        r.close();
        sort(data);
        PrintWriter out =
            new PrintWriter(new BufferedWriter(new FileWriter(args[2])));
        for (i = 0; i < data.length; ++i)
            out.println(data[i]);

        out.close();
    }
}
