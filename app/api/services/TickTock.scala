package api.services

import api.dtos.TaskDTO
import api.services.TaskService._
import database.repositories.slick.{FileRepositoryImpl, TaskRepositoryImpl}
import slick.jdbc.MySQLProfile.api._
import api.utils.DateUtils._
import database.mappings.FileMappings.FileRow
import database.repositories.{FileRepository, TaskRepository}
import database.utils.DatabaseUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * Object that contains the main method for the project.
  */
object TickTock {

  val dbUtils = new DatabaseUtils
  val fileRepo = new FileRepositoryImpl(dbUtils.DEFAULT_DB)
  val taskRepo = new TaskRepositoryImpl(dbUtils.DEFAULT_DB)
  implicit val ec: ExecutionContext = this.ec

  def retrieveDataFromDB(implicit ec: ExecutionContext): Future[Unit] = {
    println("retrieving data from DB")
    taskRepo.selectAllTasks.map { seq =>
      seq.foreach(t => scheduleTask(t.fileName, t.startDateAndTime))
    }
  }

  def main(args: Array[String]): Unit = {
    taskRepo.insertInTasksTable(TaskDTO(stringToDateFormat("2018-12-18 11:30:00", "yyyy-MM-dd HH:mm:ss"), "hello"))
  }

}