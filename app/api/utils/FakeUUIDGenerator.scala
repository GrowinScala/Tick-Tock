package api.utils

import javax.inject.Singleton

@Singleton
class FakeUUIDGenerator extends UUIDGenerator {

  def generateUUID: String = "asd1"
}
