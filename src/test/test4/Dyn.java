package test4;

public class Dyn {

  public static int test9(int i, String s) {
    return 9;
  }

  public int test8(int i, String s) {
    return 8;
  }

  public static Integer boot(String numberString)
      throws NoSuchMethodException, IllegalAccessException {
    return Integer.valueOf(numberString);
  }

  public Integer boot2(String numberString)
      throws NoSuchMethodException, IllegalAccessException {
    return Integer.valueOf(numberString);
  }
}
