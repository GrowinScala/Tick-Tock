import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Calendar, Date, TimeZone}

import api.dtos.{CreateExclusionDTO, TaskDTO}
import api.services.{PeriodType, SchedulingType}
import slick.jdbc.MySQLProfile.api._
import api.utils.DateUtils._
import api.utils.{DefaultUUIDGenerator, UUIDGenerator}
import api.validators.TaskValidator
import database.repositories.{FileRepositoryImpl, TaskRepositoryImpl}
import executionengine.ExecutionJob
import play.api.libs.json.Json

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

implicit val ec = ExecutionContext.Implicits.global
val db = Database.forConfig("dbinfo")
implicit val fileRepo = new FileRepositoryImpl(db)
implicit val taskRepo = new TaskRepositoryImpl(db)
implicit val uuidGen = new DefaultUUIDGenerator

val validator = new TaskValidator()

//val result = validator.isValidExclusionFormat(Some(List(CreateExclusionDTO(None, Some(15)))), "asd1")

/*val calendar = Calendar.getInstance()
calendar.set(2030, 12 - 1, 25, 0, 0, 0)
val schedulingDate = calendar.getTime*/

val exeJob = new ExecutionJob("asd", "asd", SchedulingType.RunOnce)

exeJob.calculateDelay(Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))

