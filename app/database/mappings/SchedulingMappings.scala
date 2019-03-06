package database.mappings

import java.sql.Timestamp
import java.util.Date

import api.services.Criteria.Criteria
import api.services.{Criteria, DayType}
import api.services.DayType.DayType
import play.api.libs.json.{Json, OFormat}
import slick.dbio.Effect
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

object SchedulingMappings {

  //---------------------------------------------------------
  //# ROW REPRESENTATION
  //---------------------------------------------------------

  case class SchedulingRow(
                           schedulingId: String,
                           taskId: String,
                           schedulingDate: Option[Date],
                           day: Option[Int],
                           dayOfWeek: Option[Int],
                           dayType: Option[DayType],
                           month: Option[Int],
                           year: Option[Int],
                           criteria: Option[Criteria]
                         )

  implicit val exclusionsRowFormat: OFormat[SchedulingRow] = Json.format[SchedulingRow]

  //---------------------------------------------------------
  //# TABLE MAPPINGS
  //---------------------------------------------------------

  class ExclusionsTable(tag: Tag) extends Table[SchedulingRow](tag, "exclusions"){
    def schedulingId = column[String]("schedulingId", O.PrimaryKey, O.Length(36))
    def taskId = column[String]("taskId", O.Unique, O.Length(36))
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
  private def booleanToDayType(boolean: Boolean): DayType = if(boolean) DayType.Weekend else DayType.Weekday

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
}
