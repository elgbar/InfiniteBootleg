package no.elg.infiniteBootleg;

import no.elg.infiniteBootleg.util.Util;
import no.kh498.util.Reflection;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * @author Elg
 */
@SuppressWarnings("unused")
public class ProgramArgs {

    public static void executeArgs(String[] args) {
        Map<String, String> options = Util.interpreterArgs(args);

        for (Map.Entry<String, String> entry : options.entrySet()) {
            Reflection.getMethod(ProgramArgs.class, entry.getKey().toLowerCase(), String.class).ifPresent(method -> {
                try {
                    method.invoke(null, entry.getValue());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    /*
     * Below this comment all arguments are computed.
     * A argument parser has a couple of requirement
     *
     * - It must be static
     * - It must have String as its one and only argument
     * - The name of the method must be lowercase
     *
     */


    private static void headless(String val) {
        Main.RENDER_GRAPHIC = false;
    }
}
