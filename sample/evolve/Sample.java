package sample.evolve;

/**
 * This is a sample class used by Transformer.
 */
public class Sample {
    public static Class _version;

    public static Object make(Object[] args) {
	return VersionManager.make(_version, args);
    }
}
