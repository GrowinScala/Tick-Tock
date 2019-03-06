import java.sql.Timestamp
import java.util.Date

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

val task = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
Json.toJsObject(task)