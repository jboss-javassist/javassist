package test3;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface ChibaAnnotation {
    String name();
    String version();
    String description();
    String interfaceName();
}
