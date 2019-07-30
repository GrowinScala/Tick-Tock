package database.mappings

import java.sql.Timestamp
import java.time.LocalDate
import java.util.Date

import api.services.Criteria.Criteria
import api.services.DayType.DayType
import api.services.{ Criteria, DayType }
import play.api.libs.json.{ Json, OFormat }
import api.utils.DateUtils._
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
    exclusionDate: Option[LocalDate] = None,
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
    def exclusionId = column[String]("exclusionId", O.PrimaryKey, O.Length(36))
    def taskId = column[String]("taskId", O.Length(36))
    def exclusionDate = column[Option[LocalDate]]("exclusionDate")
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

  implicit val dateColumnType: BaseColumnType[LocalDate] = MappedColumnType.base[LocalDate, Timestamp](localDateToTimestamp, timestampToLocalDate)
  private def localDateToTimestamp(date: LocalDate): Timestamp = new Timestamp(localDateToDate(date).getTime)
  private def timestampToLocalDate(timestamp: Timestamp): LocalDate = dateToLocalDate(new Date(timestamp.getTime))

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

  def getExclusionByExclusionDate(exclusionDate: LocalDate): Query[ExclusionsTable, ExclusionRow, Seq] = {
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

  def updateExclusionByTaskId(exclusionId: String, taskId: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionId(exclusionId).map(_.taskId).update(taskId)
  }

  def updateExclusionByExclusionDate(exclusionId: String, exclusionDate: LocalDate): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionId(exclusionId).map(_.exclusionDate).update(Some(exclusionDate))
  }

  def updateExclusionByDay(exclusionId: String, day: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionId(exclusionId).map(_.day).update(Some(day))
  }

  def updateExclusionByDayOfWeek(exclusionId: String, dayOfWeek: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionId(exclusionId).map(_.dayOfWeek).update(Some(dayOfWeek))
  }

  def updateExclusionByDayType(exclusionId: String, dayType: DayType): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionId(exclusionId).map(_.dayType).update(Some(dayType))
  }

  def updateExclusionByMonth(exclusionId: String, month: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionId(exclusionId).map(_.month).update(Some(month))
  }

  def updateExclusionByYear(exclusionId: String, year: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionId(exclusionId).map(_.year).update(Some(year))
  }

  def updateExclusionByCriteria(exclusionId: String, criteria: Criteria): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionId(exclusionId).map(_.criteria).update(Some(criteria))
  }

  def deleteExclusionByExclusionId(exclusionId: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByExclusionId(exclusionId).delete
  }

  def deleteExclusionByTaskId(taskId: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getExclusionByTaskId(taskId).delete
  }

  def deleteExclusionByExclusionDate(exclusionDate: LocalDate): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
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
