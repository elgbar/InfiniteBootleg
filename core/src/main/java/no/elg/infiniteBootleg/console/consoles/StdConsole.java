package no.elg.infiniteBootleg.console.consoles;

import com.strongjoshua.console.HeadlessConsole;
import no.elg.infiniteBootleg.console.HelpfulConsoleHelpUtil;

/** A console that reads input from standard in */
public class StdConsole extends HeadlessConsole {

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
}
