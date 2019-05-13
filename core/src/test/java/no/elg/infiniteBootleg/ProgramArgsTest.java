package no.elg.infiniteBootleg;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author Elg
 */
public class ProgramArgsTest {

    @Before
    public void setUp() throws Exception {
        Main.RENDER_GRAPHIC = true;
    }

    @Test
    public void headless() {
        ProgramArgs.executeArgs(new String[] {"-headless"});
        assertFalse(Main.RENDER_GRAPHIC);
    }

    @Test
    public void handlesRandomCasing() {
        ProgramArgs.executeArgs(new String[] {"-hEadlESS"});
        assertFalse(Main.RENDER_GRAPHIC);
    }

    @Test
    public void handleInvalidArgs() {
        ProgramArgs.executeArgs(new String[] {"-hue", "he"});
    }
}
