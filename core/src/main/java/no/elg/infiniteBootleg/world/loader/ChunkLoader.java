package no.elg.infiniteBootleg.world.loader;

import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.ChunkImpl;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handle saving and loading of chunks.
 * <p>
 * If a chunk is saved to disk then that chunk will be loaded (assuming {@link Settings#loadWorldFromDisk} is {@code
 * true}) Otherwise
 * it will be generated with the given {@link ChunkGenerator}
 *
 * @author Elg
 */
public class ChunkLoader {

    public static final String CHUNK_FOLDER = "chunks";
    private final World world;
    private final ChunkGenerator generator;

    public ChunkLoader(@NotNull World world, @NotNull ChunkGenerator generator) {
        this.world = world;
        this.generator = generator;
    }

    @Nullable
    public static FileHandle getChunkFile(@NotNull World world, int chunkX, int chunkY) {
        FileHandle worldFile = world.getWorldFolder();
        if (worldFile == null) {
            return null;
        }
        return worldFile.child(CHUNK_FOLDER + File.separator + chunkX + File.separator + chunkY);
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
            ChunkImpl chunk = new ChunkImpl(world, chunkX, chunkY);
            final FileHandle chunkFile = getChunkFile(world, chunkX, chunkY);
            if (chunk.assemble(chunkFile.readBytes())) {
                chunk.finishLoading();
                return chunk;
            }
            //Failed to assemble, generate new chunk
        }
        return generator.generate(world, chunkX, chunkY);
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
        if (!Settings.loadWorldFromDisk) {
            return false;
        }
        FileHandle chunkFile = getChunkFile(world, chunkX, chunkY);
        return chunkFile != null && chunkFile.exists();
    }

    public void save(@NotNull Chunk chunk) {
        if (Settings.loadWorldFromDisk && chunk.shouldSave() && chunk.isLoaded()) {
            //only save if valid and changed
            FileHandle fh = getChunkFile(world, chunk.getChunkX(), chunk.getChunkY());
            if (fh == null) {
                return;
            }
            fh.writeBytes(chunk.disassemble(), false);
        }
    }

    public ChunkGenerator getGenerator() {
        return generator;
    }
}
