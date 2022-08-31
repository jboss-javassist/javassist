package test4;

public class JIRA181b {
	public @interface Condition {
    	Class<?> condition();
    }

	@Condition(condition = String.class)
	public Object aField;
	@Condition(condition = void.class)
	public Object aField2;
}
