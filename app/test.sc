import java.io.File
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import api.services.TaskService
import api.utils.DateUtils._
import api.utils.DefaultUUIDGenerator
import api.validators.TaskValidator
import com.typesafe.config.ConfigFactory
import database.mappings.TaskMappings.TaskRow
import database.repositories.{FileRepositoryImpl, TaskRepositoryImpl}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext

implicit val ec = ExecutionContext.Implicits.global
val db = Database.forConfig("dbinfo")
implicit val fileRepo = new FileRepositoryImpl(db)
implicit val taskRepo = new TaskRepositoryImpl(db)
implicit val uuidGen = new DefaultUUIDGenerator

val validator = new TaskValidator()

val calendar = Calendar.getInstance()

calendar.setTime(stringToDateFormat("2030-01-01 12:30:45", "yyyy-MM-dd HH:mm:ss"))

val row = TaskRow("asd", "asd", 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some("PST"))
println(row)