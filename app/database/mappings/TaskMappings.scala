package database.mappings

import java.sql.Timestamp
import java.util.Date

import play.api.libs.json.{Json, OFormat}
import slick.dbio.Effect
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._
import slick.sql.FixedSqlAction


/**
  * Object that contains the representation of the Task table Row,
  * the slick mappings for the table
  * and query extensions for that table.
  */
object TaskMappings {

  //---------------------------------------------------------
  //# ROW REPRESENTATION
  //---------------------------------------------------------
  case class TaskRow(
                      taskId: String,
                      fileId: String,
                      period: Int,
                      value: Option[Int],
                      startDateAndTime: Option[Date],
                      endDateAndTime: Option[Date],
                      totalOccurrences: Option[Int],
                      currentOccurrences: Option[Int]
                    )


  implicit val taskRowFormat: OFormat[TaskRow] = Json.format[TaskRow]

  //---------------------------------------------------------
  //# TABLE MAPPINGS
  //---------------------------------------------------------
  class TasksTable(tag: Tag) extends Table[TaskRow](tag, "tasks") {
    def taskId = column[String]("taskId", O.PrimaryKey, O.Length(36))
    def fileId = column[String]("fileId", O.Length(36))
    def period = column[Int]("period")
    def value = column[Option[Int]]("value")
    def startDateAndTime = column[Option[Date]]("startDateAndTime")
    def endDateAndTime = column[Option[Date]]("endDateAndTime")
    def totalOccurrences = column[Option[Int]]("totalOccurrences")
    def currentOccurrences = column[Option[Int]]("currentOccurrences")

    /*def fileIdFK =
      foreignKey("fileId", fileId, filesTable)(_.fileId, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)*/

    def * = (taskId, fileId, period, value, startDateAndTime, endDateAndTime, totalOccurrences, currentOccurrences) <> (TaskRow.tupled, TaskRow.unapply)
  }

  //---------------------------------------------------------
  //# TYPE MAPPINGS
  //---------------------------------------------------------
  implicit val dateColumnType: BaseColumnType[Date] = MappedColumnType.base[Date, Timestamp](dateToTimestamp, timestampToDate)
  private def dateToTimestamp(date: Date): Timestamp = new Timestamp(date.getTime)
  private def timestampToDate(timestamp: Timestamp): Date = new Date(timestamp.getTime)

  /*
  implicit val uuidColumnType: BaseColumnType[UUID] = MappedColumnType.base[UUID, String](uuidToString, stringToUUID)
  private def uuidToString(uuid: UUID): String = uuid.toString
  private def stringToUUID(string: String): UUID = UUID.fromString(string)
  */

  //---------------------------------------------------------
  //# QUERY EXTENSIONS
  //---------------------------------------------------------
  lazy val tasksTable = TableQuery[TasksTable]
  val createTasksTableAction = tasksTable.schema.create
  val dropTasksTableAction = tasksTable.schema.drop
  val selectAllFromTasksTable = tasksTable
  val deleteAllFromTasksTable = tasksTable.delete

  //TODO - Define better names
  def selectByTaskId(taskId: String): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.taskId === taskId)
  }

  def selectByFileId(fileId: String): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.fileId === fileId)
  }

  def selectByPeriod(period: Int): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.period === period)
  }

  def selectByValue(value: Int): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.value === value)
  }

  def selectByStartDateAndTime(startDateAndTime: Date): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.startDateAndTime === startDateAndTime)
  }

  def selectByEndDateAndTime(endDateAndTime: Date): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.endDateAndTime === endDateAndTime)
  }

  def selectByTotalOccurrences(totalOccurrences: Int): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.totalOccurrences === totalOccurrences)
  }

  def selectByCurrentOccurrences(currentOccurrences: Int): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.currentOccurrences === currentOccurrences)
  }

  def insertTask(task: TaskRow): FixedSqlAction[Int, NoStream, Effect.Write] = {
    tasksTable += task
  }

  def updateTaskByTaskId(taskId: String, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.taskId === taskId).update(task)
  }

  def updateTaskByFileId(fileId: String, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.fileId === fileId).update(task)
  }

  def updateTaskByPeriod(period: Int, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.period === period).update(task)
  }

  def updateTaskByStartDateAndTime(startDateAndTime: Date, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.startDateAndTime === startDateAndTime).update(task)
  }

  def updateTaskByEndDateAndTime(endDateAndTime: Date, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.endDateAndTime === endDateAndTime).update(task)
  }

  def updateTaskByTotalOccurrences(totalOccurrences: Int, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.totalOccurrences === totalOccurrences).update(task)
  }

  def updateTaskByCurrentOccurrences(currentOccurrences: Int, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.currentOccurrences === currentOccurrences).update(task)
  }

  def deleteByTaskId(id: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.taskId === id).delete
  }

  def deleteByFileId(id: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.fileId === id).delete
  }

  def deleteByPeriod(period: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.period === period).delete
  }

  def deleteByValue(value: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.value === value).delete
  }

  def deleteByStartDateAndTime(startDateAndTime: Date): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.startDateAndTime === startDateAndTime).delete
  }

  def deleteByEndDateAndTime(endDateAndTime: Date): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.endDateAndTime === endDateAndTime).delete
  }

  def deleteByTotalOccurrences(totalOccurrences: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.totalOccurrences === totalOccurrences).delete
  }

  def deleteByCurrentOccurrences(currentOccurrences: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    tasksTable.filter(_.currentOccurrences === currentOccurrences).delete
  }

}
