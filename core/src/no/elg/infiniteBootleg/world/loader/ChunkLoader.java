package no.elg.infiniteBootleg.world.loader;

import com.badlogic.gdx.files.FileHandle;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * Handle saving and loading of chunks.
 * <p>
 * If a chunk is saved to disk then that chunk will be loaded (assuming {@link Main#loadWorldFromDisk} is {@code true}) Otherwise
 * it will be generated with the given {@link ChunkGenerator}
 *
 * @author Elg
 */
public class ChunkLoader {

    private final World world;
    private final ChunkGenerator generator;

    public ChunkLoader(@NotNull World world, @NotNull ChunkGenerator generator) {
        this.world = world;
        this.generator = generator;
    }

    /**
     * @param chunkX
     *     The y coordinate of the chunk (in chunk view)
     * @param chunkY
     *     The x coordinate of the chunk (in chunk view)
     *
     * @return If a chunk at the given location exists
     */
    public boolean existsOnDisk(int chunkX, int chunkY) {
        if (!Main.loadWorldFromDisk) { return false; }
        FileHandle chunkFile = Chunk.getChunkFile(world, chunkX, chunkY);
        return chunkFile != null && chunkFile.exists();
    }

    /**
     * Load the chunk at the given chunk location
     *
     * @param chunkX
     *     The y coordinate of the chunk (in chunk view)
     * @param chunkY
     *     The x coordinate of the chunk (in chunk view)
     *
     * @return The loaded chunk
     */
    public Chunk load(int chunkX, int chunkY) {
        if (existsOnDisk(chunkX, chunkY)) {
            Chunk chunk = new Chunk(world, chunkX, chunkY);
            //noinspection ConstantConditions checked in existsOnDisk
            chunk.assemble(chunk.getChunkFile().readBytes());
            return chunk;
        }
        else {
            return generator.generate(world, chunkX, chunkY);
        }
    }

    public void save(@NotNull Chunk chunk) {
        if (Main.loadWorldFromDisk && chunk.isModified()) {
            //only save if modified
            FileHandle fh = chunk.getChunkFile();
            if (fh == null) { return; }
            fh.writeBytes(chunk.disassemble(), false);
        }
    }

    public ChunkGenerator getGenerator() {
        return generator;
    }
}
