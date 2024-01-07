package no.elg.infiniteBootleg.screens.hud

import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisSelectBox
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.ashley.plusAssign
import ktx.scene2d.KTable
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.actors
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.KVisTable
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.visCheckBox
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visSlider
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visWindow
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.addHideButton
import no.elg.infiniteBootleg.screens.hide
import no.elg.infiniteBootleg.screens.hideOnEscape
import no.elg.infiniteBootleg.screens.setIBDefaults
import no.elg.infiniteBootleg.world.Staff
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedInventoryItemComponentOrNull
import no.elg.infiniteBootleg.world.magic.Gem
import no.elg.infiniteBootleg.world.magic.Ring
import no.elg.infiniteBootleg.world.magic.Wood
import no.elg.infiniteBootleg.world.magic.parts.AntiGravityRing
import no.elg.infiniteBootleg.world.magic.parts.Birch
import no.elg.infiniteBootleg.world.magic.parts.Diamond
import no.elg.infiniteBootleg.world.magic.parts.GemRating
import no.elg.infiniteBootleg.world.magic.parts.GemType
import no.elg.infiniteBootleg.world.magic.parts.RingRating
import no.elg.infiniteBootleg.world.magic.parts.RingType
import no.elg.infiniteBootleg.world.magic.parts.WoodRating
import no.elg.infiniteBootleg.world.magic.parts.WoodType
import no.elg.infiniteBootleg.world.world.ClientWorld

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

fun Stage.addStaffCreatorOverlay(world: ClientWorld): KVisWindow {
  actors {
    val onAnyElementChanged: MutableList<() -> Unit> = mutableListOf()
    return visWindow("Staff Creator") {
      hideOnEscape()
      addHideButton()
      hide()

      val gems = mutableListOf<() -> Gem?>()
      val rings = mutableListOf<() -> Ring?>()

      @Scene2dDsl
      fun KVisTable.addGemSelector(allowDisable: Boolean = true): () -> Gem? =
        addSelector<GemType, GemRating, Gem>(Diamond, GemRating.FLAWLESS, onAnyElementChanged, allowDisable) { type, rating -> Gem(type, rating) }

      @Scene2dDsl
      fun KVisTable.addRingSelector(): () -> Ring? =
        addSelector<RingType<RingRating?>, RingRating, Ring>(AntiGravityRing, RingRating.FLAWLESS, onAnyElementChanged) { type, rating -> Ring(type, rating) }

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
        this@visWindow.pack()
        this@visWindow.centerWindow()
      }

      val container = KVisTable(true)

      @Scene2dDsl
      fun KVisTable.addWoodSelector(): () -> Wood {
        var type: WoodType = Birch
        var rating: WoodRating = WoodRating.FRESHLY_CUT
        visTable {
          setIBDefaults()
          sealedSelector<WoodType>(onAnyElementChanged, type) {
            type = it
            container.addGemsAndRings(type)
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
        actor(container) {
          setIBDefaults(pad = false)
          addGemsAndRings(wood().type)
        }
        row()
        visTextButton("Create Staff") {
          onClick {
            val newStaff = Staff(wood(), gems.mapNotNull { it() }, rings.mapNotNull { it() })

            world.controlledPlayerEntities.forEach { player ->
              Main.logger().log("Giving player ${player.nameOrNull} a new staff $newStaff")
              val existing = player.selectedInventoryItemComponentOrNull
              if (existing != null) {
                existing.element = newStaff
              } else {
                player += SelectedInventoryItemComponent(newStaff)
              }
            }
          }
        }
      }
      pack()
    }
  }
}
