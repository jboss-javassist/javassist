package sample.duplicate;

/*
  Runtime metaobject (JDK 1.2 or later only).

  With the javassist.tools.reflect package, the users can attach a metaobject
  to an object.  The metaobject can control the behavior of the object.
  For example, you can implement fault tolerancy with this ability.  One
  of the implementation techniques of fault tolernacy is to make a copy
  of every object containing important data and maintain it as a backup.
  If the machine running the object becomes down, the backup object on a
  different machine is used to continue the execution.

  To make the copy of the object a real backup, all the method calls to
  the object must be also sent to that copy.  The metaobject is needed
  for this duplication of the method calls.  It traps every method call
  and invoke the same method on the copy of the object so that the
  internal state of the copy is kept equivalent to that of the original
  object.

  First, run sample.duplicate.Viewer without a metaobject.

  % java sample.duplicate.Viewer

  This program shows a ball in a window.

  Then, run the same program with a metaobject, which is an instance
  of sample.duplicate.DuplicatedObject.

  % java sample.duplicate.Main

  You would see two balls in a window.  This is because
  sample.duplicate.Viewer is loaded by javassist.tools.reflect.Loader so that
  a metaobject would be attached.
*/
public class Main {
    public static void main(String[] args) throws Throwable {
	javassist.tools.reflect.Loader cl = new javassist.tools.reflect.Loader();
	cl.makeReflective("sample.duplicate.Ball",
			  "sample.duplicate.DuplicatedObject",
			  "javassist.tools.reflect.ClassMetaobject");
	cl.run("sample.duplicate.Viewer", args);
    }
}
