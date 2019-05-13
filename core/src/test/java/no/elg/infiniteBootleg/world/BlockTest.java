package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.TestGraphic;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Elg
 */
public class BlockTest extends TestGraphic {


    @Test
    public void correctType() {
        for (Material mat : Material.values()) {
            Block b = mat.create(0, 0, null);
            Assert.assertEquals(mat, b.getMaterial());
        }

    }
}
