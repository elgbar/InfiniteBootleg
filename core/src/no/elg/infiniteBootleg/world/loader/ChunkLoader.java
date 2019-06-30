package no.elg.infiniteBootleg.world.loader;

import com.badlogic.gdx.files.FileHandle;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
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
     * @param chunkLoc
     *     The location of the chunk
     *
     * @return If a chunk at the given location exists
     */
    public boolean existsOnDisk(@NotNull Location chunkLoc) {
        if (!Main.loadWorldFromDisk) { return false; }
        FileHandle chunkFile = Chunk.geChunkFile(world, chunkLoc);
        return chunkFile != null && chunkFile.exists();
    }

    /**
     * Load the chunk at the given chunk location
     *
     * @param chunkLoc
     *     The location of the chunk (in chunk view)
     *
     * @return The loaded chunk
     */
    public Chunk load(@NotNull Location chunkLoc) {
        if (existsOnDisk(chunkLoc)) {
            Chunk chunk = new Chunk(world, chunkLoc);
            //noinspection ConstantConditions checked in existsOnDisk
            chunk.assemble(chunk.getChunkFile().readBytes());
            return chunk;
        }
        else {
            return generator.generate(world, chunkLoc);
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
}
