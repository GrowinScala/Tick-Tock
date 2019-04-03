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
    value: Option[Int] = None,
    startDateAndTime: Option[Date] = None,
    endDateAndTime: Option[Date] = None,
    totalOccurrences: Option[Int] = None,
    currentOccurrences: Option[Int] = None,
    timezone: Option[String] = None)

  implicit val taskRowFormat: OFormat[TaskRow] = Json.format[TaskRow]

  //---------------------------------------------------------
  //# TABLE MAPPINGS
  //---------------------------------------------------------
  class TasksTable(tag: Tag) extends Table[TaskRow](tag, "tasks") {
    def taskId = column[String]("taskId", O.Length(36))
    def fileId = column[String]("fileId", O.Length(36))
    def period = column[Int]("period")
    def value = column[Option[Int]]("value")
    def startDateAndTime = column[Option[Date]]("startDateAndTime")
    def endDateAndTime = column[Option[Date]]("endDateAndTime")
    def totalOccurrences = column[Option[Int]]("totalOccurrences")
    def currentOccurrences = column[Option[Int]]("currentOccurrences")
    def timezone = column[Option[String]]("timezone")

    def * = (taskId, fileId, period, value, startDateAndTime, endDateAndTime, totalOccurrences, currentOccurrences, timezone) <> (TaskRow.tupled, TaskRow.unapply)
  }

  //---------------------------------------------------------
  //# TYPE MAPPINGS
  //---------------------------------------------------------
  implicit val dateColumnType: BaseColumnType[Date] = MappedColumnType.base[Date, Timestamp](dateToTimestamp, timestampToDate)
  private def dateToTimestamp(date: Date): Timestamp = new Timestamp(date.getTime)
  private def timestampToDate(timestamp: Timestamp): Date = new Date(timestamp.getTime)

  //---------------------------------------------------------
  //# QUERY EXTENSIONS
  //---------------------------------------------------------
  lazy val tasksTable = TableQuery[TasksTable]
  val createTasksTableAction = tasksTable.schema.create
  val dropTasksTableAction = tasksTable.schema.drop
  val selectAllFromTasksTable = tasksTable
  val deleteAllFromTasksTable = tasksTable.delete

  def getTaskByTaskId(taskId: String): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.taskId === taskId)
  }

  def getTaskByFileId(fileId: String): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.fileId === fileId)
  }

  def getTaskByPeriod(period: Int): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.period === period)
  }

  def getTaskByValue(value: Int): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.value === value)
  }

  def getTaskByStartDateAndTime(startDateAndTime: Date): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.startDateAndTime === startDateAndTime)
  }

  def getTaskByEndDateAndTime(endDateAndTime: Date): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.endDateAndTime === endDateAndTime)
  }

  def getTaskByTotalOccurrences(totalOccurrences: Int): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.totalOccurrences === totalOccurrences)
  }

  def getTaskByCurrentOccurrences(currentOccurrences: Int): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.currentOccurrences === currentOccurrences)
  }

  def getTaskByTimezone(timezone: String): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.timezone === timezone)
  }

  def insertTask(task: TaskRow): FixedSqlAction[Int, NoStream, Effect.Write] = {
    tasksTable += task
  }

  def updateTaskByTaskId(taskId: String, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByTaskId(taskId).update(task)
  }

  def updateTaskByFileId(fileId: String, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByFileId(fileId).update(task)
  }

  def updateTaskByPeriod(period: Int, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByPeriod(period).update(task)
  }

  def updateTaskByValue(value: Int, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByValue(value).update(task)
  }

  def updateTaskByStartDateAndTime(startDateAndTime: Date, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByStartDateAndTime(startDateAndTime).update(task)
  }

  def updateTaskByEndDateAndTime(endDateAndTime: Date, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByEndDateAndTime(endDateAndTime).update(task)
  }

  def updateTaskByTotalOccurrences(totalOccurrences: Int, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByTotalOccurrences(totalOccurrences).update(task)
  }

  def updateTaskByCurrentOccurrences(currentOccurrences: Int, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByCurrentOccurrences(currentOccurrences).update(task)
  }

  def updateTaskByTimezone(timezone: String, task: TaskRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByTimezone(timezone).update(task)
  }

  def deleteTaskByTaskId(taskId: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByTaskId(taskId).delete
  }

  def deleteTaskByFileId(fileId: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByFileId(fileId).delete
  }

  def deleteTaskByPeriod(period: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByPeriod(period).delete
  }

  def deleteTaskByValue(value: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByValue(value).delete
  }

  def deleteTaskByStartDateAndTime(startDateAndTime: Date): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByStartDateAndTime(startDateAndTime).delete
  }

  def deleteTaskByEndDateAndTime(endDateAndTime: Date): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByEndDateAndTime(endDateAndTime).delete
  }

  def deleteTaskByTotalOccurrences(totalOccurrences: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByTotalOccurrences(totalOccurrences).delete
  }

  def deleteTaskByCurrentOccurrences(currentOccurrences: Int): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByCurrentOccurrences(currentOccurrences).delete
  }

  def deleteTaskByTimezone(timezone: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getTaskByTimezone(timezone).delete
  }

}
