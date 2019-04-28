package no.elg.infiniteBootleg;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Elg
 */
public class ProgramArgsTest {

    @Test
    public void headless() {
        Main.HEADLESS = false;
        ProgramArgs.executeArgs(new String[] {"-headless"});
        assertTrue(Main.HEADLESS);
    }

    @Test
    public void handlesRandomCase() {
        Main.HEADLESS = false;
        ProgramArgs.executeArgs(new String[] {"-hEadlESS"});
        assertTrue(Main.HEADLESS);
    }

    @Test
    public void handleInvalidArgs() {
        ProgramArgs.executeArgs(new String[] {"-hue", "he"});
    }
}
