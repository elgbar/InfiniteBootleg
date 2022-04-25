package no.elg.infiniteBootleg;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Collections;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.args.ProgramArgs;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import no.elg.infiniteBootleg.console.ConsoleLogger;
import no.elg.infiniteBootleg.util.CancellableThreadScheduler;
import no.elg.infiniteBootleg.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public abstract class CommonMain extends ApplicationAdapter implements Main {

  private static Main inst;

  protected final boolean test;
  @NotNull protected final CancellableThreadScheduler scheduler;
  @NotNull protected ConsoleHandler console;

  protected CommonMain(boolean test, @Nullable ProgramArgs progArgs) {
    if (progArgs != null) {
      progArgs.dispose();
    }
    synchronized (INST_LOCK) {
      if (inst != null) {
        throw new IllegalStateException("A main instance have already be declared");
      }
      inst = this;
    }
    this.test = test;
    scheduler = new CancellableThreadScheduler(Settings.schedulerThreads);
  }

  @NotNull
  static Main inst() {
    return inst;
  }

  static {
    // use unique iterators
    Collections.allocateIterators = true;
  }

  @Override
  public void create() {
    console = new ConsoleHandler();
    Gdx.app.setApplicationLogger(console);
    Gdx.app.setLogLevel(test || Settings.debug ? Application.LOG_DEBUG : Application.LOG_INFO);

    console.setAlpha(0.85f);
    console.log(LogLevel.SUCCESS, "Version #" + Util.getVersion());
    console.log(
        "You can also start the program with arguments for '--help' or '-?' as arg to see all possible options");
  }

  @Override
  public @NotNull ConsoleHandler getConsole() {
    return console;
  }

  @Override
  public @NotNull CancellableThreadScheduler getScheduler() {
    return scheduler;
  }

  @Override
  public boolean isNotTest() {
    return !test;
  }

  @Override
  public @NotNull ConsoleLogger getConsoleLogger() {
    return console;
  }

  @Override
  public void dispose() {
    console.dispose();
    scheduler.shutdown();
  }
}
