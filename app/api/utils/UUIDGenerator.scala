package api.utils

import java.util.UUID

import javax.inject.Singleton

trait UUIDGenerator {

  def generateUUID: String
}
