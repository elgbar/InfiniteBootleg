package no.elg.infiniteBootleg.console;

import com.badlogic.gdx.utils.Disposable;
import java.io.Console;
import java.util.Scanner;
import no.elg.infiniteBootleg.Main;
import org.jetbrains.annotations.NotNull;

/** Read input from {@link System#console()} or {@link System#in} if no console exists. */
public class SystemConsoleReader implements Runnable, Disposable {

  private final ConsoleHandler consoleHandler;
  private boolean running;

  public SystemConsoleReader(@NotNull ConsoleHandler consoleHandler) {
    this.consoleHandler = consoleHandler;

    Thread thread = new Thread(this, "System Console Reader Thread");
    thread.setDaemon(true);
    thread.start();
  }

  @Override
  public void run() {
    if (running) {
      return;
    }
    running = true;
    Console console = System.console();
    try (Scanner scanner =
        console != null ? new Scanner(console.reader()) : new Scanner(System.in)) {
      while (running) {
        String read;
        try {
          read = scanner.nextLine();
          Main.inst().getScheduler().executeSync(() -> consoleHandler.execCommand(read));
        } catch (Exception e) {
          dispose();
        }
      }
    }
  }

  @Override
  public void dispose() {
    running = false;
  }
}
