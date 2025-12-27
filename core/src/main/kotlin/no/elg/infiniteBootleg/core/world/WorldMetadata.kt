package no.elg.infiniteBootleg.core.world

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Timer
import kotlinx.coroutines.CoroutineDispatcher
import no.elg.infiniteBootleg.core.util.generateUUIDFromLong
import no.elg.infiniteBootleg.core.world.loader.WorldLoader
import java.util.concurrent.atomic.AtomicInteger

/**
 * Additional fields of a world not essential to the world itself.
 * This helps with the java object layout (JOL) size being below 64 bytes for better memory efficiency.
 */
data class WorldMetadata(
  val name: String,
  val seed: Long,
  var spawn: Long,
  var isTransient: Boolean,
  var isLoaded: Boolean = false,
  var isDisposed: Boolean = false,
  val box2dCoroutineDispatcher: CoroutineDispatcher,
  val worldTickCoroutineDispatcher: CoroutineDispatcher
) : Disposable {

  val uuid: String = generateUUIDFromLong(seed).toString()
  var worldFolder: FileHandle? = null
    /**
     * @return The current folder of the world or `null` if no disk should be used
     */
    get() {
      if (field == null) {
        field = WorldLoader.getWorldFolder(uuid)
      }
      return field
    }

  var saveTask: Timer.Task? = null
    set(value) {
      field?.cancel()
      field = value
    }

  override fun dispose() {
    saveTask = null
  }

  val chunkReads = AtomicInteger(0)
  val chunkWrites = AtomicInteger(0)
}
