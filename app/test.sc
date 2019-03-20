import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import api.dtos.TaskDTO
import api.services.{PeriodType, SchedulingType}
import slick.jdbc.MySQLProfile.api._
import api.utils.DateUtils._
import api.utils.{DefaultUUIDGenerator, UUIDGenerator}
import api.validators.TaskValidator
import database.repositories.{FileRepositoryImpl, TaskRepositoryImpl}
import play.api.libs.json.Json

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

implicit val ec = ExecutionContext.Implicits.global
val db = Database.forConfig("dbinfo")
implicit val fileRepo = new FileRepositoryImpl(db)
implicit val taskRepo = new TaskRepositoryImpl(db)
implicit val uuidGen = new DefaultUUIDGenerator

val validator = new TaskValidator()

//val date = parseDateWithTimezone("2030-01-01 00:00:00", "EST")
val date1 = parseDateWithTimezone("2019-07-01 00:00:00", "PST")
val date2 = parseDateWithTimezone("01-07-2019 00:00:00", "PST")
val date3 = parseDateWithTimezone("2019/07/01 00:00:00", "PST")
val date4 = parseDateWithTimezone("01/07/2019 00:00:00", "PST")

val date5 = parseDate("2030-01-01 00:00:00")
val date6 = parseDate("01-01-2030 00:00:00")
val date7 = parseDate("2030/01/01 00:00:00")
val date8 = parseDate("01/01/2030 00:00:00")
val date9 = parseDate("01012030 000000")