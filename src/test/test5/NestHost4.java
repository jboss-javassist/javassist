package test5;

public class NestHost4 {
  public void test() {
    new InnerClass1().new InnerClass5();
  }

  private class InnerClass1 {
    private InnerClass1() {
      new InnerClass2();
    }

    private class InnerClass5 {
      private InnerClass5() {
        new InnerClass2().new InnerClass3();
      }
    }
  }

  private class InnerClass2 {

    private class InnerClass3 {

    }
  }
}
