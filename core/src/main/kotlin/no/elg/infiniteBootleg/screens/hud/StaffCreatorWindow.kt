package no.elg.infiniteBootleg.screens.hud

import com.kotcrab.vis.ui.widget.VisSelectBox
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.scene2d.KTable
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.KVisTable
import ktx.scene2d.vis.visCheckBox
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visSlider
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.util.IBVisWindow
import no.elg.infiniteBootleg.util.ibVisWindowClosed
import no.elg.infiniteBootleg.util.setIBDefaults
import no.elg.infiniteBootleg.world.Staff
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import no.elg.infiniteBootleg.world.magic.Gem
import no.elg.infiniteBootleg.world.magic.Ring
import no.elg.infiniteBootleg.world.magic.Wood
import no.elg.infiniteBootleg.world.magic.parts.Birch
import no.elg.infiniteBootleg.world.magic.parts.Diamond
import no.elg.infiniteBootleg.world.magic.parts.GemRating
import no.elg.infiniteBootleg.world.magic.parts.GemType
import no.elg.infiniteBootleg.world.magic.parts.GravityRing
import no.elg.infiniteBootleg.world.magic.parts.RingRating
import no.elg.infiniteBootleg.world.magic.parts.RingType
import no.elg.infiniteBootleg.world.magic.parts.WoodRating
import no.elg.infiniteBootleg.world.magic.parts.WoodType
import no.elg.infiniteBootleg.world.world.ClientWorld

private val logger = KotlinLogging.logger {}

@Scene2dDsl
fun KTable.floatSlider(
  name: String,
  srcValueGetter: () -> Float,
  min: Float,
  max: Float,
  step: Float,
  onAnyElementChanged: MutableList<() -> Unit>,
  onChange: (Float) -> Unit
) {
  horizontalGroup {
    it.fillX()
    space(5f)
    visLabel(name)
    visSlider(min, max, step) {
      this.name = name
      setValue(srcValueGetter())

      onAnyElementChanged += {
        setValue(srcValueGetter())
      }
      onChange {
        onChange(this.value)
        updateAllValues(onAnyElementChanged)
      }
    }
  }
}

@Scene2dDsl
inline fun <reified TYPE : Any, reified RATING : Enum<RATING>, RESULT> KVisTable.addSelector(
  initType: TYPE,
  initRating: RATING,
  onAnyElementChanged: MutableList<() -> Unit>,
  allowDisable: Boolean = true,
  crossinline gen: (type: TYPE, rating: RATING) -> RESULT
): () -> RESULT? {
  var type: TYPE = initType
  var rating: RATING = initRating
  val selectors = mutableListOf<VisSelectBox<*>>()
  val enabledBox = visCheckBox("Enabled") {
    isChecked = true
    if (allowDisable) {
      onChange {
        selectors.forEach { it.isDisabled = !isChecked }
      }
    } else {
      isDisabled = true
    }
  }
  selectors += sealedSelector<TYPE>(onAnyElementChanged, type) { type = it }
  selectors += enumSelector<RATING>(onAnyElementChanged, rating) { rating = it }
  row()

  return { if (enabledBox.isChecked) gen(type, rating) else null }
}

fun addStaffCreatorOverlay(world: ClientWorld): IBVisWindow {
  val onAnyElementChanged: MutableList<() -> Unit> = mutableListOf()
  return world.ibVisWindowClosed("Staff Creator") {
    closeOnEscape()
    addCloseButton()

    val gems = mutableListOf<() -> Gem?>()
    val rings = mutableListOf<() -> Ring?>()

    @Scene2dDsl
    fun KVisTable.addGemSelector(allowDisable: Boolean = true): () -> Gem? =
      addSelector<GemType, GemRating, Gem>(Diamond, GemRating.FLAWLESS, onAnyElementChanged, allowDisable) { type, rating -> Gem(type, rating) }

    @Scene2dDsl
    fun KVisTable.addRingSelector(): () -> Ring? =
      addSelector<RingType<RingRating?>, RingRating, Ring>(GravityRing, RingRating.FLAWLESS, onAnyElementChanged) { type, rating -> Ring(type, rating) }

    @Scene2dDsl
    fun KVisTable.addGemsAndRings(type: WoodType) {
      clear()
      gems.clear()
      rings.clear()

      row()
      gems += addGemSelector(false)
      for (i in 1u until type.gemSlots) {
        gems += addGemSelector()
      }
      aSeparator()
      for (i in 0u until type.ringSlots) {
        rings += addRingSelector()
      }
      this@ibVisWindowClosed.pack()
      this@ibVisWindowClosed.centerWindow()
    }

    val containerTable = KVisTable(true)

    @Scene2dDsl
    fun KVisTable.addWoodSelector(): () -> Wood {
      var type: WoodType = Birch
      var rating: WoodRating = WoodRating.FRESHLY_CUT
      visTable {
        setIBDefaults()
        sealedSelector<WoodType>(onAnyElementChanged, type) {
          type = it
          containerTable.addGemsAndRings(type)
        }
        enumSelector<WoodRating>(onAnyElementChanged, rating) { rating = it }
      }
      return { Wood(type, rating) }
    }

    visTable {
      setIBDefaults()
      val wood = addWoodSelector()
      row()
      aSeparator()
      actor(containerTable) {
        setIBDefaults(pad = false)
        addGemsAndRings(wood().type)
      }
      row()
      visTextButton("Create Staff") {
        onClick {
          val newStaff = Staff(wood(), gems.mapNotNull { it() }, rings.mapNotNull { it() })

          for (player in world.controlledPlayerEntities) {
            logger.info { "Giving player ${player.nameOrNull} a new staff $newStaff" }
            val container = player.containerOrNull ?: continue
            container += newStaff.toItem()
          }
        }
      }
    }
    pack()
  }
}
