import java.io.File
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import api.services.TaskService
import api.utils.DateUtils._
import api.utils.DefaultUUIDGenerator
import api.validators.TaskValidator
import com.typesafe.config.ConfigFactory
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

calendar.setTime(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))

print(calendar.getTime)
val ts = new TaskService()

def getDateFromCalendar(day: Int, month: Int, year: Int, timezone: Option[String] = None): Date = {
  val dateCalendar = Calendar.getInstance
  //println(dateCalendar)
  if(timezone.isDefined) dateCalendar.setTimeZone(parseTimezone(timezone.get).get)
  dateCalendar.set(year, month-1, day)
  println(dateCalendar + "ola")
  dateCalendar.getTime
}

println(getDateFromCalendar(1, 1, 2030))

def removeTimeFromDate(date: Date): Date = {
  val sdf = new SimpleDateFormat("yyyy-MM-dd")
  val string = sdf.format(date)
  sdf.parse(string)
}

removeTimeFromDate(calendar.getTime)