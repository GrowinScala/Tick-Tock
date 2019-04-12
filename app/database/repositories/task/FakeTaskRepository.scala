package database.repositories.task

import api.dtos.TaskDTO
import api.services.{ PeriodType, SchedulingType }
import api.utils.DateUtils._

import scala.concurrent.Future

class FakeTaskRepository extends TaskRepository {

  def selectAllTasks: Future[Seq[TaskDTO]] = {
    Future.successful(Seq(
      TaskDTO("asd1", "test1", SchedulingType.RunOnce, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss"))),
      TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")), Some(PeriodType.Minutely), Some(2), Some(stringToDateFormat("01-01-2050 12:00:00", "dd-MM-yyyy HH:mm:ss"))),
      TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")), Some(PeriodType.Hourly), Some(1), None, Some(5), Some(5))))
  }

  /**
   * Select a single task from the database by giving its id.
   *
   * @param id - the identifier of the task.
   * @return a TaskDTO of the selected task.
   */
  def selectTask(id: String): Future[Option[TaskDTO]] = {
    Future.successful(Some(TaskDTO("asd1", "test1", SchedulingType.RunOnce, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")))))
  }

  /**
   *
   * Select the fileId from a task by giving its taskId.
   *
   * @param id - the identifier of the task.
   * @return a String containing the fileId.
   */
  def selectFileIdByTaskId(id: String): Future[Option[String]] = {
    Future.successful(Some("asd1"))
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
   * Given a an id deletes the corresponding task
   *
   * @param id - identifier of the task to be deleted
   */
  def deleteTaskById(id: String): Future[Int] = {
    Future.successful(1)
  }

  /**
   * Updates a single task given its identifier
   *
   * @param id   - identifier of the task to be updated
   * @param task - information to update the task with
   * @return an Int with information of the updated task
   */
  def updateTaskById(id: String, task: TaskDTO): Future[Boolean] = {
    Future.successful(true)
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
