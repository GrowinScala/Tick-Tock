package services.task

import services.DBConnector.{Task, filesTable}
import slick.model.ForeignKeyAction

import slick.driver.MySQLDriver.api._
import scala.concurrent._
import scala.concurrent.duration._

class TaskMappings {

  case class Task(
                   taskId: Int,
                   fileId: Int,
                   startDateAndTime: String
                 )

  class TasksTable(tag: Tag) extends Table[Task](tag, "tasks"){
    def taskId = column[Int]("taskId", O.PrimaryKey, O.AutoInc)
    def fileId = column[Int]("fileId", O.Length(100))
    def startDateAndTime = column[String]("startDateAndTime", O.Length(100))

    def fileIdFK =
      foreignKey("fileId", fileId, filesTable)(_.fileId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (taskId, fileId, startDateAndTime) <> (Task.tupled, Task.unapply)
  }
}
