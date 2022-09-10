package test3;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Anno2 {
    int[] i() default { 1, 2, 3 };
    String[] str() default { "a", "b", "c" };
    ColorName color() default ColorName.RED;
    ColorName[] color2() default { ColorName.BLUE };
}
