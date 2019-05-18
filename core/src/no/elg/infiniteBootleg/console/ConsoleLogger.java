package no.elg.infiniteBootleg.console;

import com.strongjoshua.console.LogLevel;

/**
 * @author Elg
 */
public interface ConsoleLogger {

    void logf(String msg, Object... objs);

    void logf(LogLevel level, String msg, Object... objs);

    /*
     * (non-Javadoc)
     *
     * @see com.strongjoshua.console.Console#log(java.lang.String, com.strongjoshua.console.GUIConsole.LogLevel)
     */
    void log(String msg, LogLevel level);

    void log(String msg);
}
