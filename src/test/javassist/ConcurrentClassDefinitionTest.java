package javassist;

import javassist.bytecode.ClassFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ConcurrentClassDefinitionTest {

  @Parameterized.Parameter
  public int N;

  @Parameterized.Parameters
  public static Object[] data() {
    return new Object[] {
            1, // single threaded - should pass
            Runtime.getRuntime().availableProcessors() * 2
    };
  }

  @Test
  public void showDefineClassRaceCondition() throws Exception{
    Worker[] workers = new Worker[N];
    for (int i = 0; i < N; i++) {
      workers[i] = new Worker(N + "_ " + i, 100);
      workers[i].start();
    }
    for (Worker w : workers) {
      w.join();
    }
    for (Worker w : workers) {
      if (w.e != null) {
        throw w.e;
      }
    }
  }

  private static class Worker extends Thread {
    String id;
    int count;
    Exception e;

    Worker(String id, int count) {
      this.id = id;
      this.count = count;
    }

    @Override
    public void run() {
      try {
        for (int i = 0; i < count; i++) {
          Class c = makeClass(id + "_" + i);
          assert c != null;
        }
      } catch (Exception e) {
        this.e = e;
      }
    }

    @Override
    public void interrupt() {
      super.interrupt();
    }
  }

  private static Class makeClass(String id) throws Exception {
    ClassFile cf = new ClassFile(
            false, "com.example.JavassistGeneratedClass_" + id, null);
    ClassPool classPool = ClassPool.getDefault();
    return classPool.makeClass(cf).toClass();
  }


}
