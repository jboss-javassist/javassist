package sample.evolve;

/**
 * Signals that VersionManager.newInstance() fails.
 */
public class CannotCreateException extends RuntimeException {
    public CannotCreateException(String s) {
	super(s);
    }

    public CannotCreateException(Exception e) {
	super("by " + e.toString());
    }
}
