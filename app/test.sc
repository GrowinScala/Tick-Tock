import java.util.{Calendar, Date}

import api.dtos.ExclusionDTO
import api.utils.{FakeUUIDGenerator, UUIDGenerator}
import api.utils.DateUtils._
import api.validators.TaskValidator
import java.time.Duration

import play.api.libs.json.Json

val one = dayOfWeekToDayTypeString(1)
val two = dayOfWeekToDayTypeString(2)
val three = dayOfWeekToDayTypeString(3)
val four = dayOfWeekToDayTypeString(4)
val five = dayOfWeekToDayTypeString(5)
val six = dayOfWeekToDayTypeString(6)
val seven = dayOfWeekToDayTypeString(7)
