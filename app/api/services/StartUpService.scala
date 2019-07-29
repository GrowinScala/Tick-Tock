package api.services

import javax.inject.{ Inject, Singleton }
import database.mappings.TaskMappings._
import slick.jdbc.MySQLProfile.api._
import database.repositories.file.FileRepository
import database.repositories.task.TaskRepository
import executionengine.ExecutionManager
import play._

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration.Duration

@Singleton
class StartUpService @Inject() (dtbase: Database, implicit val fileRepo: FileRepository, implicit val taskRepo: TaskRepository, implicit val executionManager: ExecutionManager) {

  implicit val ec: ExecutionContext = ExecutionContext.global

  taskRepo.selectAllTasks().map { tasks =>
    val taskService = new TaskService
    tasks.foreach(task => taskService.scheduleTask(task))
  }

}
