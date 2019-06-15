package no.elg.infiniteBootleg;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author Elg
 */
public class ProgramArgsTest {

    @Before
    public void setUp() throws Exception {
        Main.renderGraphic = true;
    }

    @Ignore
    @Test
    public void headless() {
        ProgramArgs.executeArgs(new String[] {"-headless"});
        assertFalse(Main.renderGraphic);
    }

    @Ignore
    @Test
    public void handlesRandomCasing() {
        ProgramArgs.executeArgs(new String[] {"-hEadlESS"});
        assertFalse(Main.renderGraphic);
    }

    @Test
    public void handleInvalidArgs() {
        ProgramArgs.executeArgs(new String[] {"-hue", "he"});
    }
}
