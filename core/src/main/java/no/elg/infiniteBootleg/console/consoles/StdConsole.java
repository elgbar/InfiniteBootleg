package no.elg.infiniteBootleg.console.consoles;

import com.strongjoshua.console.HeadlessConsole;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import no.elg.infiniteBootleg.console.HelpfulConsoleHelpUtil;
import org.jetbrains.annotations.NotNull;

/** A console that reads input from standard in */
public class StdConsole extends HeadlessConsole implements Runnable {

  private final ConsoleHandler consoleHandler;
  private boolean running;

  public StdConsole(@NotNull ConsoleHandler consoleHandler) {
    this.consoleHandler = consoleHandler;
    running = true;

    Thread thread = new Thread(this, "Headless Console Reader Thread");
    thread.setDaemon(true);
    thread.start();
  }

  @Override
  public void run() {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    while (running) {
      try {
        String read = in.readLine();
        if (read != null) {
          Main.inst().getScheduler().executeSync(() -> consoleHandler.execCommand(read));
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void printHelp(String command) {
    HelpfulConsoleHelpUtil.printHelp(this, exec, command);
  }

  @Override
  public void printCommands() {
    HelpfulConsoleHelpUtil.printCommands(this, exec);
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  @Override
  public void dispose() {
    super.dispose();
    running = false;
  }
}
