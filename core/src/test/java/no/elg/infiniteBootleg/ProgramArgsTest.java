package no.elg.infiniteBootleg;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author Elg
 */
public class ProgramArgsTest extends TestGraphic {

    @Test
    public void headless() {
        Main.renderGraphic = true;
        ProgramArgs.executeArgs(new String[] {"-headless"});
        assertFalse(Main.renderGraphic);
    }

    @Test
    public void handlesRandomCasing() {
        Main.renderGraphic = true;
        ProgramArgs.executeArgs(new String[] {"-hEadlESS"});
        assertFalse(Main.renderGraphic);
    }

    @Test
    public void handleInvalidArgs() {
        ProgramArgs.executeArgs(new String[] {"-hue", "he"});
    }
}
