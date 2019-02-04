package api.utils

import java.util.UUID

class DefaultUUIDGenerator extends UUIDGenerator {

  def generateUUID: String = UUID.randomUUID().toString
}
