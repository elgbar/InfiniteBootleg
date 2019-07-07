package no.elg.infiniteBootleg.world;

import box2dLight.PointLight;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.IntMap;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.PointLightPool;
import org.jetbrains.annotations.NotNull;

/**
 * A class that track the top block of each x coordinate in a world
 *
 * @author Elg
 */
public class TopBlockTracker {

    private final World world;
    private final IntIntMap topBlocks; //Key: world x | Value: world y of the top block
    private final IntMap<Array<PointLight>> lightMap; //Key: world x | Value: The light(s) at this location

    public TopBlockTracker(@NotNull World world) {
        this.world = world;
        topBlocks = new IntIntMap();
        lightMap = new IntMap<>(Chunk.CHUNK_SIZE);
    }

    /**
     * @param worldX
     *     The x coordinate in world view
     * @param worldY
     *     The potential new top world block
     */
    public void update(int worldX, int worldY) {
        int knownY = topBlocks.get(worldX, Integer.MIN_VALUE);
        if (worldY == knownY) {
            return;
        }
        else if (worldX < knownY) {
            for (int newWorldY = knownY; newWorldY > worldY; newWorldY--) {
                Block rel = world.getBlock(worldX, newWorldY);
                if (rel.getMaterial().blocksLight()) {
                    updateLight(worldX, knownY, newWorldY);
                    return;
                }
            }
        }
        updateLight(worldX, knownY, worldY);
    }

    private void updateLight(int worldX, int oldWorldY, int newWorldY) {
        if (Main.renderGraphic) {
            Array<PointLight> lights = lightMap.get(worldX);
            if (lights != null) {
                //clean up old lights
                for (PointLight light : lights) {
                    PointLightPool.inst.free(light);
                }
                lightMap.clear(Math.abs(oldWorldY - newWorldY));
            }
            else {
                lights = new Array<>();
                lightMap.put(worldX, lights);
            }

            if (oldWorldY != Integer.MIN_VALUE) {
                //create new lights
                for (int y = oldWorldY; y < newWorldY; y++) {
//                    PointLight light = PointLightPool.inst.obtain();
//                    light.setPosition(worldX, y);
//                    lights.add(light);
                }
            }
        }
        topBlocks.put(worldX, worldX);
    }
}
