package test5;

public class NestHost3 {
  private NestHost3(Builder builder) {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    public Builder() {
    }

    public NestHost3 build() {
      return new NestHost3(this);
    }
  }
}
