package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2ShapeId
import com.badlogic.gdx.box2d.structs.b2WorldId
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent.Companion.queuePhysicsEvent
import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent

class ContactManager(val box2dWorld: b2WorldId, val engine: Engine) {

//  private val contactEvents = b2ContactEvents()
//  private val sensorEvents = b2SensorEvents()
//  private val beginTouchEvents = b2ContactBeginTouchEvent.b2ContactBeginTouchEventPointer()
//  private val beginSensorEvents = b2SensorBeginTouchEvent.b2SensorBeginTouchEventPointer()
//  private val endTouchEvents = b2ContactEndTouchEvent.b2ContactEndTouchEventPointer()
//  private val endSensorEvents = b2SensorEndTouchEvent.b2SensorEndTouchEventPointer()
//  private val shapeIdA = b2ShapeId()
//  private val shapeIdB = b2ShapeId()

  fun postBox2dStepEvents() {
    val contactEvents = Box2d.b2World_GetContactEvents(box2dWorld)
    val sensorEvents = Box2d.b2World_GetSensorEvents(box2dWorld)

    val beginContactEvents = contactEvents.beginEvents()
    for (i in 0 until contactEvents.beginCount()) {
      val endEvent = beginContactEvents[i]
      handleBeginTouchEvent(endEvent.shapeIdA, endEvent.shapeIdB)
    }

    val beginSensorEvents = sensorEvents.beginEvents()
    for (i in 0 until sensorEvents.beginCount()) {
      val beginSensorEvent = beginSensorEvents[i]
      handleBeginTouchEvent(beginSensorEvent.visitorShapeId(), beginSensorEvent.sensorShapeId())
    }

    val endContactEvents = contactEvents.endEvents()
    for (i in 0 until contactEvents.endCount()) {
      val endEvent = endContactEvents[i]
      handleEndTouchEvent(endEvent.shapeIdA, endEvent.shapeIdB)
    }

    val endSensorEvents = sensorEvents.endEvents()
    for (i in 0 until sensorEvents.endCount()) {
      val endSensorEvent = endSensorEvents[i]
      handleEndTouchEvent(endSensorEvent.visitorShapeId(), endSensorEvent.sensorShapeId())
    }
  }

  private fun handleBeginTouchEvent(shapeIdA: b2ShapeId?, shapeIdB: b2ShapeId?) = queuePhysicsEvent(PhysicsEvent.ContactBeginsEvent(shapeIdA, shapeIdB))

  private fun handleEndTouchEvent(shapeIdA: b2ShapeId?, shapeIdB: b2ShapeId?) = queuePhysicsEvent(PhysicsEvent.ContactEndsEvent(shapeIdA, shapeIdB))
}
