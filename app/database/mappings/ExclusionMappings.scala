package database.mappings

import java.sql.Timestamp
import java.util.Date

import api.services.Criteria.Criteria
import api.services.DayType.DayType
import api.services.{ Criteria, DayType }
import play.api.libs.json.{ Json, OFormat }
import slick.dbio.Effect
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

object ExclusionMappings {

  //---------------------------------------------------------
  //# ROW REPRESENTATION
  //---------------------------------------------------------

  case class ExclusionRow(
    exclusionId: String,
    taskId: String,
    exclusionDate: Option[Date] = None,
    day: Option[Int] = None,
    dayOfWeek: Option[Int] = None,
    dayType: Option[DayType] = None,
    month: Option[Int] = None,
    year: Option[Int] = None,
    criteria: Option[Criteria] = None)

  implicit val exclusionsRowFormat: OFormat[ExclusionRow] = Json.format[ExclusionRow]

  //---------------------------------------------------------
  //# TABLE MAPPINGS
  //---------------------------------------------------------

  class ExclusionsTable(tag: Tag) extends Table[ExclusionRow](tag, "exclusions") {
    def exclusionId = column[String]("exclusionId", O.Length(36))
    def taskId = column[String]("taskId", O.Length(36))
    def exclusionDate = column[Option[Date]]("exclusionDate")
    def day = column[Option[Int]]("day")
    def dayOfWeek = column[Option[Int]]("dayOfWeek")
    def dayType = column[Option[DayType]]("dayType")
    def month = column[Option[Int]]("month")
    def year = column[Option[Int]]("year")
    def criteria = column[Option[Criteria]]("criteria")

    def * = (exclusionId, taskId, exclusionDate, day, dayOfWeek, dayType, month, year, criteria) <> (ExclusionRow.tupled, ExclusionRow.unapply)
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
  lazy val exclusionsTable = TableQuery[ExclusionsTable]
  val createExclusionsTableAction = exclusionsTable.schema.create
  val dropExclusionsTableAction = exclusionsTable.schema.drop
  val selectAllFromExclusionsTable = exclusionsTable
  val deleteAllFromExclusionsTable = exclusionsTable.delete

  def getExclusionByExclusionId(exclusionId: String): Query[ExclusionsTable, ExclusionRow, Seq] = {
    exclusionsTable.filter(_.exclusionId === exclusionId)
  }

  def getExclusionByTaskId(taskId: String): Query[ExclusionsTable, ExclusionRow, Seq] = {
    exclusionsTable.filter(_.taskId === taskId)
  }

  def getExclusionByExclusionDate(exclusionDate: Date): Query[ExclusionsTable, ExclusionRow, Seq] = {
    exclusionsTable.filter(_.exclusionDate === exclusionDate)
  }

  def getExclusionByDay(day: Int): Query[ExclusionsTable, ExclusionRow, Seq] = {
    exclusionsTable.filter(_.day === day)
  }

  def getExclusionByDayOfWeek(dayOfWeek: Int): Query[ExclusionsTable, ExclusionRow, Seq] = {
    exclusionsTable.filter(_.dayOfWeek === dayOfWeek)
  }

  def getExclusionByDayType(dayType: DayType): Query[ExclusionsTable, ExclusionRow, Seq] = {
    exclusionsTable.filter(_.dayType === dayType)
  }

  def getExclusionByMonth(month: Int): Query[ExclusionsTable, ExclusionRow, Seq] = {
    exclusionsTable.filter(_.month === month)
  }

  def getExclusionByYear(year: Int): Query[ExclusionsTable, ExclusionRow, Seq] = {
    exclusionsTable.filter(_.year === year)
  }

  def getExclusionByCriteria(criteria: Criteria): Query[ExclusionsTable, ExclusionRow, Seq] = {
    exclusionsTable.filter(_.criteria === criteria)
  }

  def insertExclusion(exclusion: ExclusionRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    exclusionsTable += exclusion
  }

  def updateExclusionByExclusionId(exclusionId: String, exclusion: ExclusionRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionId(exclusionId).update(exclusion)
  }

  def updateExclusionByTaskId(taskId: String, exclusion: ExclusionRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByTaskId(taskId).update(exclusion)
  }

  def updateExclusionByExclusionDate(exclusionDate: Date, exclusion: ExclusionRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionDate(exclusionDate).update(exclusion)
  }

  def updateExclusionByDay(day: Int, exclusion: ExclusionRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByDay(day).update(exclusion)
  }

  def updateExclusionByDayOfWeek(dayOfWeek: Int, exclusion: ExclusionRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByDayOfWeek(dayOfWeek).update(exclusion)
  }

  def updateExclusionByDayType(dayType: DayType, exclusion: ExclusionRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByDayType(dayType).update(exclusion)
  }

  def updateExclusionByMonth(month: Int, exclusion: ExclusionRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByMonth(month).update(exclusion)
  }

  def updateExclusionByYear(year: Int, exclusion: ExclusionRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByYear(year).update(exclusion)
  }

  def updateExclusionByCriteria(criteria: Criteria, exclusion: ExclusionRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByCriteria(criteria).update(exclusion)
  }

  def deleteExclusionByExclusionId(exclusionId: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionId(exclusionId).delete
  }

  def deleteExclusionByTaskId(taskId: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByTaskId(taskId).delete
  }

  def deleteExclusionByExclusionDate(exclusionDate: Date): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionDate(exclusionDate).delete
  }

  def deleteExclusionByDay(day: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByDay(day).delete
  }

  def deleteExclusionByDayOfWeek(dayOfWeek: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByDayOfWeek(dayOfWeek).delete
  }

  def deleteExclusionByDayType(dayType: DayType): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByDayType(dayType).delete
  }

  def deleteExclusionByMonth(month: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByMonth(month).delete
  }

  def deleteExclusionByYear(year: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByYear(year).delete
  }

  def deleteExclusionByCriteria(criteria: Criteria): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByCriteria(criteria).delete
  }

}
