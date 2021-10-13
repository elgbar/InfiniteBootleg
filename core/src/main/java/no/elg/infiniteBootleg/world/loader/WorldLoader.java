package no.elg.infiniteBootleg.world.loader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.util.Random;
import java.util.UUID;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.protobuf.Proto;
import no.elg.infiniteBootleg.util.ZipUtils;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import no.elg.infiniteBootleg.world.generator.FlatChunkGenerator;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class WorldLoader {

    public static final String WORLD_INFO_PATH = "world.dat";

    public static UUID getUUIDFromSeed(long seed) {
        byte[] uuidSeed = new byte[128];
        var random = new Random(seed);
        random.nextBytes(uuidSeed);
        return UUID.nameUUIDFromBytes(uuidSeed);
    }

    @Nullable
    private static World loadWorld(long seed) {
        var uuid = getUUIDFromSeed(seed);
        var worldFolder = getWorldFolder(uuid);
        FileHandle worldZip = getWorldZip(worldFolder);

        if (!Settings.loadWorldFromDisk || worldFolder == null) {
            return null;
        }
        if (worldZip == null || !worldZip.exists()) {
            Main.logger().log("No world save found");
            return null;
        }
        worldFolder.deleteDirectory();
        ZipUtils.unzip(worldFolder, worldZip);

        return null;
    }

    public static FileHandle getWorldZip(FileHandle folder) {
        return folder != null ? folder.parent().child(folder.name() + ".zip") : null;
    }

    public static FileHandle getWorldFolder(@NotNull UUID uuid) {
        return Gdx.files.external(Main.WORLD_FOLDER + uuid);
    }

    public static ChunkGenerator generatorFromProto(@NotNull Proto.World protoWorld) {
        return switch (protoWorld.getGenerator()) {
            case PERLIN, UNRECOGNIZED -> new PerlinChunkGenerator(protoWorld.getSeed());
            case FLAT -> new FlatChunkGenerator();
            case EMPTY -> new EmptyChunkGenerator();
        };
    }
}
