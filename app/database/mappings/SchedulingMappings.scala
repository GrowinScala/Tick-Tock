package database.mappings

import java.sql.Timestamp
import java.util.Date

import api.services.Criteria.Criteria
import api.services.DayType.DayType
import api.services.{ Criteria, DayType }
import play.api.libs.json.{ Json, OFormat }
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

object SchedulingMappings {

  //---------------------------------------------------------
  //# ROW REPRESENTATION
  //---------------------------------------------------------

  case class SchedulingRow(
    schedulingId: String,
    taskId: String,
    schedulingDate: Option[Date] = None,
    day: Option[Int] = None,
    dayOfWeek: Option[Int] = None,
    dayType: Option[DayType] = None,
    month: Option[Int] = None,
    year: Option[Int] = None,
    criteria: Option[Criteria] = None)

  implicit val schedulingsRowFormat: OFormat[SchedulingRow] = Json.format[SchedulingRow]

  //---------------------------------------------------------
  //# TABLE MAPPINGS
  //---------------------------------------------------------

  class SchedulingsTable(tag: Tag) extends Table[SchedulingRow](tag, "schedulings") {
    def schedulingId = column[String]("schedulingId", O.Length(36))
    def taskId = column[String]("taskId", O.Length(36))
    def schedulingDate = column[Option[Date]]("schedulingDate")
    def day = column[Option[Int]]("day")
    def dayOfWeek = column[Option[Int]]("dayOfWeek")
    def dayType = column[Option[DayType]]("dayType")
    def month = column[Option[Int]]("month")
    def year = column[Option[Int]]("year")
    def criteria = column[Option[Criteria]]("criteria")

    def * = (schedulingId, taskId, schedulingDate, day, dayOfWeek, dayType, month, year, criteria) <> (SchedulingRow.tupled, SchedulingRow.unapply)
  }

  //---------------------------------------------------------
  //# FILES TABLE TYPE MAPPINGS
  //---------------------------------------------------------
  implicit val dateColumnType: BaseColumnType[Date] = MappedColumnType.base[Date, Timestamp](dateToTimestamp, timestampToDate)
  private def dateToTimestamp(date: Date): Timestamp = new Timestamp(date.getTime)
  private def timestampToDate(timestamp: Timestamp): Date = new Date(timestamp.getTime)

  implicit val dayTypeColumnType: BaseColumnType[DayType] = MappedColumnType.base[DayType, Boolean](dayTypeToBoolean, booleanToDayType)
  private def dayTypeToBoolean(dayType: DayType): Boolean = dayType == DayType.Weekend
  private def booleanToDayType(boolean: Boolean): DayType = if (boolean) DayType.Weekend else DayType.Weekday

  implicit val criteriaColumnType: BaseColumnType[Criteria] = MappedColumnType.base[Criteria, Int](criteriaToInt, intToCriteria)
  private def criteriaToInt(criteria: Criteria): Int = {
    criteria match {
      case Criteria.First => 1
      case Criteria.Second => 2
      case Criteria.Third => 3
      case Criteria.Fourth => 4
      case Criteria.Last => 5
    }
  }
  private def intToCriteria(int: Int): Criteria = {
    int match {
      case 1 => Criteria.First
      case 2 => Criteria.Second
      case 3 => Criteria.Third
      case 4 => Criteria.Fourth
      case 5 => Criteria.Last
    }
  }

  //---------------------------------------------------------
  //# QUERY EXTENSIONS
  //---------------------------------------------------------
  lazy val schedulingsTable = TableQuery[SchedulingsTable]
  val createSchedulingsTableAction = schedulingsTable.schema.create
  val dropSchedulingsTableAction = schedulingsTable.schema.drop
  val selectAllFromSchedulingsTable = schedulingsTable
  val deleteAllFromSchedulingsTable = schedulingsTable.delete

  def getSchedulingBySchedulingId(schedulingId: String): Query[SchedulingsTable, SchedulingRow, Seq] = {
    schedulingsTable.filter(_.schedulingId === schedulingId)
  }

