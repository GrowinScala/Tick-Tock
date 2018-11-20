package services

import slick.driver.MySQLDriver.api._
import scala.concurrent._
import scala.concurrent.duration._

object DBConnector {

  case class File(
                 fileName: String,
                 uploadDate: String
                 )

  case class Task(
                 taskId: Int,
                 fileName: String,
                 startDateAndTime: String
                 )

  class FilesTable(tag: Tag) extends Table[File](tag, "files"){
    def fileName = column[String]("fileName", O.PrimaryKey, O.Length(100))
    def uploadDate = column[String]("uploadDate", O.Length(20))

    def * = (fileName, uploadDate) <> (File.tupled, File.unapply)
  }

  class TasksTable(tag: Tag) extends Table[Task](tag, "tasks"){
    def taskId = column[Int]("taskId", O.PrimaryKey, O.AutoInc)
    def fileName = column[String]("fileName", O.Length(100))
    def startDateAndTime = column[String]("startDateAndTime", O.Length(100))

    def fileNameFK =
      foreignKey("fileName", fileName, filesTable)(_.fileName, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (taskId, fileName, startDateAndTime) <> (Task.tupled, Task.unapply)
  }

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

  def existsCorrespondingFileName(fileName: String): Boolean = {
    exec(filesTable.filter(_.fileName === fileName).result) != Vector()
  }

  def insertFilesTableAction(file: File): Unit = {
    exec(filesTable += File(file.fileName, file.uploadDate))
  }
  def insertTasksTableAction(task: Task): Unit = {
    if(existsCorrespondingFileName(task.fileName)) exec(tasksTable += Task(0, task.fileName, task.startDateAndTime))
    else println("Could not insert Task with name " + task.fileName + " due to not finding a corresponding File.")
  }

  lazy val db = Database.forConfig("dbinfo")

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)
}
