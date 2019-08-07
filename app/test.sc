import java.util.{Calendar, Date}

import api.dtos.ExclusionDTO
import api.utils.{FakeUUIDGenerator, UUIDGenerator}
import api.utils.DateUtils._
import api.validators.TaskValidator
import java.time.Duration

import play.api.libs.json.Json

val dateCalendar = Calendar.getInstance
dateCalendar.set(2030, 4, 1)
dateToLocalDate(dateCalendar.getTime)
