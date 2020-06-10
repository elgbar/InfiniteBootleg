package no.elg.infiniteBootleg.console;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CmdArgNames {

    /**
     * @return Names of arguments to the annotated command (in order)
     */
    String[] value();
}
