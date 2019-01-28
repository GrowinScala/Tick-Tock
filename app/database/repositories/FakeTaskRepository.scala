package database.repositories

import api.dtos.TaskDTO
import api.services.{PeriodType, SchedulingType}
import api.utils.DateUtils._
import javax.inject.Inject
import slick.dbio.DBIO

import scala.concurrent.Future

class FakeTaskRepository extends TaskRepository{

  def selectAllTasks: Future[Seq[TaskDTO]] = {
    Future.successful(Seq(
      TaskDTO("asd1", getCurrentDateTimestamp, "test1", SchedulingType.RunOnce),
      TaskDTO("asd2", getCurrentDateTimestamp, "test2", SchedulingType.Periodic, Some(PeriodType.Minutely), Some(2), Some(getCurrentDateTimestamp)),
      TaskDTO("asd3", getCurrentDateTimestamp, "test3", SchedulingType.Periodic, Some(PeriodType.Hourly), Some(1), None, Some(5), Some(5))
    ))
  }

  /**
    * Select a single task from the database by giving its id.
    *
    * @param id - the identifier of the task.
    * @return a TaskDTO of the selected task.
    */
  def selectTaskByTaskId(id: String): Future[TaskDTO] = {
    Future.successful(TaskDTO("asd1", getCurrentDateTimestamp, "test1", SchedulingType.RunOnce))
  }

  /**
    *
    * Select the fileId from a task by giving its taskId.
    *
    * @param id - the identifier of the task.
    * @return a String containing the fileId.
    */
  def selectFileIdByTaskId(id: String): Future[String] = {
    Future.successful("asd1")
  }

  /**
    * Select the totalOccurrences from a task on the database by giving its id.
    *
    * @param id - the identifier of the task.
    * @return an Int representing the totalOccurrences of the task.
    */
  def selectTotalOccurrencesByTaskId(id: String): Future[Option[Int]] = {
    Future.successful(Some(5))
  }

  /**
    * Select the currentOccurrences from a task on the database by giving its id
    *
    * @param id - the identifier of the task we want to select.
    * @return an Int representing the currentOccurrences of the task.
    */
  def selectCurrentOccurrencesByTaskId(id: String): Future[Option[Int]] = {
    Future.successful(Some(5))
  }

  /**
    *
    * Reduces the currentOccurrences from a task on the database by 1 by giving its id.
    *
    * @param id - the identifier of the task we want to select.
    */
  def decrementCurrentOccurrencesByTaskId(id: String): Future[Unit] = {
    Future.successful((): Unit)
  }

  /**
    * Deletes all tasks from the tasks table on the database.
    */
  def deleteAllTasks: Future[Int] = {
    Future.successful(3)
  }

  /**
    * Creates the tasks table on the database.
    */
  def createTasksTable: Future[Unit] = {
    Future.successful((): Unit)
  }

  /**
    * Drops the tasks table on the database.
    */
  def dropTasksTable: Future[Unit] = {
    Future.successful((): Unit)
  }

  /**
    * Inserts a task (row) on the tasks table on the database.
    *
    * @param task TaskDTO to be inserted.
    */
  def insertInTasksTable(task: TaskDTO): Future[Boolean] = {
    Future.successful(true)
  }
}
