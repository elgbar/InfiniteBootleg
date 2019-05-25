package no.elg.infiniteBootleg;

import no.elg.infiniteBootleg.util.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author Elg
 */
@SuppressWarnings("unused")
public class ProgramArgs {

    public static void executeArgs(String[] args) {
        Map<String, String> options = Util.interpreterArgs(args);

        for (Map.Entry<String, String> entry : options.entrySet()) {
            try {
                Method method = ProgramArgs.class.getMethod(entry.getKey().toLowerCase(), String.class);
                if (method != null) {
                    method.invoke(null, entry.getValue());
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
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
        Main.renderGraphic = false;
    }
}