  def getSchedulingByTaskId(taskId: String): Query[SchedulingsTable, SchedulingRow, Seq] = {
    schedulingsTable.filter(_.taskId === taskId)
  }

  def getSchedulingBySchedulingDate(schedulingDate: Date): Query[SchedulingsTable, SchedulingRow, Seq] = {
    schedulingsTable.filter(_.schedulingDate === schedulingDate)
  }

  def getSchedulingByDay(day: Int): Query[SchedulingsTable, SchedulingRow, Seq] = {
    schedulingsTable.filter(_.day === day)
  }

  def getSchedulingByDayOfWeek(dayOfWeek: Int): Query[SchedulingsTable, SchedulingRow, Seq] = {
    schedulingsTable.filter(_.dayOfWeek === dayOfWeek)
  }

  def getSchedulingByDayType(dayType: DayType): Query[SchedulingsTable, SchedulingRow, Seq] = {
    schedulingsTable.filter(_.dayType === dayType)
  }

  def getSchedulingByMonth(month: Int): Query[SchedulingsTable, SchedulingRow, Seq] = {
    schedulingsTable.filter(_.month === month)
  }

  def getSchedulingByYear(year: Int): Query[SchedulingsTable, SchedulingRow, Seq] = {
    schedulingsTable.filter(_.year === year)
  }

  def getSchedulingByCriteria(criteria: Criteria): Query[SchedulingsTable, SchedulingRow, Seq] = {
    schedulingsTable.filter(_.criteria === criteria)
  }

  def insertScheduling(scheduling: SchedulingRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    schedulingsTable += scheduling
  }

  def updateSchedulingBySchedulingId(schedulingId: String, scheduling: SchedulingRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingBySchedulingId(schedulingId).update(scheduling)
  }

  def updateSchedulingByTaskId(taskId: String, scheduling: SchedulingRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByTaskId(taskId).update(scheduling)
  }

  def updateSchedulingBySchedulingDate(schedulingDate: Date, scheduling: SchedulingRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingBySchedulingDate(schedulingDate).update(scheduling)
  }

  def updateSchedulingByDay(day: Int, scheduling: SchedulingRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByDay(day).update(scheduling)
  }

  def updateSchedulingByDayOfWeek(dayOfWeek: Int, scheduling: SchedulingRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByDayOfWeek(dayOfWeek).update(scheduling)
  }

  def updateSchedulingByDayType(dayType: DayType, scheduling: SchedulingRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByDayType(dayType).update(scheduling)
  }

  def updateSchedulingByMonth(month: Int, scheduling: SchedulingRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByMonth(month).update(scheduling)
  }

  def updateSchedulingByYear(year: Int, scheduling: SchedulingRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByYear(year).update(scheduling)
  }

  def updateSchedulingByCriteria(criteria: Criteria, scheduling: SchedulingRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByCriteria(criteria).update(scheduling)
  }

  def deleteSchedulingBySchedulingId(schedulingId: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingBySchedulingId(schedulingId).delete
  }

  def deleteSchedulingByTaskId(taskId: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByTaskId(taskId).delete
  }

  def deleteSchedulingBySchedulingDate(schedulingDate: Date): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingBySchedulingDate(schedulingDate).delete
  }

  def deleteSchedulingByDay(day: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByDay(day).delete
  }

  def deleteSchedulingByDayOfWeek(dayOfWeek: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByDayOfWeek(dayOfWeek).delete
  }

  def deleteSchedulingByDayType(dayType: DayType): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByDayType(dayType).delete
  }

  def deleteSchedulingByMonth(month: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByMonth(month).delete
  }

  def deleteSchedulingByYear(year: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByYear(year).delete
  }

  def deleteSchedulingByCriteria(criteria: Criteria): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getSchedulingByCriteria(criteria).delete
  }

}
