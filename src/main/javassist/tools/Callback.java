package javassist.tools;

import java.util.HashMap;
import java.util.UUID;

/**
 * Creates bytecode that when executed calls back to the objects result method.
 */
public abstract class Callback {

    public static HashMap<String, Callback> callbacks = new HashMap<String, Callback>();

    private final String callbackCode;

    /**
     * Constructs a new <code>Callback</code> object.
     *
     * @param src         java code that returns one or many objects. if many objects
     *                    are returned they should be comma separated
     */
    public Callback(String src){
        String uuid = UUID.randomUUID().toString();
        callbacks.put(uuid, this);
        callbackCode = "((javassist.tools.Callback) javassist.tools.Callback.callbacks.get(\""+uuid+"\")).result(new Object[]{"+src+"});";
    }

    /**
     * Gets called when bytecode is executed
     *
     * @param objects     java code that returns one or many objects. if many objects
     *                    are returned they should be comma separated
     */
    public abstract void result(Object... objects);

    @Override
    public String toString(){
        return callbackCode;
    }
}
