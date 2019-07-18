package no.elg.infiniteBootleg.console;

import com.strongjoshua.console.HeadlessConsole;
import no.elg.infiniteBootleg.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A console that reads input from standard in
 */
public class StdConsole extends HeadlessConsole implements Runnable {

    private BufferedReader in;
    private Thread thread;
    private boolean running;

    public StdConsole() {
        running = true;

        thread = new Thread(this, "Headless Console Reader Thread");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        in = new BufferedReader(new InputStreamReader(System.in));
        while (running) {
            try {
                String read = in.readLine();
                Main.SCHEDULER.executeSync(() -> execCommand(read));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        running = false;
    }
}
