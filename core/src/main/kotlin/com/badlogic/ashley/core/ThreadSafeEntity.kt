package com.badlogic.ashley.core

import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.Bits

class ThreadSafeEntity : Entity() {
  @Synchronized
  override fun add(component: Component?): Entity? = super.add(component)

  @Synchronized
  override fun <T : Component?> addAndReturn(component: T?): T? = super.addAndReturn(component)

  @Synchronized
  override fun <T : Component?> remove(componentClass: Class<T?>?): T? = super.remove(componentClass)

  @Synchronized
  override fun removeAll() {
    super.removeAll()
  }

  @Synchronized
  override fun <T : Component?> getComponent(componentClass: Class<T?>?): T? = super.getComponent(componentClass)

  @Synchronized
  override fun <T : Component?> getComponent(componentType: ComponentType?): T? = super.getComponent(componentType)

  override fun getComponents(): ImmutableArray<Component?>? = super.components

  override fun hasComponent(componentType: ComponentType?): Boolean = super.hasComponent(componentType)

  override fun getComponentBits(): Bits? = super.componentBits

  override fun getFamilyBits(): Bits? = super.familyBits

  @Synchronized
  override fun addInternal(component: Component?): Boolean = super.addInternal(component)

  @Synchronized
  override fun removeInternal(componentClass: Class<out Component?>?): Component? = super.removeInternal(componentClass)

  @Synchronized
  override fun notifyComponentAdded() {
    super.notifyComponentAdded()
  }

  @Synchronized
  override fun notifyComponentRemoved() {
    super.notifyComponentRemoved()
  }
}
