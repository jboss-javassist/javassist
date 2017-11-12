package test4;

import java.util.ArrayList;

public class JIRA181<T extends Number> extends ArrayList<T> {
    /** default serialVersionUID */
    private static final long serialVersionUID = 1L;

    public @interface Condition {
    	Class<? extends ICondition> condition();
    }

    public @interface Condition2 {
    	Class<?> condition();
    }

    @Condition(condition = B.class)
    public Object aField;

    @Condition2(condition = B[].class)
    public Object aField2;

    public interface ICondition {
        boolean match(Object src);
    }

    private class B implements ICondition {
        public boolean match(Object src) {
            return JIRA181.this.size() > 0;
        }
    }
}
