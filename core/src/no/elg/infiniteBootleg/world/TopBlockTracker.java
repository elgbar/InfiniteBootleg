package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntSet;
import org.jetbrains.annotations.NotNull;

/**
 * A class that track the top block of each x coordinate in a world
 *
 * @author Elg
 */
public class TopBlockTracker {

    private final World world;
    private final IntIntMap topBlocks; //Key: world x | Value: chunk y of the top block
    private final IntMap<IntSet> chunksLocs; //Key: chunk x | Value: All local x's with this chunk as top loc

    public TopBlockTracker(@NotNull World world) {
        this.world = world;
        topBlocks = new IntIntMap();
        chunksLocs = new IntMap<>();
    }

    public void setTopBlock(int worldX, int worldY) {
//        int chunkX = CoordUtil.worldToChunk(worldX);
//        int chunkY = CoordUtil.worldToChunk(worldY);
//        if (topBlocks.containsKey(worldX)) {
//            IntSet cs = chunksLocs.get(chunkX);
//            cs.remove(worldX);
//            if (cs.isEmpty()) {
//                chunksLocs.remove(worldX);
//                world.getChunk(chunkX, topBlocks.get(worldX, 0)).setAllowUnload(true);
//                System.out.printf("Chunk (%d,%d) is no longer top chunk for world x: %d%n", chunkX, chunkY, worldX);
//            }
//        }
//        world.getChunk(chunkX, chunkY).setAllowUnload(false);
//        topBlocks.put(worldX, chunkY);
//        System.out.printf("New top chunk for world x '%d' is Chunk (%d,%d)%n", worldX, chunkX, chunkY);
    }

    /**
     * Update the top block of the given world coordinate. If there are no loaded chunks (or all chunks are air) then the top
     * block will not be updated
     *
     * @param worldX
     */
    public void update(int worldX) {


//        int chunkX = CoordUtil.worldToChunk(worldX);
//
//
//        int vertTop = world.getRender().getChunksInView()[WorldRender.VERT_END];
//        int vertBottom = world.getRender().getChunksInView()[WorldRender.VERT_START];
//
//        int horzLeft = world.getRender().getChunksInView()[WorldRender.VERT_START];
//        int horzRight = world.getRender().getChunksInView()[WorldRender.VERT_END];
//
//        //The given world world
//        if (horzLeft < chunkX || horzRight > chunkX) {
//            return;
//        }
//
//        //TODO compute with current top chunk
////        Chunk currChunk = topBlocks.get(worldX);
////        if (currChunk != null && !currChunk.isAllAir()) {
////
////        }
//
//        Integer maxY = null;
//        outer:
//        for (int chunkY = vertTop - 1; chunkY >= vertBottom; chunkY--) {
//            for (int localY = chunkY; localY < Chunk.CHUNK_SIZE; localY++) {
//                int worldY = CoordUtil.chunkToWorld(chunkY, localY);
//                if (world.isAir(worldX, worldY + 1) && !world.isAir(worldX, worldY + 1)) {
//                    maxY = chunkY;
//                    break outer;
//                }
//            }
//
//        }
//        if (maxY == null) {
//
//        }
    }
}
