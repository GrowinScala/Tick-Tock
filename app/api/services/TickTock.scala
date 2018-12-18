package api.services

import api.services.TaskService._
import database.mappings.FileMappings.FileRow
import database.repositories.{FileRepository, TaskRepository}
import slick.jdbc.MySQLProfile.api._
import database.utils.DatabaseUtils._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Object that contains the main method for the project.
  */
object TickTock {

  implicit val ec: ExecutionContext = this.ec

  val fileRepo = new FileRepository(DEFAULT_DB)
  val taskRepo = new TaskRepository(DEFAULT_DB)

  def retrieveDataFromDB(implicit ec: ExecutionContext): Future[Unit] = {
    println("retrieving data from DB")
    taskRepo.selectAllTasks.map { seq =>
      seq.foreach(t => fileRepo.selectNameFromFileId(t.fileId).map(name => scheduleTask(name, t.startDateAndTime)))
    }
  }

  def main(args: Array[String]): Unit = {
    fileRepo.createFilesTable
    taskRepo.createTasksTable

    fileRepo.insertInFilesTable(FileRow(0, "EmailSender", "EmailSender", FileService.getCurrentDateTimestamp))

    retrieveDataFromDB
  }

}