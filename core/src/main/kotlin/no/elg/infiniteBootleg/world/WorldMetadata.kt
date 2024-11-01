package no.elg.infiniteBootleg.world

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Timer
import no.elg.infiniteBootleg.util.generateUUIDFromLong
import no.elg.infiniteBootleg.world.loader.WorldLoader.getWorldFolder

/**
 * Additional fields of a world not essential to the world itself to
 */
data class WorldMetadata(
  val name: String,
  val seed: Long,
  var spawn: Long,
  var isTransient: Boolean,
  var isLoaded: Boolean = false
) : Disposable {

  val uuid: String = generateUUIDFromLong(seed).toString()
  var worldFolder: FileHandle? = null
    /**
     * @return The current folder of the world or `null` if no disk should be used
     */
    get() {
      if (field == null) {
        field = getWorldFolder(uuid)
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
}
