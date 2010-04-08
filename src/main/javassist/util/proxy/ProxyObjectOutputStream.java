package javassist.util.proxy;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

/**
 * An input stream class which knows how to serialize proxies created via {@link ProxyFactory}. It must
 * be used when serialising proxies created from a proxy factory configured with
 * {@link ProxyFactory#useWriteReplace} set to false. Subsequent deserialization of the serialized data
 * must employ a {@link ProxyObjectInputStream}
 */
public class ProxyObjectOutputStream extends ObjectOutputStream
{
    /**
     * create an output stream which can be used to serialize an object graph which includes proxies created
     * using class ProxyFactory
     * @param out
     * @throws IOException whenever ObjectOutputStream would also do so
     * @throws SecurityException whenever ObjectOutputStream would also do so
     * @throws NullPointerException if out is null
     */
    public ProxyObjectOutputStream(OutputStream out) throws IOException
    {
        super(out);
    }

    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        Class cl = desc.forClass();
        if (ProxyFactory.isProxyClass(cl)) {
            writeBoolean(true);
            Class superClass = cl.getSuperclass();
            Class[] interfaces = cl.getInterfaces();
            byte[] signature = ProxyFactory.getFilterSignature(cl);
            String name = superClass.getName();
            writeObject(name);
            // we don't write the marker interface ProxyObject
            writeInt(interfaces.length - 1);
            for (int i = 0; i < interfaces.length; i++) {
                Class interfaze = interfaces[i];
                if (interfaze != ProxyObject.class) {
                    name = interfaces[i].getName();
                    writeObject(name);
                }
            }
            writeInt(signature.length);
            write(signature);
        } else {
            writeBoolean(false);
            super.writeClassDescriptor(desc);
        }
    }
}
