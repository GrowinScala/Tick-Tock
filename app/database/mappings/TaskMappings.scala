package database.mappings

import java.sql.Timestamp
import java.util.Date

import slick.driver.MySQLDriver.api._

import database.mappings.FileMappings._

import scala.concurrent.Await

object TaskMappings {

  //---------------------------------------------------------
  //# ROW REPRESENTATION
  //---------------------------------------------------------
  case class TaskRow(
                   taskId: Int,
                   fileId: Int,
                   startDateAndTime: Date
                 )

  //---------------------------------------------------------
  //# TABLE MAPPINGS
  //---------------------------------------------------------
  class TasksTable(tag: Tag) extends Table[TaskRow](tag, "tasks"){
    def taskId = column[Int]("taskId", O.PrimaryKey, O.AutoInc)
    def fileId = column[Int]("fileId", O.Length(100))
    def startDateAndTime = column[Date]("startDateAndTime", O.Length(100))

    def fileIdFK =
      foreignKey("fileId", fileId, filesTable)(_.fileId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

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

  def selectByTaskId(id: Int): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.taskId === id)
  }
  def selectByFileId(id: Int): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.fileId === id)
  }
  def selectByStartDateAndTime(startDateAndTime: Date): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.startDateAndTime === startDateAndTime)
  }
  def insertTask(task: TaskRow) = {
    tasksTable += task
  }
  def updateByTaskId(id: Int, task: TaskRow) = {
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
  def deleteByName(name: String) = {
    filesTable.filter(_.fileName === name).delete
  }
  def deleteByPath(path: String) = {
    filesTable.filter(_.filePath === path).delete
  }

  /*def insertInTasksTable(task: TaskRow): Unit = {
    if(existsCorrespondingFileId(task.fileId)) exec(tasksTable += TaskRow(0, task.fileId, task.startDateAndTime))
    else println("Could not insert Task with id " + task.fileId + " due to not finding a corresponding File.")
  }*/

}