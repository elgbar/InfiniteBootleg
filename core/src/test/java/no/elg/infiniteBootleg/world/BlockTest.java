package no.elg.infiniteBootleg.world;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Elg
 */
public class BlockTest {


    @Test
    public void correctType() {
        for (Material mat : Material.values()) {
            Block b = mat.create(0, 0, null);
            Assert.assertEquals(mat, b.getMaterial());
        }

    }
}
