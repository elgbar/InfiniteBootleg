package no.elg.infiniteBootleg.args;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Elg
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Argument {

    /**
     * @return Help string
     */
    String value();

    /**
     * @return Single char alias
     */
    char alt() default '\0';
}
