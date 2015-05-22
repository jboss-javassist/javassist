package sample.evolve;

import java.util.Hashtable;
import java.lang.reflect.*;

/**
 * Runtime system for class evolution
 */
public class VersionManager {
    private static Hashtable versionNo = new Hashtable();

    public final static String latestVersionField = "_version";

    /**
     * For updating the definition of class my.X, say:
     * 
     * VersionManager.update("my.X");
     */
    public static void update(String qualifiedClassname)
            throws CannotUpdateException {
        try {
            Class c = getUpdatedClass(qualifiedClassname);
            Field f = c.getField(latestVersionField);
            f.set(null, c);
        }
        catch (ClassNotFoundException e) {
            throw new CannotUpdateException("cannot update class: "
                    + qualifiedClassname);
        }
        catch (Exception e) {
            throw new CannotUpdateException(e);
        }
    }

    private static Class getUpdatedClass(String qualifiedClassname)
            throws ClassNotFoundException {
        int version;
        Object found = versionNo.get(qualifiedClassname);
        if (found == null)
            version = 0;
        else
            version = ((Integer)found).intValue() + 1;

        Class c = Class.forName(qualifiedClassname + "$$" + version);
        versionNo.put(qualifiedClassname, new Integer(version));
        return c;
    }

    /*
     * initiaVersion() is used to initialize the _version field of the updatable
     * classes.
     */
    public static Class initialVersion(String[] params) {
        try {
            return getUpdatedClass(params[0]);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("cannot initialize " + params[0]);
        }
    }

    /**
     * make() performs the object creation of the updatable classes. The
     * expression "new <updatable class>" is replaced with a call to this
     * method.
     */
    public static Object make(Class clazz, Object[] args) {
        Constructor[] constructors = clazz.getConstructors();
        int n = constructors.length;
        for (int i = 0; i < n; ++i) {
            try {
                return constructors[i].newInstance(args);
            }
            catch (IllegalArgumentException e) {
                // try again
            }
            catch (InstantiationException e) {
                throw new CannotCreateException(e);
            }
            catch (IllegalAccessException e) {
                throw new CannotCreateException(e);
            }
            catch (InvocationTargetException e) {
                throw new CannotCreateException(e);
            }
        }

        throw new CannotCreateException("no constructor matches");
    }
}
