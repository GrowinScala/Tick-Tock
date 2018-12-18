package api.services

import api.dtos.TaskDTO
import api.services.TaskService._
import database.repositories.slick.{FileRepositoryImpl, TaskRepositoryImpl}
import slick.jdbc.MySQLProfile.api._
import database.utils.DatabaseUtils._
import api.utils.DateUtils._
import database.mappings.FileMappings.FileRow
import database.repositories.{FileRepository, TaskRepository}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Object that contains the main method for the project.
  */
object TickTock {

  val fileRepo = new FileRepositoryImpl(DEFAULT_DB)
  val taskRepo = new TaskRepositoryImpl(DEFAULT_DB)
  implicit val ec: ExecutionContext = this.ec

  def retrieveDataFromDB(implicit ec: ExecutionContext): Future[Unit] = {
    println("retrieving data from DB")
    taskRepo.selectAllTasks.map { seq =>
      seq.foreach(t => fileRepo.selectNameFromFileId(t.fileId).map(name => scheduleTask(name, t.startDateAndTime)))
    }
  }

  def main(args: Array[String]): Unit = {
    taskRepo.insertInTasksTable(TaskDTO(stringToDateFormat("2018-12-18 11:30:00", "yyyy-MM-dd HH:mm:ss"), "hello"))
  }

}