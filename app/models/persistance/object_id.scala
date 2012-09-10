package models.persistance.object_id

import org.bson.types.ObjectId


object Object_id {
  def objectId2string(oi : ObjectId) : String = oi.toStringBabble
  def string2objectId(s : String) : ObjectId = new ObjectId(s, true)
}

