package database.mappings

import java.sql.Timestamp
import java.util.Date

import slick.jdbc.MySQLProfile.api._
import database.mappings.FileMappings._
import play.api.libs.json.{Json, OFormat}
import slick.dbio.Effect
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
                      taskId: Int,
                      fileId: Int,
                      startDateAndTime: Date
                    )

  //TODO - separate TaskRow
  implicit val taskRowFormat: OFormat[TaskRow] = Json.format[TaskRow]

  //---------------------------------------------------------
  //# TABLE MAPPINGS
  //---------------------------------------------------------
  class TasksTable(tag: Tag) extends Table[TaskRow](tag, "tasks") {
    def taskId = column[Int]("taskId", O.PrimaryKey, O.AutoInc)

    def fileId = column[Int]("fileId", O.Length(100))

    def startDateAndTime = column[Date]("startDateAndTime", O.Length(100))

    def fileIdFK =
      foreignKey("fileId", fileId, filesTable)(_.fileId, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def * = (taskId, fileId, startDateAndTime) <> (TaskRow.tupled, TaskRow.unapply)
  }

  //---------------------------------------------------------
  //# TYPE MAPPINGS
  //---------------------------------------------------------
  implicit val columnType: BaseColumnType[Date] =
  MappedColumnType.base[Date, Timestamp](dateToTimestamp, timestampToDate)

  private def dateToTimestamp(date: Date): Timestamp =
    new Timestamp(date.getTime)

  private def timestampToDate(timestamp: Timestamp): Date =
    new Date(timestamp.getTime)

  //---------------------------------------------------------
  //# QUERY EXTENSIONS
  //---------------------------------------------------------
  lazy val tasksTable = TableQuery[TasksTable]
  val createTasksTableAction = tasksTable.schema.create
  val dropTasksTableAction = tasksTable.schema.drop
  val selectAllFromTasksTable = tasksTable
  val deleteAllFromTasksTable = tasksTable.delete

  //TODO - Define better names
  def selectByTaskId(id: Int): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.taskId === id)
  }

  def selectByFileId(id: Int): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.fileId === id)
  }

  def selectByStartDateAndTime(startDateAndTime: Date): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.startDateAndTime === startDateAndTime)
  }

  def insertTask(task: TaskRow): FixedSqlAction[Int, NoStream, Effect.Write] = {
    tasksTable += task
  }

  def updateTaskByTaskId(id: Int, task: TaskRow) = {
    tasksTable.filter(_.taskId === id).update(task)
  }

  def updateByFileId(id: Int, task: TaskRow) = {
    tasksTable.filter(_.fileId === id).update(task)
  }

  def updateByStartDateAndTime(startDateAndTime: Date, task: TaskRow) = {
    tasksTable.filter(_.startDateAndTime === startDateAndTime).update(task)
  }

  def deleteByTaskId(id: Int) = {
    tasksTable.filter(_.taskId === id).delete
  }

  def deleteByFileId(id: Int) = {
    tasksTable.filter(_.fileId === id).delete
  }

  def deleteByStartDateAndTime(startDateAndTime: Date) = {
    tasksTable.filter(_.startDateAndTime === startDateAndTime).delete
  }

}
