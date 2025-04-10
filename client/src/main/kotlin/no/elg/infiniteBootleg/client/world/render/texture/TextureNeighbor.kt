package no.elg.infiniteBootleg.client.world.render.texture

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.collections.GdxArray
import no.elg.infiniteBootleg.client.world.textureRegion
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion.Companion.disallowedRotation
import java.util.EnumMap

private val logger = KotlinLogging.logger {}

object TextureNeighbor {

  private val textureNeighbors = mutableMapOf<Material, MutableList<NeighborTexture>>()

  fun getTexture(center: Material, neighbors: EnumMap<Direction, Material>): RotatableTextureRegion? =
    textureNeighbors[center]?.firstOrNull { it.matches(neighbors) }?.textureRegion ?: center.textureRegion

  fun getMaterialFromTextureName(materialName: String): Material? {
    val name = materialName.replace("_", "")
    val maybeMaterial = Material.valueOfOrNull(name)
    if (maybeMaterial == null) {
      logger.error { "Could not find material $name" }
    }
    return maybeMaterial
  }

  fun getDirection(name: String): Direction? =
    try {
      Direction.valueOf(name.uppercase())
    } catch (e: IllegalArgumentException) {
      logger.error(e) { "Unknown direction $name" }
      null
    }

  fun generateNeighborMap(textureAtlasRegions: GdxArray<TextureAtlas.AtlasRegion>) {
    for (region: TextureAtlas.AtlasRegion in textureAtlasRegions) {
      val split = region.name.split('-')
      // If the size is even or only one, then it is not a neighbor texture
      if (split.size % 2 == 0 || split.size == 1) continue

      val centerMaterial = getMaterialFromTextureName(split[0]) ?: continue
      val wantedNeighbors = split.drop(1).chunked(2).mapNotNullTo(mutableSetOf()) { (materialName, directionName) ->
        val material = getMaterialFromTextureName(materialName)
        val direction = getDirection(directionName)
        if (material != null && direction != null) {
          direction to material
        } else {
          null
        }
      }

      val neighborTextureSet = textureNeighbors.computeIfAbsent(centerMaterial) { mutableListOf() }
      neighborTextureSet += NeighborTexture(region.disallowedRotation(region.name), wantedNeighbors)
    }
    // We want the texture neighbors with the most directions to be first
    for (textureNeighbor in textureNeighbors.values) {
      textureNeighbor.sortByDescending(NeighborTexture::directions)
    }
    logger.debug { "Generated ${textureNeighbors.map { it.value.size }.sum()} neighbor textures" }
  }

  data class NeighborTexture(val textureRegion: RotatableTextureRegion, val neighbor: Set<Pair<Direction, Material>>) {
    val directions: Int = neighbor.size
    fun matches(neighbors: EnumMap<Direction, Material>) = neighbor.all { (dir, mat) -> neighbors[dir] == mat }
  }
}
