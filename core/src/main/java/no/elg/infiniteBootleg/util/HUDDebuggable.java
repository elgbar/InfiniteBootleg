package no.elg.infiniteBootleg.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public interface HUDDebuggable {

    /**
     * @return Information to display on the debug HUD
     */
    @NotNull
    default String hudDebug() {
        return "";
    }

}
