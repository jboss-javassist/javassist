package sample.duplicate;

import javassist.tools.reflect.*;

public class DuplicatedObject extends Metaobject {
    private DuplicatedObject backup;

    // if a base-level object is created, this metaobject creates
    // a copy of the base-level object.

    public DuplicatedObject(Object self, Object[] args) {
	super(self, args);
	ClassMetaobject clazz = getClassMetaobject();
	if (clazz.isInstance(args[0]))
	    backup = null;	// self is a backup object.
	else {
	    Object[] args2 = new Object[1];
	    args2[0] = self;
	    try {
		Metalevel m = (Metalevel)clazz.newInstance(args2);
		backup = (DuplicatedObject)m._getMetaobject();
	    }
	    catch (CannotCreateException e) {
		backup = null;
	    }
	}
    }

    public Object trapMethodcall(int identifier, Object[] args) 
	throws Throwable
    {
	Object obj = super.trapMethodcall(identifier, args);
	if (backup != null)
	    backup.trapMethodcall(identifier, args);

	return obj;
    }
}
