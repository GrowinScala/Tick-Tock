package database.mappings

import java.sql.Timestamp
import java.util.Date
import java.util.UUID

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
                      taskId: String,
                      fileId: String,
                      startDateAndTime: Date
                    )

  //TODO - separate TaskRow
  implicit val taskRowFormat: OFormat[TaskRow] = Json.format[TaskRow]

  //---------------------------------------------------------
  //# TABLE MAPPINGS
  //---------------------------------------------------------
  class TasksTable(tag: Tag) extends Table[TaskRow](tag, "tasks") {
    def taskId = column[String]("taskId", O.PrimaryKey, O.Length(36))
    def fileId = column[String]("fileId", O.Length(36))
    def startDateAndTime = column[Date]("startDateAndTime")

    /*def fileIdFK =
      foreignKey("fileId", fileId, filesTable)(_.fileId, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)*/

    def * = (taskId, fileId, startDateAndTime) <> (TaskRow.tupled, TaskRow.unapply)
  }

  //---------------------------------------------------------
  //# TYPE MAPPINGS
  //---------------------------------------------------------
  implicit val dateColumnType: BaseColumnType[Date] = MappedColumnType.base[Date, Timestamp](dateToTimestamp, timestampToDate)
  private def dateToTimestamp(date: Date): Timestamp = new Timestamp(date.getTime)
  private def timestampToDate(timestamp: Timestamp): Date = new Date(timestamp.getTime)

  /*implicit val uuidColumnType: BaseColumnType[UUID] = MappedColumnType.base[UUID, String](uuidToString, stringToUUID)
  private def uuidToString(uuid: UUID): String = uuid.toString
  private def stringToUUID(string: String): UUID = UUID.fromString(string)
  */
  //---------------------------------------------------------
  //# QUERY EXTENSIONS
  //---------------------------------------------------------
  lazy val tasksTable = TableQuery[TasksTable]
  val createTasksTableAction = tasksTable.schema.create
  /*val createTasksTableActionSQL = sqlu"""
    CREATE TABLE `ticktock`.`tasks` (
    `taskId` VARCHAR(36) NOT NULL,
    `fileId` VARCHAR(36) NOT NULL,
    `startDateAndTime` TIMESTAMP NOT NULL,
    PRIMARY KEY (`taskId`),
    INDEX `fileId_idx` (`fileId` ASC) VISIBLE,
    CONSTRAINT `fileIdFK`
    FOREIGN KEY (`fileId`)
    REFERENCES `ticktock`.`files` (`fileId`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT);"""*/
  val dropTasksTableAction = tasksTable.schema.drop
  //val dropTasksTableActionSQL = sqlu"DROP TABLE `ticktock`.`tasks`;"
  val selectAllFromTasksTable = tasksTable
  val deleteAllFromTasksTable = tasksTable.delete

  //TODO - Define better names
  def selectByTaskId(id: String): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.taskId === id)
  }

  def selectByFileId(id: String): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.fileId === id)
  }

  def selectByStartDateAndTime(startDateAndTime: Date): Query[TasksTable, TaskRow, Seq] = {
    tasksTable.filter(_.startDateAndTime === startDateAndTime)
  }

  def insertTask(task: TaskRow): FixedSqlAction[Int, NoStream, Effect.Write] = {
    tasksTable += task
  }

  def updateTaskByTaskId(id: String, task: TaskRow) = {
    tasksTable.filter(_.taskId === id).update(task)
  }

  def updateByFileId(id: String, task: TaskRow) = {
    tasksTable.filter(_.fileId === id).update(task)
  }

  def updateByStartDateAndTime(startDateAndTime: Date, task: TaskRow) = {
    tasksTable.filter(_.startDateAndTime === startDateAndTime).update(task)
  }

  def deleteByTaskId(id: String) = {
    tasksTable.filter(_.taskId === id).delete
  }

  def deleteByFileId(id: String) = {
    tasksTable.filter(_.fileId === id).delete
  }

  def deleteByStartDateAndTime(startDateAndTime: Date) = {
    tasksTable.filter(_.startDateAndTime === startDateAndTime).delete
  }

}
