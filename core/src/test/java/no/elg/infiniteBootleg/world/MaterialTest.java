package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.TestGraphic;
import no.elg.infiniteBootleg.world.generator.FlatChunkGenerator;
import org.junit.Test;

/**
 * @author Elg
 */
public class MaterialTest extends TestGraphic {

    @Test
    public void create() {
        World world = new World(new FlatChunkGenerator());
        for (Material material : Material.values()) {
            System.out.println("Trying to create " + material);
            material.create(world, 0, 0);
            System.out.println("Created " + material);
        }
    }
}
