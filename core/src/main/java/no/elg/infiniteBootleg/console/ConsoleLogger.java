package no.elg.infiniteBootleg.console;

import com.badlogic.gdx.ApplicationLogger;
import com.strongjoshua.console.LogLevel;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public interface ConsoleLogger extends ApplicationLogger {

  /**
   * Log a level with {@link LogLevel#DEFAULT} loglevel
   *
   * @param msg The message to log
   * @param objs The object to format
   * @see String#format(String, Object...)
   */
  default void logf(@NotNull String msg, @NotNull Object... objs) {
    logf(LogLevel.DEFAULT, msg, objs);
  }

  /**
   * @param level The level to log at
   * @param msg The message to log
   * @param objs The object to format
   * @see String#format(String, Object...)
   */
  default void logf(LogLevel level, @NotNull String msg, @NotNull Object... objs) {
    log(level, String.format(msg, objs));
  }

  /**
   * @param level The level to log at
   * @param msg The message to log
   */
  void log(@NotNull LogLevel level, @NotNull String msg);

  default void success(@NotNull String msg) {
    log(LogLevel.SUCCESS, msg);
  }

  /**
   * Log a level with {@link LogLevel#DEFAULT} loglevel
   *
   * @param msg The message to log
   */
  default void log(@NotNull String msg) {
    log(LogLevel.DEFAULT, msg);
  }

  default void success(@NotNull String msg, @NotNull Object... objs) {
    logf(LogLevel.SUCCESS, msg, objs);
  }

  @Override
  default void log(@NotNull String tag, @NotNull String message) {
    log("[" + tag + "] " + message);
  }

  default void warn(@NotNull String message) {
    error("WARN", message);
  }

  @Override
  default void log(@NotNull String tag, @NotNull String message, @Nullable Throwable exception) {
    log(tag, message);
    if (exception != null) {
      exception.printStackTrace(System.out);
    }
  }

  default void warn(@NotNull String message, @NotNull Object... objs) {
    error("WARN", message, objs);
  }

  default void error(@NotNull String tag, @NotNull String message, @NotNull Object... objs) {
    logf(LogLevel.ERROR, "[" + tag + "] " + message, objs);
  }

  default void warn(@NotNull String tag, @NotNull String message, @NotNull Object... objs) {
    logf(LogLevel.ERROR, "WARN [" + tag + "] " + message, objs);
  }

  default void error(@NotNull String message) {
    log(LogLevel.ERROR, message);
  }

  @Override
  default void error(@NotNull String tag, @NotNull String message) {
    log(LogLevel.ERROR, "[" + tag + "] " + message);
  }

  @Override
  default void error(@NotNull String tag, @NotNull String message, @Nullable Throwable exception) {
    error(tag, message);
    if (exception != null) {
      exception.printStackTrace(System.err);
    }
  }

  @Override
  default void debug(@NotNull String tag, @NotNull String message) {
    log(LogLevel.DEFAULT, "DBG [" + tag + "] " + message);
  }

  default void debug(@NotNull String tag, @NotNull Function0<String> message) {
    log(LogLevel.DEFAULT, "DBG [" + tag + "] " + message.invoke());
  }

  @Override
  default void debug(@NotNull String tag, @NotNull String message, @Nullable Throwable exception) {
    debug(tag, message);
    if (exception != null) {
      exception.printStackTrace(System.out);
    }
  }
}
