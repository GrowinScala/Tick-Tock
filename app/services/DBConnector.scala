package services

import java.util.Date
import java.sql.{Timestamp}

import slick.driver.MySQLDriver.api._

import scala.concurrent._
import scala.concurrent.duration._

object DBConnector {

  case class File(
                 fileId: Int,
                 fileName: String,
                 filePath: String,
                 uploadDate: Date
                 )

  case class Task(
                 taskId: Int,
                 fileId: Int,
                 startDateAndTime: Date
                 )

  class FilesTable(tag: Tag) extends Table[File](tag, "files"){
    def fileId = column[Int]("FileId", O.PrimaryKey)
    def fileName = column[String]("fileName", O.Unique, O.Length(30))
    def filePath = column[String]("filePath", O.Length(100))
    def uploadDate = column[Date]("uploadDate")

    def * = (fileId, fileName, filePath, uploadDate) <> (File.tupled, File.unapply)
  }

  class TasksTable(tag: Tag) extends Table[Task](tag, "tasks"){
    def taskId = column[Int]("taskId", O.PrimaryKey, O.AutoInc)
    def fileId = column[Int]("fileId", O.Length(100))
    def startDateAndTime = column[Date]("startDateAndTime")

    def fileIdFK =
      foreignKey("fileId", fileId, filesTable)(_.fileId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (taskId, fileId, startDateAndTime) <> (Task.tupled, Task.unapply)
  }

  implicit val columnType: BaseColumnType[Date] =
    MappedColumnType.base[Date, Timestamp](dateToTimestamp, timestampToDate)

  private def dateToTimestamp(date: Date): Timestamp =
    new Timestamp(date.getTime)

  private def timestampToDate(timestamp: Timestamp): Date =
    new Date(timestamp.getTime)

  lazy val filesTable = TableQuery[FilesTable]
  lazy val tasksTable = TableQuery[TasksTable]

  val createFilesTable = filesTable.schema.create
  val createTasksTable = tasksTable.schema.create

  val dropFilesTable = filesTable.schema.drop
  val dropTasksTable = tasksTable.schema.drop

  val deleteAllFromFilesTable = filesTable.delete
  val deleteAllFromTasksTable = tasksTable.delete

  val selectAllFromFilesTable = filesTable
  val selectAllFromTasksTable = tasksTable

  def selectFileIdFromName(fileName: String) = {
    exec(filesTable.filter(_.fileName === fileName).map(_.fileId).result)
  }

  def selectNameFromFileId(fileId: Int) = {
    exec(filesTable.filter(_.fileId === fileId).map(_.fileName).result)
  }

  def existsCorrespondingFileId(fileId: Int): Boolean = {
    val res = exec(filesTable.filter(_.fileId === fileId).result)
    res != Vector()
  }

  def insertFilesTableAction(file: File): Unit = {
    exec(filesTable += File(file.fileId, file.fileName, file.filePath, file.uploadDate))
  }

  def insertTasksTableAction(task: Task): Unit = {
    if(existsCorrespondingFileId(task.fileId)) exec(tasksTable += Task(0, task.fileId, task.startDateAndTime))
    else println("Could not insert Task with id " + task.fileId + " due to not finding a corresponding File.")
  }

  val db = Database.forConfig("dbinfo")

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)
}
