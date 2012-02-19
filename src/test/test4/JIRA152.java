package test4;

import java.util.Map;
import java.util.HashMap;

public class JIRA152 {
    public int foo(int i) { return i; }
    public int bar(int j) { return j; }
    public int tested(int k) {
        String[] p;
        if (k > 0)
            p = new String[1];
        else
            p = null;

        if (p != null)
            while (k < p.length)
                k++;

        return 0;
    }

    public String put(String s, Object obj) {
        return s;
    }

    private static Map<String, String[]> buildColumnOverride(JIRA152 element, String path) {
        Map<String, String[]> columnOverride = new HashMap<String, String[]>();
        if ( element == null ) return null;
        String singleOverride = element.toString();
        String multipleOverrides = element.toString();
        String[] overrides;
        if ( singleOverride != null ) {
            overrides = new String[] { singleOverride };
        }
        /*else if ( multipleOverrides != null ) {
            // overrides = columnOverride.get("foo");
            overrides = null;
        }*/
        else {
            overrides = null;
        }

        if ( overrides != null ) {
            for (String depAttr : overrides) {
                columnOverride.put(
                        element.put(path, depAttr.getClass()),
                        new String[] { depAttr.toLowerCase() }
                );
                //columnOverride.put("a", new String[1]);
            }
        }
        return columnOverride;
    }

    public int test() {
        Map<String,String[]> map = buildColumnOverride(this, "foo");
        return map.size();
    }
}
