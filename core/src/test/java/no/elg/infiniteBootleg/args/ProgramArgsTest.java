package no.elg.infiniteBootleg.args;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.TestGraphic;
import org.junit.Assert;
import static org.junit.Assert.fail;
import org.junit.Test;

public class ProgramArgsTest extends TestGraphic {

    @Test
    public void headless() {
        Settings.renderGraphic = true;
        new ProgramArgs(new String[] {"--headless"});
        Assert.assertFalse(Settings.renderGraphic);
    }

    @Test
    public void noLoad() {
        Settings.loadWorldFromDisk = true;
        new ProgramArgs(new String[] {"--no_load"});
        Assert.assertFalse(Settings.loadWorldFromDisk);
    }

    @Test
    public void worldSeed() {
        new ProgramArgs(new String[] {"--world_seed=test123"});
        Assert.assertEquals("test123".hashCode(), Settings.worldSeed);
    }

    @Test
    public void handlesRandomCasing() {
        Settings.renderGraphic = true;
        new ProgramArgs(new String[] {"--hEadlESS"});
        Assert.assertFalse(Settings.renderGraphic);
    }

    @Test
    public void handleInvalidArgs() {
        new ProgramArgs(new String[] {"-hue", "he"});
    }

    @Test
    public void threads() {
        ProgramArgs pa = new ProgramArgs(new String[0]);
        Assert.assertTrue(pa.threads("0"));
        Assert.assertTrue(pa.threads("1"));
        Assert.assertTrue(pa.threads("32"));
        Assert.assertFalse(pa.threads("-32"));
        Assert.assertFalse(pa.threads("aaa"));
        Assert.assertFalse(pa.threads(""));
        pa.dispose();
    }

    @Test
    public void noDuplicateAltSwitches() {

        Map<Character, String> known = new HashMap<>();
        for (Method method : ProgramArgs.class.getDeclaredMethods()) {
            Argument a = method.getAnnotation(Argument.class);
            if (a != null) {
                if (a.alt() != '\0' && known.containsKey(a.alt())) {
                    String otherMethod = known.get(a.alt());
                    fail("Duplicate alternative switch found! Both the method '" + method.getName() + "' and '" +
                         otherMethod + "' have the switch '" + a.alt() + "'");
                }
                known.put(a.alt(), method.getName());
            }
        }

    }
}
